package etu.wollen.vk.api;

import etu.wollen.vk.database.DatabaseUtils;
import etu.wollen.vk.exceptions.ApiError;
import etu.wollen.vk.model.conf.Board;
import etu.wollen.vk.model.database.BoardComment;
import etu.wollen.vk.model.database.WallPost;
import etu.wollen.vk.model.database.WallPostComment;
import etu.wollen.vk.model.database.WallPostLike;
import etu.wollen.vk.transport.HttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static etu.wollen.vk.conf.Config.*;

public class PostDownloader {
	private static final int GET_POSTS_COUNT = 100;
	private static final int GET_BOARD_COMMENTS_COUNT = 100;
	private static final int GET_POST_COMMENTS_COUNT = 100;
	private static final int GET_POST_LIKES_COUNT = 1000;

	private static final int GET_POST_COMMENTS_MAX_ATTEMPTS = 5;
	private static final int GET_POST_LIKES_MAX_ATTEMPTS = 5;

	private static final String REQUEST_GROUPS_GET_BY_ID = VK_API_URL + "groups.getById?fields=is_member&group_ids=%s&access_token=%s&v=" + VERSION;
	private static final String REQUEST_BOARD_GET_TOPICS = VK_API_URL + "board.getTopics?group_id=%s&topic_ids=%s&access_token=%s&v=" + VERSION;
	private static final String REQUEST_WALL_GET = VK_API_URL + "wall.get?owner_id=%s&offset=%s&count=" + GET_POSTS_COUNT + "&access_token=%s&v=" + VERSION;
	private static final String REQUEST_BOARD_GET_COMMENTS = VK_API_URL + "board.getComments?&group_id=%s&topic_id=%s&offset=%s&count=" + GET_BOARD_COMMENTS_COUNT + "&access_token=%s&v=" + VERSION;
	private static final String REQUEST_WALL_GET_COMMENTS = VK_API_URL + "wall.getComments?owner_id=%s&post_id=%s&offset=%s&count=" + GET_POST_COMMENTS_COUNT + "&access_token=%s&v=" + VERSION;
	private static final String REQUEST_LIKES_GET_LIST = VK_API_URL + "likes.getList?type=post&owner_id=%s&item_id=%s&offset=%s&count=" + GET_POST_LIKES_COUNT + "&access_token=%s&v=" + VERSION;

	private final String accessToken;
	private final Map<Long, String> groupNames = new HashMap<>();
	private final Map<Board, String> topicTitles = new HashMap<>();
	private final boolean privateGroupsEnabled;

	public PostDownloader(List<String> groupShortNames, List<Board> boards, String accessToken, boolean privateGroupsEnabled) {
		this.accessToken = accessToken;
		this.privateGroupsEnabled = privateGroupsEnabled;
		fillGroupNames(groupShortNames);
		fillTopicTitles(boards);
	}
	
	public Map<Long, String> getGroupNames(){
		return groupNames;
	}

	public Map<Board, String> getTopicTitles(){
		return topicTitles;
	}

