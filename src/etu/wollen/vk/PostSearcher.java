package etu.wollen.vk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class PostSearcher {

	public static void main(String[] args) {
		try {
			// connect and create DB
			DBConnector.connect();
			// DBConnector.deleteDB();
			DBConnector.createDB();

			// gather groups to search from file
			InputFileParser ifp = new InputFileParser();
			if (!ifp.parseFileGroups("gr_list.txt")) {
				return;
			}
			List<String> grList = ifp.getGrList();
			
			/// TODO extract properties to conf.properties file
			long find_id = ifp.getFind_id();
			String find_pattern = ifp.getFind_pattern();
			boolean byId = ifp.isById();
			Date dateRestr = ifp.getDateRestr();
			String access_token = ifp.getAcessToken();
			
			if (byId) {
				System.out.println("ID to find: " + find_id);
			} else {
				System.out.println("Pattern to find: " + find_pattern);
			}
			System.out.println("Date restriction: " + dateRestr);
			System.out.println("Parsing " + grList.size() + " groups: " + grList);

			// get set of group id's using list of short names
			PostDownloader pd = new PostDownloader(access_token);
			pd.fillGroupNames(grList);

			// if started with -skip then skip parsing group, just search
			if (!(args.length > 0 && args[0].equals("-skip"))) {
				pd.parseGroups(dateRestr);
			} else {
				System.out.println("Parsing skipped!");
			}

			// start searching for comments and posts, results to file
			System.out.println("Start searching...  after date: " + dateRestr);
			Map<Long, String> groupNames = pd.getGroupNames();
			List<WallPost> posts = null;
			List<WallComment> comments = null;
			List<WallComment> answers = null;
			List<Like> likes = null;

			// by signer id
			if (byId) {
				posts = findPostsBySigner(find_id, groupNames.keySet(), dateRestr);
				comments = findCommentsBySigner(find_id, groupNames.keySet(), dateRestr);
				answers = findCommentsByReply(find_id, groupNames.keySet(), dateRestr);
				likes = findLikesByUser(find_id, groupNames.keySet(), dateRestr);
			}
			// by pattern
			else {
				posts = findPostsByPattern(find_pattern, groupNames.keySet(), dateRestr);
				comments = findCommentsByPattern(find_pattern, groupNames.keySet(), dateRestr);
			}
			posts.sort(new WPcomparator());
			comments.sort(new WCcomparator());
			answers.sort(new WCcomparator());
			likes.sort(new Lcomparator());
			
			// output to file
			System.out.println("Saving results...");
			String outname1 = "output_by_date.txt";
			String outname2 = "output_by_group.txt";
			printToFile(outname1, find_id, false, groupNames, posts, comments, answers, likes);
			printToFile(outname2, find_id, true, groupNames, posts, comments, answers, likes);
			System.out.println("Program finished!");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void printToFile(String output, long id, boolean cluster, Map<Long, String> groupNames, List<WallPost> posts, List<WallComment> comments, List<WallComment> answers, List<Like> likes){
		PrintStream standard = System.out;
		PrintStream st;
		try {
			st = new PrintStream(new FileOutputStream(output));
			System.setOut(st);
			System.out.println("ID: "+id + System.lineSeparator());
			
			if(cluster){
				System.out.println(posts.size() + " posts found!" + System.lineSeparator());
				System.out.println(comments.size() + " comments found!" + System.lineSeparator());
				System.out.println(answers.size() + " answers found!" + System.lineSeparator());
				System.out.println(likes.size() + " post likes found!" + System.lineSeparator());
				for(Map.Entry<Long, String> group : groupNames.entrySet()){
					long group_id = -group.getKey(); // group value without minus here
					String name = group.getValue();
					List<WallPost> selectedPosts = WallPost.getByGroupId(posts, group_id);
					List<WallComment> selectedComments = WallComment.getByGroupId(comments, group_id);
					List<WallComment> selectedAnswers = WallComment.getByGroupId(answers, group_id);
					List<Like> selectedLikes = Like.getByGroupId(likes, group_id);
					
					if(!selectedPosts.isEmpty() || !selectedComments.isEmpty() || !selectedAnswers.isEmpty() || !selectedLikes.isEmpty()){
						System.out.println("<<< " + name + " >>>");
						for (WallPost w : selectedPosts) {
							w.print();
							System.out.print(System.lineSeparator());
						}
						for (WallComment w : selectedComments) {
							w.print();
							System.out.print(System.lineSeparator());
						}
						for (WallComment w : selectedAnswers) {
							w.print();
							System.out.print(System.lineSeparator());
						}
						for (Like l : selectedLikes) {
							l.print();
							System.out.print(System.lineSeparator());
						}
						System.out.println(System.lineSeparator()+System.lineSeparator());
					}
				}
			}
			else{
				System.out.println(posts.size() + " posts found!" + System.lineSeparator());
				for (WallPost w : posts) {
					w.print();
					System.out.print(System.lineSeparator());
				}
				System.out.println(System.lineSeparator() + System.lineSeparator() + comments.size() + " comments found!"
						+ System.lineSeparator());
				for (WallComment w : comments) {
					w.print();
					System.out.print(System.lineSeparator());
				}
				System.out.println(System.lineSeparator() + System.lineSeparator() + answers.size() + " answers found!"
						+ System.lineSeparator());
				for (WallComment w : answers) {
					w.print();
					System.out.print(System.lineSeparator());
				}
				System.out.println(System.lineSeparator() + System.lineSeparator() + likes.size() + " post likes found!"
						+ System.lineSeparator());
				for (Like l : likes) {
					l.print();
					System.out.print(System.lineSeparator());
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally{
			System.setOut(standard);
		}
	}

	public static List<WallPost> findPostsBySigner(long signer_id, Set<Long> set, Date dateRestr) throws SQLException {
		List<WallPost> res = new ArrayList<WallPost>();
		List<WallPost> wp = DBConnector.getPostsBySinger(signer_id);
		for (WallPost w : wp) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}

	public static List<WallComment> findCommentsBySigner(long signer_id, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<WallComment> res = new ArrayList<WallComment>();
		List<WallComment> wc = DBConnector.getCommentsBySigner(signer_id);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}

	public static List<WallComment> findCommentsByReply(long signer_id, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<WallComment> res = new ArrayList<WallComment>();
		List<WallComment> wc = DBConnector.getCommentsByReply(signer_id);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}
	
	public static List<Like> findLikesByUser(long user, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<Like> res = new ArrayList<>();
		List<Like> likes = DBConnector.getLikesByUser(user);
		for (Like l : likes) {
			if (set.contains(l.getOwner_id() * (-1)) && l.getDate().getTime() >= dateRestr.getTime()) {
				res.add(l);
			}
		}
		return res;
	}

	public static List<WallPost> findPostsByPattern(String regex, Set<Long> set, Date dateRestr) throws SQLException {
		List<WallPost> res = new ArrayList<WallPost>();
		List<WallPost> wp = DBConnector.getAllPosts();
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		for (WallPost w : wp) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				if (pattern.matcher(w.getText()).find()) {
					res.add(w);
				}
			}
		}
		return res;
	}

	public static List<WallComment> findCommentsByPattern(String regex, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<WallComment> res = new ArrayList<WallComment>();
		List<WallComment> wc = DBConnector.getAllComments();
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				if (pattern.matcher(w.getText()).find()) {
					res.add(w);
				}
			}
		}
		return res;
	}
}
