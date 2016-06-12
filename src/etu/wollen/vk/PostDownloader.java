package etu.wollen.vk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PostDownloader {
	private Map<Long, String> groupNames = new HashMap<Long, String>();
	private static final boolean showInfo = false;

	public void fillGroupNames(ArrayList<String> groupShortNames) {
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
	
	public Set<Long> getGroupSet(){
		return groupNames.keySet();
	}

	public void parseGroups(Date dateRestr) {
		int count = 1;
		for (Map.Entry<Long, String> group : groupNames.entrySet()) {
			Long wall_id = group.getKey();
			String wall_name = group.getValue();
			System.out.println("Parsing vk.com/club" + wall_id + " ("+wall_name+")    " + count + " of " + groupNames.size());
			getPosts(wall_id * (-1), dateRestr);
			++count;
		}
	}

	public static void printAllPosts() throws SQLException {
		ArrayList<WallPost> wp = DBConnector.getAllPosts();
		for (WallPost w : wp) {
			w.print();
		}
	}

	public static void printAllComments() throws SQLException {
		ArrayList<WallComment> wc = DBConnector.getAllComments();
		for (WallComment w : wc) {
			w.print();
		}
	}

	// send GET and return response in UTF-8
	private String sendGET(String urlToRead) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		return result.toString();
	}

	private String sendGETtimeout(String request, int attempts) throws Exception {
		String response = "";
		for (int i = 0; i < attempts; ++i) {
			try {
				response = sendGET(request);
				break; //exit cycle
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

	public void getPosts(long wall_id, Date dateRestr) {
		long offset = 0;
		long count = 100;

		try {
			ArrayList<WallPost> allWall = DBConnector.getPostsFromWall(wall_id);
			int wallDBsize = allWall.size();
			Set<Long> allWallSet = new HashSet<Long>();
			for (WallPost wp : allWall) {
				allWallSet.add(wp.getPost_id());
			}

			boolean allRead = false;
			while (!allRead) {
				String request = "http://api.vk.com/method/wall.get?owner_id=" + wall_id + "&offset=" + offset
						+ "&count=" + count + "&v=5.45";
				offset += 100;

				String response = sendGETtimeout(request, 11);

				JSONParser jp = new JSONParser();
				JSONObject jsonresponse = (JSONObject) jp.parse(response);
				JSONObject resp = (JSONObject) jsonresponse.get("response");
				Object posts_count_object = resp.get("count");
				long posts_count = posts_count_object == null ? 0 : (long) posts_count_object;
				JSONArray items = (JSONArray) resp.get("items");
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
						if (wallDBsize >= posts_count) {
							allRead = true;
							System.out.println("All posts parsed");
							break;
						}

						if (showInfo) {
							System.out.println(wp.getPostUrl() + " " + wp.getDate() + " " + wallDBsize + " of "
									+ posts_count + " exists");
						}
					} else {
						// write to db
						DBConnector.insertPost(wp);
						++wallDBsize;
						allWallSet.add(wp.getPost_id());
						allWall.add(wp);

						// write comments too
						long coms = getComments(wp.getGroup_id(), wp.getPost_id());

						if (showInfo) {
							System.out.println(wp.getPostUrl() + " " + wp.getDate() + " ¹" + wallDBsize + " of "
									+ posts_count + " added with " + coms + " comments");
						}
					}

					// date restriction
					if (wp.getDate().getTime() < dateRestr.getTime()) {
						allRead = true;
						System.out.println("Date restriction reached! All posts parsed");
						break;
					}
				}
				if (items.size() == 0) {
					allRead = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public long getComments(long wall_id, long post_id) {
		long offset = 0;
		long count = 100;
		long commentsRead = 0;

		try {
			boolean allRead = false;

			while (!allRead) {
				String request = "http://api.vk.com/method/wall.getComments?owner_id=" + wall_id + "&post_id="
						+ post_id + "&offset=" + offset + "&count=" + count + "&v=5.45";
				offset += 100;

				String response = sendGETtimeout(request, 11);
				// System.out.println(response);

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
					WallComment wc = new WallComment(comment_id, from_id, new Date(date * 1000), text, wall_id,
							post_id);
					++commentsRead;

					// wc.print();
					DBConnector.insertComment(wc);
				}
				if (commentsRead >= comments_count) {
					allRead = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return commentsRead;
	}
}
