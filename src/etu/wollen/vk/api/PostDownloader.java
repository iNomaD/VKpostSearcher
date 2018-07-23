package etu.wollen.vk.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import etu.wollen.vk.database.DBConnector;
import etu.wollen.vk.models.Like;
import etu.wollen.vk.models.WallComment;
import etu.wollen.vk.models.WallPost;
import etu.wollen.vk.transport.HttpClient;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import static etu.wollen.vk.conf.Config.commentThreads;
import static etu.wollen.vk.conf.Config.groupThreads;
import static etu.wollen.vk.conf.Config.version;

public class PostDownloader {
	private String access_token;
	private Map<Long, String> groupNames = new HashMap<>();
	
	public PostDownloader(String access_token){
		this.access_token = access_token;
	}

	public void fillGroupNames(List<String> groupShortNames) {
		if (!groupShortNames.isEmpty()) {
			StringBuilder request = new StringBuilder("https://api.vk.com/method/groups.getById?group_ids=");
			for (String gr : groupShortNames) {
				request.append(gr).append(",");
			}
			request.append("&v=" + version + "&access_token=").append(access_token);
			try {
				String response = HttpClient.sendGETtimeout(request.toString(), 11);

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
	
	public Map<Long, String> getGroupNames(){
		return groupNames;
	}

	public void parseGroups(final Date dateRestr) {
		class WallParser implements Runnable {
			private long wall_id;
			private String wall_name;
			private int count;

			private WallParser(long wall_id, String wall_name, int count) {
				this.wall_id = wall_id;
				this.wall_name = wall_name;
				this.count = count;
			}

			@Override
			public void run() {
				System.out.println("Parsing vk.com/club" + wall_id + " (" + wall_name + ")    " + count + " of "
						+ groupNames.size());
				getPosts(wall_id * (-1), wall_name, dateRestr, access_token);
				System.gc();
			}

		}
		ExecutorService executor = Executors.newFixedThreadPool(groupThreads);

		int count = 1;
		for (Map.Entry<Long, String> group : groupNames.entrySet()) {
			Long wall_id = group.getKey();
			String wall_name = group.getValue();
			WallParser worker = new WallParser(wall_id, wall_name, count);
			++count;
			executor.execute(worker);
		}

		executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}

	private static void getPosts(long wall_id, String wall_name, Date dateRestr, final String access_token) {
		final long count = 100;
		long offset = 0;

		try {
			Set<Long> allWallSet = DBConnector.getPostsFromWallIdSet(wall_id);

			boolean allRead = false;
			while (!allRead) {
				final List<WallPost> postsToCommit = new ArrayList<>();
				final List<WallComment> commentsToCommit = new ArrayList<>();
				final List<Like> likesToCommit = new ArrayList<>();
				AtomicReference<Exception> nestedThreadException = new AtomicReference<>(null);

				/// TODO check timeouts
				String request = "https://api.vk.com/method/wall.get?owner_id=" + wall_id + "&offset=" + offset
						+ "&count=" + count + "&v=" + version + "&access_token=" + access_token;
				offset += count;

				String response = HttpClient.sendGETtimeout(request, 11);

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
					if (!allRead && wp.getDate().getTime() < dateRestr.getTime()) {
						allRead = true;
						System.out.println("Date restriction in " + wall_id);
					}
				}

				if (postsToCommit.size() > 0) {
					// downloads comments and likes in multithreads
					ExecutorService executor = Executors.newFixedThreadPool(commentThreads);

					for (final WallPost wp : postsToCommit) {
						Runnable worker = () -> {
						    try {
                                List<WallComment> wcl = getComments(wp.getGroupId(), wp.getPostId(), access_token);
                                List<Like> lkl = getLikes(wp.getGroupId(), wp.getPostId(), wp.getDate(), access_token);
                                synchronized (commentsToCommit) {
                                    commentsToCommit.addAll(wcl);
                                    likesToCommit.addAll(lkl);
                                }
                            }
                            catch (Exception e){
						        nestedThreadException.set(e);
                            }
                        };
						executor.execute(worker);
					}

					executor.shutdown();
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    if(nestedThreadException.get() != null) throw nestedThreadException.get();

					// write to db
					DBConnector.insertPostsWithComments(postsToCommit, commentsToCommit, likesToCommit);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("ERROR: Wall " + wall_id + " ("+wall_name+") has not been parsed");
		}
	}

	private static List<WallComment> getComments(long wall_id, long post_id, String access_token) throws Exception{
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
								+ post_id + "&offset=" + offset + "&count=" + count + "&v=" + version + "&access_token=" + access_token;

						response = HttpClient.sendGETtimeout(request, 11);

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

	private static List<Like> getLikes(long wall_id, long post_id, Date date, String access_token) throws Exception{
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
								+ post_id + "&offset=" + offset + "&count=" + count + "&v=" + version + "&access_token=" + access_token;
						response = HttpClient.sendGETtimeout(request, 11);

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
