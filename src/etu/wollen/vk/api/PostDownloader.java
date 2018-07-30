package etu.wollen.vk.api;

import etu.wollen.vk.database.DBConnector;
import etu.wollen.vk.models.Like;
import etu.wollen.vk.models.WallComment;
import etu.wollen.vk.models.WallPost;
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
	private final String accessToken;
	private final Map<Long, String> groupNames = new HashMap<>();

	public PostDownloader(List<String> groupShortNames, String accessToken){
		this.accessToken = accessToken;
		fillGroupNames(groupShortNames);
	}
	
	public Map<Long, String> getGroupNames(){
		return groupNames;
	}

	public void parseGroups(final Date dateRestriction) {
		class WallParser implements Callable<Void> {
			private long wall_id;
			private String wall_name;
			private int count;
			private ExecutorService executorService;

			private WallParser(long wall_id, String wall_name, int count, ExecutorService executorService) {
				this.wall_id = wall_id;
				this.wall_name = wall_name;
				this.count = count;
				this.executorService = executorService;
			}

			@Override
			public Void call() {
				System.out.println("Parsing vk.com/club" + wall_id + " (" + wall_name + ")    " + count + " of "
						+ groupNames.size());
				getPosts(wall_id * (-1), wall_name, dateRestriction, executorService);
				return null;
			}

		}

		ExecutorService primaryExecutor = null;
		ExecutorService secondaryExecutor = null;
        try {
			primaryExecutor = Executors.newFixedThreadPool(PRIMARY_THREADS);
			secondaryExecutor = Executors.newFixedThreadPool(SECONDARY_THREADS);
			List<Callable<Void>> workers = new ArrayList<>(groupNames.entrySet().size());
			int count = 1;
			for (Map.Entry<Long, String> group : groupNames.entrySet()) {
				Long wall_id = group.getKey();
				String wall_name = group.getValue();
				WallParser worker = new WallParser(wall_id, wall_name, count++, secondaryExecutor);
				workers.add(worker);
			}
			primaryExecutor.invokeAll(workers);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally{
			if(primaryExecutor != null) primaryExecutor.shutdown();
			if(secondaryExecutor != null) secondaryExecutor.shutdown();
		}
	}

	private void fillGroupNames(List<String> groupShortNames) {
		if (!groupShortNames.isEmpty()) {
			StringBuilder request = new StringBuilder("https://api.vk.com/method/groups.getById?group_ids=");
			for (String gr : groupShortNames) {
				request.append(gr).append(",");
			}
			request.append("&v=" + VERSION + "&access_token=").append(accessToken);
			try {
				String response = HttpClient.getInstance().sendGETtimeout(request.toString(), 11);

				JSONParser jp = new JSONParser();
				JSONObject jsonresponse = (JSONObject) jp.parse(response);
				JSONArray items = (JSONArray) jsonresponse.get("response");
				for (Object oitem : items) {
					JSONObject item = (JSONObject) oitem;
					long gid = (long) item.get("id");
					String name = (String) item.get("name");
					long is_closed = (long) item.get("is_closed");
					if(is_closed == 0){
						groupNames.put(gid, name);
					}
					else{
						// TODO check whether the user is a member of the private group
						String screen_name = (String) item.get("screen_name");
						System.out.println("Closed group: " + screen_name);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void getPosts(long wall_id, String wall_name, Date dateRestriction, ExecutorService secondaryExecutor) {
		final long count = 100;
		long offset = 0;

		ExecutorService databaseExecutor = null;
		Future<?> currentDatabaseTask = null;
		try {
			databaseExecutor = Executors.newSingleThreadExecutor();
			Set<Long> allWallSet = DBConnector.getPostsFromWallIdSet(wall_id);
			boolean allRead = false;
			while (!allRead) {
				final List<WallPost> postsToCommit = new ArrayList<>();
				final List<WallComment> commentsToCommit = new ArrayList<>();
				final List<Like> likesToCommit = new ArrayList<>();
				final AtomicReference<Exception> nestedThreadException = new AtomicReference<>(null);

				/// TODO check timeouts
				String request = "https://api.vk.com/method/wall.get?owner_id=" + wall_id + "&offset=" + offset
						+ "&count=" + count + "&v=" + VERSION + "&access_token=" + accessToken;
				offset += count;

				String response = HttpClient.getInstance().sendGETtimeout(request, 11);

				JSONParser jp = new JSONParser();
				JSONObject jsonresponse = (JSONObject) jp.parse(response);
				JSONObject resp = (JSONObject) jsonresponse.get("response");
				if(resp == null){
				    JSONObject error = (JSONObject) jsonresponse.get("error");
				    throw new Exception(error.toJSONString());
                }
                Object posts_count_object = resp.get("count");
				long posts_count = posts_count_object == null ? 0 : (long) posts_count_object;
				JSONArray items = (JSONArray) resp.get("items");
				if (items.size() == 0) {
					allRead = true;
				}

				for (Object oitem : items) {
					JSONObject item = (JSONObject) oitem;

					long date = (long) item.get("date");
					long post_id = (long) item.get("id");
					Object signer_id_object = item.get("signer_id");
					long signer_id = signer_id_object == null ? 0 : (long) signer_id_object;
					long owner_id = (long) item.get("owner_id");
					long from_id = (long) item.get("from_id");
					if (owner_id != from_id) {
						signer_id = from_id;
					}
					String text = (String) item.get("text");
					WallPost wp = new WallPost(post_id, wall_id, signer_id, new Date(date * 1000), text);

					// if wall post exists
					if (allWallSet.contains(post_id)) {
						if (!allRead && allWallSet.size() >= posts_count) {
							allRead = true;
							System.out.println("All posts in " + wall_id + " ("+wall_name+") have been parsed");
						}
					} else {
						// stash in memory
						allWallSet.add(wp.getPostId());
						postsToCommit.add(wp);
					}

					// if date restriction reached
					if (!allRead && wp.getDate().getTime() < dateRestriction.getTime()) {
						allRead = true;
						System.out.println("Date restriction in " + wall_id);
					}
				}

				if (postsToCommit.size() > 0) {
					// downloads comments and likes in multiple threads
					List<Callable<Void>> workers = new ArrayList<>(2*postsToCommit.size());
					for (final WallPost wp : postsToCommit) {
						Callable<Void> likesWorker = () -> {
						    try {
                                List<Like> lkl = getLikes(wp.getGroupId(), wp.getPostId(), wp.getDate());
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
								List<WallComment> wcl = getComments(wp.getGroupId(), wp.getPostId());
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
						DBConnector.insertPostsWithComments(postsToCommit, commentsToCommit, likesToCommit);
						return null;
					});
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("ERROR: Wall " + wall_id + " ("+wall_name+") has not been parsed");
		}
		finally{
			if(databaseExecutor != null) databaseExecutor.shutdown();
		}
	}

	private List<WallComment> getComments(long wall_id, long post_id) throws Exception{
		List<WallComment> comments = new ArrayList<>();
		final long count = 100;
		long offset = 0;
		long commentsRead = 0;
		String request = "";
		String response = "";

		try {
			boolean allRead = false;
			final int attempts = 5;

			while (!allRead) {
				for (int i = 0; i < attempts; ++i) {
					try {
                        /// TODO check timeouts
						request = "https://api.vk.com/method/wall.getComments?owner_id=" + wall_id + "&post_id="
								+ post_id + "&offset=" + offset + "&count=" + count + "&v=" + VERSION + "&access_token=" + accessToken;

						response = HttpClient.getInstance().sendGETtimeout(request, 11);

						JSONParser jp = new JSONParser();
						JSONObject jsonresponse = (JSONObject) jp.parse(response);
						JSONObject resp = (JSONObject) jsonresponse.get("response");
                        if(resp == null){
                            JSONObject error = (JSONObject) jsonresponse.get("error");
                            throw new Exception(error.toJSONString());
                        }
						Object comments_count_object = resp.get("count");
						long comments_count = comments_count_object == null ? 0 : (long) comments_count_object;
						JSONArray items = (JSONArray) resp.get("items");
						for (Object oitem : items) {
							JSONObject item = (JSONObject) oitem;
							long date = (long) item.get("date");
							long comment_id = (long) item.get("id");
							long from_id = (long) item.get("from_id");
							String text = (String) item.get("text");
							Object reply_object = item.get("reply_to_user");
							long reply = reply_object == null ? 0 : (long) reply_object;
							WallComment wc = new WallComment(comment_id, from_id, new Date(date * 1000), text, wall_id,
									post_id, reply);
							comments.add(wc);
							++commentsRead;
						}
						if (items.size() == 0 || commentsRead >= comments_count) {
							allRead = true;
						}
                        offset += count;
						break; // exit cycle
					} catch (NullPointerException e) {
						if (i < attempts - 1) {
							System.out.println("Can't get comments from http://vk.com/wall" + wall_id + "_" + post_id
									+ "... " + (attempts - i - 1) + " more attempts...");
						} else {
							throw e;
						}
					}
				}
			}

		} catch (Exception e) {
			System.out.println(request + " => " + response);
			throw e;
		}
		return comments;
	}

	private List<Like> getLikes(long wall_id, long post_id, Date date) throws Exception{
		List<Like> likes = new ArrayList<>();
		final long count = 100;
		long offset = 0;
		long likesRead = 0;
		String request = "";
		String response = "";

		try {
			boolean allRead = false;
			final int attempts = 5;

			while (!allRead) {
				for (int i = 0; i < attempts; ++i) {
					try {
                        /// TODO check timeouts
						request = "https://api.vk.com/method/likes.getList?type=post&owner_id=" + wall_id + "&item_id="
								+ post_id + "&offset=" + offset + "&count=" + count + "&v=" + VERSION + "&access_token=" + accessToken;
						response = HttpClient.getInstance().sendGETtimeout(request, 11);

						JSONParser jp = new JSONParser();
						JSONObject jsonresponse = (JSONObject) jp.parse(response);
						JSONObject resp = (JSONObject) jsonresponse.get("response");
                        if(resp == null){
                            JSONObject error = (JSONObject) jsonresponse.get("error");
                            throw new Exception(error.toJSONString());
                        }
						Object likes_count_object = resp.get("count");
						long likes_count = likes_count_object == null ? 0 : (long) likes_count_object;
						JSONArray items = (JSONArray) resp.get("items");
						for (Object oitem : items) {
							long user = (long) oitem;
							Like like = new Like(user, "post", wall_id, post_id, date);
							likes.add(like);
							++likesRead;
						}
						if (items.size() == 0 || likesRead >= likes_count) {
							allRead = true;
						}
						offset += count;
						break; // exit cycle
					} catch (NullPointerException e) {
						if (i < attempts - 1) {
							System.out.println("Can't get likes from http://vk.com/wall" + wall_id + "_" + post_id
									+ "... " + (attempts - i - 1) + " more attempts...");
						} else {
							throw e;
						}
					}
				}
			}

		} catch (Exception e) {
			System.out.println(request + " => " + response);
			throw e;
		}
		return likes;
	}
}
