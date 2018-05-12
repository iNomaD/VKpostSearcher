package etu.wollen.vk;

import etu.wollen.vk.api.PostDownloader;
import etu.wollen.vk.conf.Config;
import etu.wollen.vk.conf.ConfigParser;
import etu.wollen.vk.database.DBConnector;
import etu.wollen.vk.models.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.*;

public class PostSearcher {

	public static void main(String[] args) {

	    try {
            // connect and create DB
            DBConnector.connect();
            // DBConnector.deleteDB();
            DBConnector.createDB();
        }
        catch (Exception e){
	        e.printStackTrace();
            System.out.println("Can't initialize database");
            return;
        }

        // gather groups to search from file
        ConfigParser configParser = new ConfigParser();
        Config config;
        try {
             config = configParser.parseFileGroups("gr_list.txt");
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Error occured while loading the configuration");
            return;
        }

        List<String> grList = config.getGroupList();

        /// TODO extract properties to conf.properties file

        User find_user = config.getFindUser();
        long find_id = -1;
        String find_pattern = config.getFindPattern();
        boolean byId = config.isById();
        Date dateRestr = config.getDateRestriction();
        String access_token = config.getAccessToken();

        if (byId) {
            find_id = find_user.getId();
            System.out.println("ID to find: " + find_id + " ("+find_user.getFirstName()+" "+find_user.getLastName()+")");
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
        List<WallPost> posts;
        List<WallComment> comments;
        List<WallComment> answers;
        List<Like> likes;

        // by signer id
        try {
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
                answers = new ArrayList<>();
                likes = new ArrayList<>();
            }
        }
        catch(Exception e){
            e.printStackTrace();
            return;
        }

        // sort
        posts.sort(new DateComparator());
        comments.sort(new DateComparator());
        answers.sort(new DateComparator());
        likes.sort(new DateComparator());


        // new ones first
        Collections.reverse(posts);
        Collections.reverse(comments);
        Collections.reverse(answers);
        Collections.reverse(likes);

        // output to file
        System.out.println("Saving results...");
        String outname1 = "output_by_date.txt";
        String outname2 = "output_by_group.txt";
        printToFile(outname1, find_user, find_pattern, false, groupNames, posts, comments, answers, likes);
        printToFile(outname2, find_user, find_pattern, true, groupNames, posts, comments, answers, likes);
        System.out.println("Program finished!");
	}
	
	private static void printToFile(String output, User user, String pattern, boolean cluster, Map<Long, String> groupNames, List<WallPost> posts, List<WallComment> comments, List<WallComment> answers, List<Like> likes){
		PrintStream standard = System.out;
		PrintStream st;
		try {
			st = new PrintStream(new FileOutputStream(output));
			System.setOut(st);
			if(user != null){
				System.out.println("ID: "+user.getId()+" ("+user.getFirstName()+" "+user.getLastName()+")"+System.lineSeparator());
			}
			else{
				System.out.println("Pattern: [" + pattern + "]"+ System.lineSeparator());
			}
			
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

	private static List<WallPost> findPostsBySigner(long signer_id, Set<Long> set, Date dateRestr) throws SQLException {
		List<WallPost> res = new ArrayList<>();
		List<WallPost> wp = DBConnector.getPostsBySigner(signer_id);
		for (WallPost w : wp) {
			if (set.contains(w.getGroupId() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}

	private static List<WallComment> findCommentsBySigner(long signer_id, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<WallComment> res = new ArrayList<>();
		List<WallComment> wc = DBConnector.getCommentsBySigner(signer_id);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}

	private static List<WallComment> findCommentsByReply(long signer_id, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<WallComment> res = new ArrayList<>();
		List<WallComment> wc = DBConnector.getCommentsByReply(signer_id);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}
	
	private static List<Like> findLikesByUser(long user, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<Like> res = new ArrayList<>();
		List<Like> likes = DBConnector.getLikesByUser(user);
		for (Like l : likes) {
			if (set.contains(l.getOwnerId() * (-1)) && l.getDate().getTime() >= dateRestr.getTime()) {
				res.add(l);
			}
		}
		return res;
	}

	private static List<WallPost> findPostsByPattern(String regex, Set<Long> set, Date dateRestr) throws SQLException {
		List<WallPost> res = new ArrayList<>();
		List<WallPost> wp = DBConnector.getPostsByPattern(regex);
		for (WallPost w : wp) {
			if (set.contains(w.getGroupId() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}

	private static List<WallComment> findCommentsByPattern(String regex, Set<Long> set, Date dateRestr)
			throws SQLException {
		List<WallComment> res = new ArrayList<>();
		List<WallComment> wc = DBConnector.getCommentsByPattern(regex);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}
}
