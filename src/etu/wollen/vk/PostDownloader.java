package etu.wollen.vk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PostDownloader {
	private Map<Long, String> groupNames = new HashMap<Long, String>();
	private static final int groupThreads = 4;
	private static final int commentThreads = 8;

	public void fillGroupNames(List<String> groupShortNames) {
		ArrayList<Long> groupIds = new ArrayList<Long>();
		if (!groupShortNames.isEmpty()) {
			String request = "http://api.vk.com/method/groups.getById?group_ids=";
			for (String gr : groupShortNames) {
				request += gr + ",";
			}
			try {
				String response = sendGETtimeout(request, 11);

				JSONParser jp = new JSONParser();
				JSONObject jsonresponse = (JSONObject) jp.parse(response);
				JSONArray items = (JSONArray) jsonresponse.get("response");
				for (Object oitem : items) {
					JSONObject item = (JSONObject) oitem;
					long gid = (long) item.get("gid");
					String name = (String) item.get("name");
					groupIds.add(gid);
					groupNames.put(gid, name);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Set<Long> getGroupSet() {
		return groupNames.keySet();
	}

	public void parseGroups(final Date dateRestr) {
		class WallParser implements Runnable {
			long wall_id;
			String wall_name;
			int count;

			WallParser(long wall_id, String wall_name, int count) {
				this.wall_id = wall_id;
				this.wall_name = wall_name;
				this.count = count;
			}

			@Override
			public void run() {
				System.out.println("Parsing vk.com/club" + wall_id + " (" + wall_name + ")    " + count + " of "
						+ groupNames.size());
				getPosts(wall_id * (-1), dateRestr);
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
		while (!executor.isTerminated()) {
		}
	}

	// send GET and return response in UTF-8
	private static String sendGET(String urlToRead) throws Exception {
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuilder result = new StringBuilder(conn.getContentLength());
		char[] cbuf = new char[1024];
		int len;
		while ((len = rd.read(cbuf)) != -1) {
			result.append(cbuf, 0, len);
		}
		rd.close();
		return result.toString();
	}

	public static String sendGETtimeout(String request, int attempts) throws Exception {
		String response = "";
		for (int i = 0; i < attempts; ++i) {
			try {
				response = sendGET(request);
				break; // exit cycle
			} catch (Exception e) {
				if (i < attempts - 1) {
					System.out.println("Connection timed out... " + (attempts - i - 1) + " more attempts...");
				} else {
					throw e;
				}
			}
		}
		return response;
	}

	public static void getPosts(long wall_id, Date dateRestr) {
		final long count = 100;
		long offset = 0;

		try {
			List<WallPost> wallPosts = DBConnector.getPostsFromWall(wall_id);
			Set<Long> allWallSet = new HashSet<Long>();
			for (WallPost wp : wallPosts) {
				allWallSet.add(wp.getPost_id());
			}

			boolean allRead = false;
			while (!allRead) {
				final List<WallPost> postsToCommit = new ArrayList<>();
				final List<WallComment> comsToCommit = new ArrayList<>();
				final List<Like> likesToCommit = new ArrayList<>();

				String request = "http://api.vk.com/method/wall.get?owner_id=" + wall_id + "&offset=" + offset
						+ "&count=" + count + "&v=5.45";
				offset += count;

				String response = sendGETtimeout(request, 11);

				JSONParser jp = new JSONParser();
				JSONObject jsonresponse = (JSONObject) jp.parse(response);
				JSONObject resp = (JSONObject) jsonresponse.get("response");
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
							System.out.println("All posts in " + wall_id + " were parsed");
						}
					} else {
						// stash in memory
						allWallSet.add(wp.getPost_id());
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
						Runnable worker = new Runnable() {
							@Override
							public void run() {
								List<WallComment> wcl = getComments(wp.getGroup_id(), wp.getPost_id());
								List<Like> lkl = getLikes(wp.getGroup_id(), wp.getPost_id(), wp.getDate());
								synchronized (comsToCommit) {
									comsToCommit.addAll(wcl);
									likesToCommit.addAll(lkl);
								}
							}
						};
						executor.execute(worker);
					}

					executor.shutdown();
					while (!executor.isTerminated()) {
						Thread.sleep(50);
					}

					// write to db
					DBConnector.insertPostsWithComments(postsToCommit, comsToCommit, likesToCommit);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Wall " + wall_id + " was not parsed");
		}
	}

	public static List<WallComment> getComments(long wall_id, long post_id) {
		List<WallComment> comments = new ArrayList<WallComment>();
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
						request = "http://api.vk.com/method/wall.getComments?owner_id=" + wall_id + "&post_id="
								+ post_id + "&offset=" + offset + "&count=" + count + "&v=5.45";
						offset += count;

						response = sendGETtimeout(request, 11);

						JSONParser jp = new JSONParser();
						JSONObject jsonresponse = (JSONObject) jp.parse(response);
						JSONObject resp = (JSONObject) jsonresponse.get("response");
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
			e.printStackTrace();
		}
		return comments;
	}

	public static List<Like> getLikes(long wall_id, long post_id, Date date) {
		List<Like> likes = new ArrayList<Like>();
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
						request = "http://api.vk.com/method/likes.getList?type=post&owner_id=" + wall_id + "&item_id="
								+ post_id + "&offset=" + offset + "&count=" + count + "&v=5.45";
						offset += count;
						response = sendGETtimeout(request, 11);

						JSONParser jp = new JSONParser();
						JSONObject jsonresponse = (JSONObject) jp.parse(response);
						JSONObject resp = (JSONObject) jsonresponse.get("response");
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
			e.printStackTrace();
		}
		return likes;
	}
}
