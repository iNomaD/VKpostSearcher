package etu.wollen.vk;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PostDownloader {
	private static long id_to_find = 1;
	private static Date dateRestr = new Date();
	private static final boolean showInfo = false;

	public static void main(String[] args) {
		PostDownloader pd = new PostDownloader();

		try {
			// connect and create DB
			DBConnector.connect();
			// DBConnector.deleteDB();
			DBConnector.createDB();

			// gather groups to search from file
			ArrayList<String> grList = pd.parseFileGroups("gr_list.txt");
			System.out.println("Parsing " + grList.size() + " groups: " + grList);

			// get set of group ids using list of short names
			Set<Long> grSet = new HashSet<Long>(pd.getGroupIds(grList));

			// if started with -skip then skip parsing group, just search
			if (!(args.length > 0 && args[0].equals("-skip"))) {
				pd.parseGroups(grSet);
			} else {
				System.out.println("Parsing skipped!");
			}

			// start searching for comments and posts, results to file
			System.out.println("Start searching... id=" + id_to_find + " after date: " + dateRestr);
			String outname = "output.txt";
			PrintStream st = new PrintStream(new FileOutputStream(outname));
			PrintStream standard = System.out;
			System.setOut(st);
			pd.findPosts(id_to_find, grSet);
			pd.findComments(id_to_find, grSet);
			System.setOut(standard);
			System.out.println("Program finished! Results in file: " + outname);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ArrayList<String> parseFileGroups(String filename) throws IOException {
		ArrayList<String> grList = new ArrayList<String>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			String id_str = br.readLine();
			id_to_find = Long.parseLong(id_str);
			System.out.println("ID to find: " + id_to_find);

			String date_str = br.readLine();
			String[] pts = date_str.split("\\.");
			Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(pts[2]), Integer.parseInt(pts[1]) - 1, Integer.parseInt(pts[0]));
			dateRestr = cal.getTime();
			System.out.println("Date restriction: " + dateRestr);

			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.equals("")) {
					line = line.replaceAll(".*/", "");
					grList.add(line);
				}
			}
		} catch (IOException e) {
			System.out.println("Error reading file");
			e.printStackTrace();
		} finally {
			br.close();
		}

		return grList;
	}

	public ArrayList<Long> getGroupIds(ArrayList<String> groupShortNames) {
		ArrayList<Long> groupIds = new ArrayList<Long>();
		if (!groupShortNames.isEmpty()) {
			String request = "http://api.vk.com/method/groups.getById?group_ids=";
			for (String gr : groupShortNames) {
				request += gr + ",";
			}
			try {
				String response = sendGETtimeout(request, 5);

				JSONParser jp = new JSONParser();
				JSONObject jsonresponse = (JSONObject) jp.parse(response);
				JSONArray items = (JSONArray) jsonresponse.get("response");
				for (Object oitem : items) {
					JSONObject item = (JSONObject) oitem;
					long gid = (long) item.get("gid");
					groupIds.add(gid);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return groupIds;
	}

	public void parseGroups(Set<Long> groupIds) {
		int count = 0;
		for (Long wall_id : groupIds) {
			System.out.println("Parsing vk.com/club" + wall_id + "    " + count + " of " + groupIds.size());
			getPosts(wall_id * (-1));
			++count;
		}
	}

	public void findPosts(long signer_id, Set<Long> set) throws SQLException {
		ArrayList<WallPost> wp = DBConnector.getAllPosts();
		int count = 0;
		for (WallPost w : wp) {
			if (w.getSigner_id() == signer_id && set.contains(w.getGroup_id() * (-1))
					&& w.getDate().getTime() >= dateRestr.getTime()) {
				w.print();
				++count;
			}
		}
		System.out.println(count + " posts found!" + System.lineSeparator() + System.lineSeparator());
	}

	public void findComments(long signer_id, Set<Long> set) throws SQLException {
		ArrayList<WallComment> wc = DBConnector.getAllComments();
		int count = 0;
		for (WallComment w : wc) {
			if (w.getFrom_id() == signer_id && set.contains(w.getGroup_id() * (-1))
					&& w.getDate().getTime() >= dateRestr.getTime()) {
				w.print();
				++count;
			}
		}
		System.out.println(count + " comments found!");
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
			} catch (ConnectException e) {
				if (i < attempts - 1) {
					System.out.println("Connection timed out... " + (attempts - i - 1) + " more attempts...");
				} else {
					throw e;
				}
			}
		}
		return response;
	}

	public void getPosts(long wall_id) {
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

				String response = sendGETtimeout(request, 5);

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
				String request = "https://api.vk.com/method/wall.getComments?owner_id=" + wall_id + "&post_id="
						+ post_id + "&offset=" + offset + "&count=" + count + "&v=5.45";
				offset += 100;

				String response = sendGETtimeout(request, 5);
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