	public void parseGroups(final Date dateRestriction) {
		class WallParser implements Callable<Void> {
			private final long wallId;
			private final String wallName;
			private final int count;
			private final ExecutorService databaseExecutor;
			private final ExecutorService secondaryExecutor;

			private WallParser(long wallId, String wallName, int count, ExecutorService databaseExecutor, ExecutorService secondaryExecutor) {
				this.wallId = wallId;
				this.wallName = wallName;
				this.count = count;
				this.databaseExecutor = databaseExecutor;
				this.secondaryExecutor = secondaryExecutor;
			}

			@Override
			public Void call() {
				String title = "vk.com/club" + wallId + " (" + wallName + ")";
				System.out.println("Parsing " + title + " " + count + " of " + groupNames.size());
				getPosts(wallId * (-1), title, dateRestriction, databaseExecutor, secondaryExecutor);
				return null;
			}

		}

		class BoardParser implements Callable<Void> {
			private final long groupId;
			private final long topicId;
			private final String topicTitle;
			private final int count;
			private final ExecutorService databaseExecutor;

			private BoardParser(long groupId, long topicId, String topicTitle, int count, ExecutorService databaseExecutor) {
				this.groupId = groupId;
				this.topicId = topicId;
				this.topicTitle = topicTitle;
				this.count = count;
				this.databaseExecutor = databaseExecutor;
			}

			@Override
			public Void call() {
				String title = "vk.com/topic-" + groupId + "_" + topicId + " (" + topicTitle + ")";
				System.out.println("Parsing " + title + " " + count + " of " + topicTitles.size());
				getBoardComments(groupId, topicId, title, dateRestriction, databaseExecutor);
				return null;
			}

		}

		ExecutorService primaryExecutor = null;
		ExecutorService secondaryExecutor = null;
		ExecutorService databaseExecutor = null;
        try {
			primaryExecutor = Executors.newFixedThreadPool(PRIMARY_THREADS);
			secondaryExecutor = Executors.newFixedThreadPool(SECONDARY_THREADS);
			databaseExecutor = Executors.newSingleThreadExecutor(); // TODO migrate to database with multiple connections
			List<Callable<Void>> workers = new ArrayList<>(groupNames.entrySet().size());
			int groupCount = 1;
			for (Map.Entry<Long, String> group : groupNames.entrySet()) {
				Long wallId = group.getKey();
				String wallName = group.getValue();
				workers.add(new WallParser(wallId, wallName, groupCount++, databaseExecutor, secondaryExecutor));
			}
			int topicCount = 1;
			for (Map.Entry<Board, String> topic : topicTitles.entrySet()) {
				long wallId = topic.getKey().getGroupId();
				long topicId = topic.getKey().getTopicId();
				String topicTitle = topic.getValue();
				workers.add(new BoardParser(wallId, topicId, topicTitle, topicCount++, databaseExecutor));
			}
			primaryExecutor.invokeAll(workers);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
			if (primaryExecutor != null) primaryExecutor.shutdown();
			if (secondaryExecutor != null) secondaryExecutor.shutdown();
			if (databaseExecutor != null) databaseExecutor.shutdown();
		}
	}

