package etu.wollen.vk;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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
			if(!ifp.parseFileGroups("gr_list.txt")){
				return;
			}
			ArrayList<String> grList = ifp.getGrList();
			long find_id = ifp.getFind_id();
			String find_pattern = ifp.getFind_pattern();
			boolean byId = ifp.isById();
			Date dateRestr = ifp.getDateRestr();
			if(byId){
				System.out.println("ID to find: " + find_id);
			}
			else{
				System.out.println("Pattern to find: " + find_pattern);
			}
			System.out.println("Date restriction: " + dateRestr);
			System.out.println("Parsing " + grList.size() + " groups: " + grList);

			// get set of group id's using list of short names
			PostDownloader pd = new PostDownloader();
			pd.fillGroupNames(grList);

			// if started with -skip then skip parsing group, just search
			if (!(args.length > 0 && args[0].equals("-skip"))) {
				pd.parseGroups(dateRestr);
			} else {
				System.out.println("Parsing skipped!");
			}

			// start searching for comments and posts, results to file
			System.out.println("Start searching...  after date: " + dateRestr);
			String outname = "output.txt";
			PrintStream st = new PrintStream(new FileOutputStream(outname));
			PrintStream standard = System.out;
			System.setOut(st);
			Set<Long> grSet = pd.getGroupSet();
			
			ArrayList<WallPost> posts = null;
			ArrayList<WallComment> comments = null;

			//by signer id
			if(byId){
				posts = findPostsBySigner(find_id, grSet, dateRestr);
				comments = findCommentsBySigner(find_id, grSet, dateRestr);
			}
			//by pattern
			else{
				posts = findPostsByPattern(find_pattern, grSet, dateRestr);
				comments = findCommentsByPattern(find_pattern, grSet, dateRestr);
			}
			System.out.println(posts.size() + " posts found!" + System.lineSeparator());
			posts.sort(new WPcomparator());
			for (WallPost w : posts) {
				w.print();
				System.out.print(System.lineSeparator());
			}
			System.out.println(System.lineSeparator() + System.lineSeparator()+comments.size() + " comments found!" + System.lineSeparator());
			comments.sort(new WCcomparator());
			for (WallComment w : comments) {
				w.print();
				System.out.print(System.lineSeparator());
			}
			
			System.setOut(standard);
			System.out.println("Program finished! Results in file: " + outname);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<WallPost> findPostsBySigner(long signer_id, Set<Long> set, Date dateRestr) throws SQLException {
		ArrayList<WallPost> res = new ArrayList<WallPost>();
		ArrayList<WallPost> wp = DBConnector.getPostsBySinger(signer_id);
		for (WallPost w : wp) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}

	public static ArrayList<WallComment> findCommentsBySigner(long signer_id, Set<Long> set, Date dateRestr) throws SQLException {
		ArrayList<WallComment> res = new ArrayList<WallComment>();
		ArrayList<WallComment> wc = DBConnector.getCommentsBySigner(signer_id);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}
	
	
	public static ArrayList<WallPost> findPostsByPattern(String regex, Set<Long> set, Date dateRestr) throws SQLException {
		ArrayList<WallPost> res = new ArrayList<WallPost>();
		ArrayList<WallPost> wp = DBConnector.getAllPosts();
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		for (WallPost w : wp) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				if(pattern.matcher(w.getText()).find()){
					res.add(w);
				}
			}
		}
		return res;
	}
	
	public static ArrayList<WallComment> findCommentsByPattern(String regex, Set<Long> set, Date dateRestr) throws SQLException {
		ArrayList<WallComment> res = new ArrayList<WallComment>();
		ArrayList<WallComment> wc = DBConnector.getAllComments();
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				if(pattern.matcher(w.getText()).find()){
					res.add(w);
				}
			}
		}
		return res;
	}
}
