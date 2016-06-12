package etu.wollen.vk;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class PostSearcher {
	
	public static void main(String[] args) {
		try {
			// connect and create DB
			DBConnector.connect();
			// DBConnector.deleteDB();
			DBConnector.createDB();

			// gather groups to search from file
			InputFileParser ifp = new InputFileParser();
			ifp.parseFileGroups("gr_list.txt");
			ArrayList<String> grList = ifp.getGrList();
			long find_id = ifp.getFind_id();
			String find_pattern = ifp.getFind_pattern();
			boolean byId = ifp.isById();
			Date dateRestr = ifp.getDateRestr();
			System.out.println("ID to find: " + find_id);
			System.out.println("Pattern to find: " + find_pattern);
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
			System.out.println("Start searching... id=" + find_id + " after date: " + dateRestr);
			String outname = "output.txt";
			PrintStream st = new PrintStream(new FileOutputStream(outname));
			PrintStream standard = System.out;
			System.setOut(st);
			Set<Long> grSet = pd.getGroupSet();
			
			//by signer id
			if(byId){
				ArrayList<WallPost> posts = findPostsBySigner(find_id, grSet, dateRestr);
				System.out.println(posts.size() + " posts found!" + System.lineSeparator());
				posts.sort(new WPcomparator());
				for (WallPost w : posts) {
					w.print();
					System.out.print(System.lineSeparator());
				}
				
				ArrayList<WallComment> comments = findCommentsBySigner(find_id, grSet, dateRestr);
				System.out.println(System.lineSeparator() + System.lineSeparator()+comments.size() + " comments found!" + System.lineSeparator());
				comments.sort(new WCcomparator());
				for (WallComment w : comments) {
					w.print();
					System.out.print(System.lineSeparator());
				}
			}
			//by pattern
			else{
				
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
	
	/*
	public static void findPostsByPattern(long signer_id, Set<Long> set, Date dateRestr) throws SQLException {
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

	public static void findCommentsByPattern(long signer_id, Set<Long> set, Date dateRestr) throws SQLException {
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
	*/
}
