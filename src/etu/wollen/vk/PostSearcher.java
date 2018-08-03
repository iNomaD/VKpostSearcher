package etu.wollen.vk;

import etu.wollen.vk.api.PostDownloader;
import etu.wollen.vk.api.UserDownloader;
import etu.wollen.vk.conf.Config;
import etu.wollen.vk.conf.ConfigParser;
import etu.wollen.vk.database.DatabaseUtils;
import etu.wollen.vk.models.*;
import etu.wollen.vk.transport.HttpClient;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.*;

public class PostSearcher {

	private static boolean getFlag(String[] args, String flag){
		if(args.length > 0){
			for(String arg : args){
				if(arg.equals(flag)) return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		boolean skip = getFlag(args, "-skip");
		boolean friends = getFlag(args, "-friends");
		boolean debug = getFlag(args, "-debug");

		try {
			try {
				// connect and create DB
				DatabaseUtils.createDB();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Can't initialize database");
				return;
			}

			// gather groups to search from file
			HttpClient.getInstance().setDebugEnabled(debug);
			ConfigParser configParser = new ConfigParser();
			Config config;
			try {
				config = configParser.parseFileGroups("gr_list.txt");
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error occurred while loading the configuration");
				return;
			}

			// prepare for searching
			if (config.isById()) {
				System.out.println("User to find: " + config.getFindUser());
			} else {
				System.out.println("Pattern to find: " + config.getFindPattern());
			}
			System.out.println("Date restriction: " + config.getDateRestriction());
			System.out.println("Processing " + config.getGroupList() + " groups: " + config.getGroupList());

			// obtain the set of group ids using list of short names
			PostDownloader pd = new PostDownloader(config.getGroupList(), config.getAccessToken());

			// if started with -skip then skip parsing group, just search
			if (!skip) {
				pd.parseGroups(config.getDateRestriction());
			} else {
				System.out.println("Parsing skipped!");
			}

			// start searching for comments and posts, results to file
			System.out.println("Start searching...  after date: " + config.getDateRestriction());
			List<User> findUsers = config.isById() ? new ArrayList<User>() {{
				add(config.getFindUser());
			}} : null;
			findData(pd.getGroupNames(), config.getDateRestriction(),
					config.isById(), findUsers, config.getFindPattern(), "output");

			// process friends of the user if -friends flag is enabled and userId is valid
			if (friends && config.isById()) {
				System.out.println("Downloading friends...");
				try {
					List<User> friendsList = new UserDownloader(config.getAccessToken()).getFriends(config.getFindUser());
					System.out.println("Found " + friendsList.size() + " friends");
					if (friendsList.size() > 0) {
						findData(pd.getGroupNames(), config.getDateRestriction(),
								true, friendsList, null, "output_friends");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			System.out.println("Program finished!");
		}
		finally {
			DatabaseUtils.closeDB();
		}
	}

	private static void findData(Map<Long, String> groupNames, Date dateRestriction,
								 boolean byId, List<User> findUsers, String findPattern,
								 String outputFileName){
		List<WallPost> posts = new ArrayList<>();
		List<WallComment> comments = new ArrayList<>();
		List<WallComment> answers = new ArrayList<>();
		List<Like> likes = new ArrayList<>();

		// by signer id
		try {
			if (byId) {
				for(User user: findUsers){
					posts.addAll(findPostsBySigner(user.getId(), groupNames.keySet(), dateRestriction));
					comments.addAll(findCommentsBySigner(user.getId(), groupNames.keySet(), dateRestriction));
					answers.addAll(findCommentsByReply(user.getId(), groupNames.keySet(), dateRestriction));
					likes.addAll(findLikesByUser(user.getId(), groupNames.keySet(), dateRestriction));
				}
			}
			// by pattern
			else {
				posts = findPostsByPattern(findPattern, groupNames.keySet(), dateRestriction);
				comments = findCommentsByPattern(findPattern, groupNames.keySet(), dateRestriction);
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
		User findUser = null;
        Map<Long, User> usersMap = null;
		if(byId) {
			if(findUsers.size() == 1){
				findUser = findUsers.get(0);
			}
			else if(findUsers.size() > 1){
                usersMap = new HashMap<>();
                for(User user: findUsers){
                    usersMap.putIfAbsent(user.getId(), user);
                }
            }
		}
		String outputByDate = outputFileName+"_by_date.txt";
		String outputByGroup = outputFileName+"_by_group.txt";
		printToFile(usersMap, findUser, findPattern, false, groupNames, posts, comments, answers, likes, outputByDate);
		printToFile(usersMap, findUser, findPattern, true, groupNames, posts, comments, answers, likes, outputByGroup);
	}
	
	private static void printToFile(Map<Long, User> usersMap, User user, String pattern, boolean cluster, Map<Long, String> groupNames,
                                    List<WallPost> posts, List<WallComment> comments, List<WallComment> answers, List<Like> likes,
                                    String outputFileName){
		PrintStream standard = System.out;
		try (PrintStream st = new PrintStream(new FileOutputStream(outputFileName))) {
			System.setOut(st);
			if(usersMap != null){
			    StringBuilder users = new StringBuilder();
			    for(User friend : usersMap.values()){
			        users.append(friend).append(", ");
                }
                System.out.println("Users: " + users + System.lineSeparator());
            }
			else if (user != null) {
				System.out.println("User: " + user + System.lineSeparator());
			} else {
				System.out.println("Pattern: [" + pattern + "]" + System.lineSeparator());
			}

			if (cluster) {
				System.out.println(posts.size() + " posts found!" + System.lineSeparator());
				System.out.println(comments.size() + " comments found!" + System.lineSeparator());
				if (user != null || usersMap != null) System.out.println(answers.size() + " answers found!" + System.lineSeparator());
				if (user != null || usersMap != null) System.out.println(likes.size() + " post likes found!" + System.lineSeparator());
				for (Map.Entry<Long, String> group : groupNames.entrySet()) {
					long group_id = -group.getKey(); // group value without minus here
					String name = group.getValue();
					List<WallPost> selectedPosts = WallPost.getByGroupId(posts, group_id);
					List<WallComment> selectedComments = WallComment.getByGroupId(comments, group_id);
					List<WallComment> selectedAnswers = WallComment.getByGroupId(answers, group_id);
					List<Like> selectedLikes = Like.getByGroupId(likes, group_id);

					if (!selectedPosts.isEmpty() || !selectedComments.isEmpty() || !selectedAnswers.isEmpty() || !selectedLikes.isEmpty()) {
						System.out.println("<<< " + name + " >>>");
						printPosts(selectedPosts, usersMap);
						printComments(selectedComments, usersMap);
						printAnswers(selectedAnswers, usersMap);
						printLikes(selectedLikes, usersMap);
						System.out.println(System.lineSeparator() + System.lineSeparator());
					}
				}
			} else {
				System.out.println(posts.size() + " posts found!" + System.lineSeparator());
				printPosts(posts, usersMap);

				System.out.println(System.lineSeparator() + System.lineSeparator() + comments.size() + " comments found!"
						+ System.lineSeparator());
				printComments(comments, usersMap);

				System.out.println(System.lineSeparator() + System.lineSeparator() + answers.size() + " answers found!"
						+ System.lineSeparator());
				printAnswers(answers, usersMap);

				System.out.println(System.lineSeparator() + System.lineSeparator() + likes.size() + " post likes found!"
						+ System.lineSeparator());
				printLikes(likes, usersMap);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			System.setOut(standard);
		}
	}

	private static void printPosts(List<WallPost> wallPosts, Map<Long, User> usersMap){
        for (WallPost w : wallPosts) {
            if(usersMap != null) System.out.println(usersMap.get(w.getSignerId()));
            w.print();
            System.out.print(System.lineSeparator());
        }
    }

    private static void printComments(List<WallComment> wallComments, Map<Long, User> usersMap){
        for (WallComment w : wallComments) {
            if(usersMap != null) System.out.println(usersMap.get(w.getFrom_id()));
            w.print();
            System.out.print(System.lineSeparator());
        }
    }

    private static void printAnswers(List<WallComment> answers, Map<Long, User> usersMap){
        for (WallComment w : answers) {
            if(usersMap != null) System.out.println(usersMap.get(w.getReply_to_user()));
            w.print();
            System.out.print(System.lineSeparator());
        }
    }

    private static void printLikes(List<Like> likes, Map<Long, User> usersMap){
        for (Like l : likes) {
            if(usersMap != null) System.out.println(usersMap.get(l.getUser()));
            l.print();
            System.out.print(System.lineSeparator());
        }
    }

	private static List<WallPost> findPostsBySigner(long signer_id, Set<Long> set, Date dateRestr) throws SQLException {
		List<WallPost> res = new ArrayList<>();
		List<WallPost> wp = DatabaseUtils.getPostsBySigner(signer_id);
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
		List<WallComment> wc = DatabaseUtils.getCommentsBySigner(signer_id);
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
		List<WallComment> wc = DatabaseUtils.getCommentsByReply(signer_id);
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
		List<Like> likes = DatabaseUtils.getLikesByUser(user);
		for (Like l : likes) {
			if (set.contains(l.getOwnerId() * (-1)) && l.getDate().getTime() >= dateRestr.getTime()) {
				res.add(l);
			}
		}
		return res;
	}

	private static List<WallPost> findPostsByPattern(String regex, Set<Long> set, Date dateRestr) throws SQLException {
		List<WallPost> res = new ArrayList<>();
		List<WallPost> wp = DatabaseUtils.getPostsByPattern(regex);
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
		List<WallComment> wc = DatabaseUtils.getCommentsByPattern(regex);
		for (WallComment w : wc) {
			if (set.contains(w.getGroup_id() * (-1)) && w.getDate().getTime() >= dateRestr.getTime()) {
				res.add(w);
			}
		}
		return res;
	}
}