	private void fillGroupNames(List<String> groupShortNames) {
		if (!groupShortNames.isEmpty()) {
			String groups = String.join(",", groupShortNames);
			String request = String.format(REQUEST_GROUPS_GET_BY_ID, groups, accessToken);
			try {
				String response = HttpClient.getInstance().httpGet(request);
				JSONParser jp = new JSONParser();
				JSONObject jsonResponse = (JSONObject) jp.parse(response);
				JSONArray items = (JSONArray) jsonResponse.get("response");
				for (Object oitem : items) {
					JSONObject item = (JSONObject) oitem;
					long gid = (long) item.get("id");
					String name = (String) item.get("name");
					long isClosed = (long) item.get("is_closed");
                    String screen_name = (String) item.get("screen_name");
					String groupDescription = screen_name + "[" + gid + "]" + " (" + name + ")";

                    Object isDeactivated = item.get("deactivated");
					Object isMember = item.get("is_member");
					if (isDeactivated != null) {
						System.out.println("Deactivated (" + isDeactivated + ") group: " + groupDescription);
					} else if (isClosed != 0 && !privateGroupsEnabled) {
						System.out.println("Closed group: " + groupDescription);
					} else if (isClosed != 0 && isMember != null && (long) isMember == 0) {
						System.out.println("Not member of private group: " + groupDescription);
					} else {
						groupNames.put(gid, name);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void fillTopicTitles(List<Board> boards) {
		if (!boards.isEmpty()) {
			for (Board board : boards) {
				String request = String.format(REQUEST_BOARD_GET_TOPICS, board.getGroupId(), board.getTopicId(), accessToken);
				try {
					String response = HttpClient.getInstance().httpGet(request);
					JSONParser jp = new JSONParser();
					JSONObject jsonResponse = (JSONObject) jp.parse(response);
					JSONObject resp = (JSONObject) jsonResponse.get("response");
					if (resp == null) {
						// TODO manage topics in closed groups
						System.out.println("Closed topic: (" + board.toString() + ")");
					}
					else {
						JSONArray items = (JSONArray) resp.get("items");
						for (Object oitem : items) {
							JSONObject item = (JSONObject) oitem;
							long id = (long) item.get("id");
							String title = (String) item.get("title");
							long isClosed = (long) item.get("is_closed");
							if (id == board.getTopicId()) {
								if (isClosed == 0 || privateGroupsEnabled) {
									topicTitles.put(board, title);
								} else {
									System.out.println("Closed topic: " + title + "[" + board.getTopicId() + "]" + " (" + board.toString() + ")");
								}
							}
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void getPosts(final long wallId, final String title, final Date dateRestriction,
						  final ExecutorService databaseExecutor, final ExecutorService secondaryExecutor) {

		long offset = 0;
		Future<?> currentDatabaseTask = null;
		try {
			Set<Long> allWallSet = DatabaseUtils.getPostsIdSetFromWall(wallId);
			boolean readFinished = false;
			while (!readFinished) {
				final List<WallPost> postsToCommit = new ArrayList<>();
				final List<WallPostComment> commentsToCommit = new ArrayList<>();
				final List<WallPostLike> likesToCommit = new ArrayList<>();
				final AtomicReference<Exception> nestedThreadException = new AtomicReference<>(null);

				String request = String.format(REQUEST_WALL_GET, wallId, offset, accessToken);
				String response = HttpClient.getInstance().httpGet(request);
				offset += GET_POSTS_COUNT;

				JSONParser jp = new JSONParser();
				JSONObject jsonResponse = (JSONObject) jp.parse(response);
				JSONObject resp = (JSONObject) jsonResponse.get("response");
                JSONObject error = (JSONObject) jsonResponse.get("error");
                if (error != null) {
                    Object errorCode = error.get("error_code");
                    Object errorMsg = error.get("error_msg");
					throw new ApiError(errorMsg + " (" + errorCode + ")");
                }
                Object postsCountObject = resp.get("count");
				long postsCount = postsCountObject == null ? 0 : (long) postsCountObject;
				JSONArray items = (JSONArray) resp.get("items");
				if (items.size() == 0) {
					readFinished = true;
				}

				for (Object oitem : items) {
					JSONObject item = (JSONObject) oitem;

					long date = (long) item.get("date");
					long postId = (long) item.get("id");
					Object singerIdObject = item.get("signer_id");
					long signerId = singerIdObject == null ? 0 : (long) singerIdObject;
					long ownerId = (long) item.get("owner_id");
					long fromId = (long) item.get("from_id");
					if (ownerId != fromId) {
						signerId = fromId;
					}
					String text = (String) item.get("text");
					WallPost wp = new WallPost(postId, wallId, signerId, new Date(date * 1000), text);

					if (wp.getDate().getTime() < dateRestriction.getTime()) {
					    if (!readFinished) {
                            System.out.println("Date restriction in " + title);
                            readFinished = true;
                        }
                    }
					else if (!allWallSet.contains(postId)) {
						allWallSet.add(wp.getPostId());
						postsToCommit.add(wp);
					}
				}

				if (postsToCommit.isEmpty() && allWallSet.size() >= postsCount) {
				    /* TODO bug: some new posts can be lost
				        in case of some old posts were stored in database
				        but deleted from the wall in vk
				     */
					readFinished = true;
				}
				else if (postsToCommit.size() > 0) {
					// downloads comments and likes in multiple threads
					List<Callable<Void>> workers = new ArrayList<>(2 * postsToCommit.size());
					for (final WallPost wp : postsToCommit) {
						Callable<Void> likesWorker = () -> {
						    try {
                                List<WallPostLike> lkl = getPostLikes(wp.getGroupId(), wp.getPostId(), wp.getDate());
                                synchronized (likesToCommit) {
                                    likesToCommit.addAll(lkl);
                                }
                            }
                            catch (Exception e){
						        nestedThreadException.set(e);
                            }
                            return null;
                        };
						Callable<Void> commentsWorker = () -> {
							try {
								List<WallPostComment> wcl = getPostComments(wp.getGroupId(), wp.getPostId());
								synchronized (commentsToCommit) {
									commentsToCommit.addAll(wcl);
								}
							}
							catch (Exception e){
								nestedThreadException.set(e);
							}
							return null;
						};
						workers.add(likesWorker);
						workers.add(commentsWorker);
					}
					secondaryExecutor.invokeAll(workers);
                    if(nestedThreadException.get() != null) throw nestedThreadException.get();

					// wait until the previous database task is finished
					if(currentDatabaseTask != null){
						currentDatabaseTask.get();
					}
					// write to database
					currentDatabaseTask = databaseExecutor.submit((Callable<Void>) () -> {
						DatabaseUtils.insertPostsWithData(postsToCommit, commentsToCommit, likesToCommit);
						return null;
					});
				}
			}

			if (currentDatabaseTask != null) {
				currentDatabaseTask.get();
				System.out.println("All posts in " + title + " have been parsed");
			}
			else {
				System.out.println("No new posts in " + title);
			}
		}
		catch (ApiError e) {
            System.err.println("ERROR. Wall " + title +" has not been parsed: " + e.getMessage());
        }
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("ERROR. Wall " + title +" has not been parsed");
		}
	}

	private void getBoardComments(final long groupId, final long topicId, final String title, final Date dateRestriction,
								  final ExecutorService databaseExecutor) {
		long offset = 0;
		Future<?> currentDatabaseTask = null;
		try {
			Set<Long> allBoardSet = DatabaseUtils.getBoardCommentsIdSetFromWall(groupId, topicId);
			boolean readFinished = false;
			while (!readFinished) {
				final List<BoardComment> commentsToCommit = new ArrayList<>();
				String request = String.format(REQUEST_BOARD_GET_COMMENTS, groupId, topicId, offset, accessToken);
				String response = HttpClient.getInstance().httpGet(request);
				offset += GET_BOARD_COMMENTS_COUNT;

				JSONParser jp = new JSONParser();
				JSONObject jsonResponse = (JSONObject) jp.parse(response);
				JSONObject resp = (JSONObject) jsonResponse.get("response");
                JSONObject error = (JSONObject) jsonResponse.get("error");
                if (error != null) {
                    Object errorCode = error.get("error_code");
                    Object errorMsg = error.get("error_msg");
                    throw new ApiError(errorMsg + " (" + errorCode + ")");
                }
				Object commentsCountObject = resp.get("count");
				long commentsCount = commentsCountObject == null ? 0 : (long) commentsCountObject;
				JSONArray items = (JSONArray) resp.get("items");
				if (items.size() == 0) {
					readFinished = true;
				}

				for (Object oitem : items) {
					JSONObject item = (JSONObject) oitem;

					long date = (long) item.get("date");
					long commentId = (long) item.get("id");
					long fromId = (long) item.get("from_id");
					String text = (String) item.get("text");
					BoardComment bc = new BoardComment(new Date(date * 1000), commentId, groupId, topicId, fromId, text);

					if (bc.getDate().getTime() < dateRestriction.getTime()) {
						if (!readFinished) {
							System.out.println("Date restriction in " + title);
							readFinished = true;
						}
					}
					else if (!allBoardSet.contains(commentId)) {
						allBoardSet.add(bc.getCommentId());
						commentsToCommit.add(bc);
					}
				}

				if (commentsToCommit.isEmpty() && allBoardSet.size() >= commentsCount) {
				    /* TODO bug: some new comments can be lost
				        in case of some old comments were stored in database
				        but deleted from the board in vk
				     */
					readFinished = true;
				}
				else if (commentsToCommit.size() > 0) {
					// wait until the previous database task is finished
					if(currentDatabaseTask != null){
						currentDatabaseTask.get();
					}
					// write to database
					currentDatabaseTask = databaseExecutor.submit((Callable<Void>) () -> {
						DatabaseUtils.insertBoardCommentsWithData(commentsToCommit);
						return null;
					});
				}
			}

			if (currentDatabaseTask != null) {
				currentDatabaseTask.get();
				System.out.println("All comments in " + title +" have been parsed");
			}
			else {
				System.out.println("No new comments in " + title);
			}
		}
        catch (ApiError e) {
            System.err.println("ERROR. Board " + title +" has not been parsed: " + e.getMessage());
        }
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("ERROR: Board " + title + " has not been parsed");
		}
	}

	private List<WallPostComment> getPostComments(final long wallId, final long postId) throws Exception {
		final List<WallPostComment> comments = new ArrayList<>();
		long offset = 0;
		long commentsRead = 0;

		boolean readFinished = false;
		while (!readFinished) {
			for (int i = 0; i < GET_POST_COMMENTS_MAX_ATTEMPTS; ++i) {
				String request = String.format(REQUEST_WALL_GET_COMMENTS, wallId, postId, offset, accessToken);
				String response = null;
				try {
					response = HttpClient.getInstance().httpGet(request);

					JSONParser jp = new JSONParser();
					JSONObject jsonResponse = (JSONObject) jp.parse(response);
					JSONObject resp = (JSONObject) jsonResponse.get("response");
					JSONObject error = (JSONObject) jsonResponse.get("error");
					if (error != null) {
						Object errorCode = error.get("error_code");
						Object errorMsg = error.get("error_msg");
						if (errorCode != null && errorMsg != null && (long)errorCode == 15) {
							System.err.println("Failed to download comments from wallId=" + wallId + ", postId=" + postId + ", error=" + errorMsg);
							break;
						}
						throw new ApiError(errorMsg + " (" + errorCode + ")");
					}
					Object commentsCountObject = resp.get("count");
					long commentsCount = commentsCountObject == null ? 0 : (long) commentsCountObject;
					JSONArray items = (JSONArray) resp.get("items");
					for (Object oitem : items) {
						JSONObject item = (JSONObject) oitem;
						long date = (long) item.get("date");
						long commentId = (long) item.get("id");
						long fromId = (long) item.get("from_id");
						String text = (String) item.get("text");
						Object replyObject = item.get("reply_to_user");
						long reply = replyObject == null ? 0 : (long) replyObject;
						WallPostComment wc = new WallPostComment(commentId, fromId, new Date(date * 1000), text, wallId,
								postId, reply);
						comments.add(wc);
						++commentsRead;
					}
					if (items.size() == 0 || commentsRead >= commentsCount) {
						readFinished = true;
					}
					offset += GET_POST_COMMENTS_COUNT;
					break;
				} catch (ApiError e) {
					if (i < GET_POST_COMMENTS_MAX_ATTEMPTS - 1) {
						System.out.println("Can't get comments from http://vk.com/wall" + wallId + "_" + postId
								+ "... " + (GET_POST_COMMENTS_MAX_ATTEMPTS - i - 1) + " more attempts...");
					} else {
						System.err.println(request + " => " + response);
						throw e;
					}
				} catch (Exception e) {
					System.err.println(request + " => " + response);
					throw e;
				}
			}
		}
		return comments;
	}

	private List<WallPostLike> getPostLikes(final long wallId, final long postId, final Date date) throws Exception {
		final List<WallPostLike> likes = new ArrayList<>();
		long offset = 0;
		long likesRead = 0;

		boolean readFinished = false;
		while (!readFinished) {
			for (int i = 0; i < GET_POST_LIKES_MAX_ATTEMPTS; ++i) {
				String request = String.format(REQUEST_LIKES_GET_LIST, wallId, postId, offset, accessToken);
				String response = null;
				try {
					response = HttpClient.getInstance().httpGet(request);

					JSONParser jp = new JSONParser();
					JSONObject jsonResponse = (JSONObject) jp.parse(response);
					JSONObject resp = (JSONObject) jsonResponse.get("response");
					JSONObject error = (JSONObject) jsonResponse.get("error");
					if (error != null) {
						Object errorCode = error.get("error_code");
						Object errorMsg = error.get("error_msg");
						if (errorCode != null && errorMsg != null && (long)errorCode == 15) {
							System.err.println("Failed to download likes from wallId=" + wallId + ", postId=" + postId + ", error=" + errorMsg);
							break;
						}
						throw new ApiError(errorMsg + " (" + errorCode + ")");
					}
					Object likesCountObject = resp.get("count");
					long likesCount = likesCountObject == null ? 0 : (long) likesCountObject;
					JSONArray items = (JSONArray) resp.get("items");
					for (Object oitem : items) {
						long user = (long) oitem;
						WallPostLike like = new WallPostLike(user, "post", wallId, postId, date);
						likes.add(like);
						++likesRead;
					}
					if (items.size() == 0 || likesRead >= likesCount) {
						readFinished = true;
					}
					offset += GET_POST_LIKES_COUNT;
					break;
				} catch (ApiError e) {
					if (i < GET_POST_LIKES_MAX_ATTEMPTS - 1) {
						System.out.println("Can't get likes from http://vk.com/wall" + wallId + "_" + postId
								+ "... " + (GET_POST_LIKES_MAX_ATTEMPTS - i - 1) + " more attempts...");
					} else {
						System.err.println(request + " => " + response);
						throw e;
					}
				} catch (Exception e) {
					System.err.println(request + " => " + response);
					throw e;
				}
			}
		}
		return likes;
	}
}
