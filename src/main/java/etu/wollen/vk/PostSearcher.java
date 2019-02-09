package etu.wollen.vk;

import etu.wollen.vk.api.PostDownloader;
import etu.wollen.vk.api.UserDownloader;
import etu.wollen.vk.conf.Config;
import etu.wollen.vk.conf.ConfigParser;
import etu.wollen.vk.database.DatabaseUtils;
import etu.wollen.vk.database.DatabaseWrapper;
import etu.wollen.vk.exceptions.ConfigParseException;
import etu.wollen.vk.model.conf.SearchOptions;
import etu.wollen.vk.model.conf.User;
import etu.wollen.vk.model.database.*;
import etu.wollen.vk.transport.HttpClient;
import etu.wollen.vk.utils.PrintUtils;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static etu.wollen.vk.utils.CommandLineUtils.getFlag;
import static etu.wollen.vk.utils.CommandLineUtils.getParameter;

public class PostSearcher {

	private static final String INITIAL_FILE_NAME = "input.txt";

	public static void main(String[] args) {
		boolean skipParsing = getFlag(args, "-skip");
		boolean friendsEnabled = getFlag(args, "-friends");
		boolean debugEnabled = getFlag(args, "-debug");
		boolean privateGroupsEnabled = getFlag(args, "-private");
		String sqliteDatabase = getParameter(args, "-sqlite:");
		if (sqliteDatabase == null || sqliteDatabase.isEmpty() || !sqliteDatabase.endsWith(".s3db")) {
			System.out.println("Missing argument -sqlite:{filename}.s3db");
			return;
		}

		try {
			// connect and create DB
			DatabaseWrapper.getInstance().setDatabaseFilename(sqliteDatabase);
			DatabaseUtils.createDB();

			// parse config from initial file
			HttpClient.getInstance().setDebugEnabled(debugEnabled);
			HttpClient.getInstance().setDelay(privateGroupsEnabled ? Config.HTTP_DELAY : 0);
			ConfigParser configParser = new ConfigParser();
			Config config = configParser.parseFileGroups(INITIAL_FILE_NAME);

			// prepare for searching
			if (config.isById()) {
				System.out.println("User to find: " + config.getFindUser());
			} else {
				System.out.println("Pattern to find: " + config.getFindPattern());
			}
			System.out.println("Date restriction: " + config.getDateRestriction());
			System.out.println("Groups to process: " + config.getGroupList());
			System.out.println("Boards to process: " + config.getBoardList());

			// obtain the set of group ids using list of short names
			PostDownloader pd = new PostDownloader(config.getGroupList(), config.getBoardList(),
					config.getAccessToken(), privateGroupsEnabled);

			// if started with -skip then skip parsing group, just search
			if (!skipParsing) {
				pd.parseGroups(config.getDateRestriction());
			} else {
				System.out.println("Parsing skipped!");
			}

			// start searching for comments and posts, results to file
			System.out.println("Start searching...  after date: " + config.getDateRestriction());
			Date dateRestriction = config.getDateRestriction();
			Map<Long, String> groupNames = pd.getGroupNames();
			SearchOptions searchOptions = config.isById()
					? SearchOptions.of(config.getFindUser(), dateRestriction, groupNames)
					: SearchOptions.of(config.getFindPattern(), dateRestriction, groupNames);

			findData(searchOptions, "output");

			// process friends of the user if -friends flag is enabled and userId is valid
			if (friendsEnabled && config.isById()) {
				System.out.println("Downloading friends...");
				try {
					List<User> friendsList = new UserDownloader(config.getAccessToken()).getFriends(config.getFindUser());
					System.out.println("Found " + friendsList.size() + " friends");
					if (friendsList.size() > 0) {
						findData(SearchOptions.of(friendsList, dateRestriction, groupNames), "output_friends");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			System.out.println("Program finished successfully!");
		}
		catch (SQLException e){
			e.printStackTrace();
			System.err.println("Database error");
		}
		catch (ConfigParseException e){
			e.printStackTrace();
			System.err.println("Error occurred while loading the configuration");
		}
		catch (FileNotFoundException e){
			e.printStackTrace();
			System.err.println("Filesystem error");
		}
		catch (Exception e){
			e.printStackTrace();
			System.err.println("Unknown error");
		}
		finally {
			try {
				DatabaseUtils.closeDB();
			}
			catch (Exception e){
				e.printStackTrace();
				System.err.println("Can't close database");
			}
		}
	}

	private static void findData(SearchOptions so, String outputFileName) throws SQLException, FileNotFoundException {

		List<WallPost> posts = new ArrayList<>();
		List<WallPostComment> comments = new ArrayList<>();
		List<BoardComment> boardComments = new ArrayList<>();
		List<WallPostComment> answers = new ArrayList<>();
		List<WallPostLike> likes = new ArrayList<>();

		// by user id
		if (so.getSearchType() != SearchOptions.SearchType.BY_PATTERN) {
			for(User user: so.getFindUsers().values()){
				posts.addAll(findPostsBySigner(user.getId(), so.getGroupNames().keySet(), so.getDateRestriction()));
				comments.addAll(findCommentsBySigner(user.getId(), so.getGroupNames().keySet(), so.getDateRestriction()));
				// TODO boardComments
				answers.addAll(findCommentsByReply(user.getId(), so.getGroupNames().keySet(), so.getDateRestriction()));
				likes.addAll(findLikesByUser(user.getId(), so.getGroupNames().keySet(), so.getDateRestriction()));
			}
		}
		// by pattern
		else {
			posts = findPostsByPattern(so.getFindPattern(), so.getGroupNames().keySet(), so.getDateRestriction());
			comments = findCommentsByPattern(so.getFindPattern(), so.getGroupNames().keySet(), so.getDateRestriction());
			// TODO boardComments
			answers = new ArrayList<>();
			likes = new ArrayList<>();
		}

		// sort
		Comparator<BaseSearchableEntity> recentFirstComparator = (w1, w2) -> w2.getDate().compareTo(w1.getDate());
		posts.sort(recentFirstComparator);
		comments.sort(recentFirstComparator);
		boardComments.sort(recentFirstComparator);
		answers.sort(recentFirstComparator);
		likes.sort(recentFirstComparator);

		// output to file
		String outputByDateFilename = outputFileName+"_by_date.txt";
		String outputByGroupFilename = outputFileName+"_by_group.txt";
		PrintUtils.printToFile(so, false, posts, comments, boardComments, answers, likes, outputByDateFilename);
		PrintUtils.printToFile(so, true, posts, comments, boardComments, answers, likes, outputByGroupFilename);
	}

	private static List<WallPost> findPostsBySigner(long user, Set<Long> allowedGroups, Date dateRestriction) throws SQLException {
		List<WallPost> wallPosts = DatabaseUtils.getPostsBySigner(user);
		return wallPosts
				.stream()
				.filter(a -> allowedGroups.contains(a.getGroupId() * (-1)) && !a.getDate().before(dateRestriction))
				.collect(Collectors.toList());
	}

	private static List<WallPostComment> findCommentsBySigner(long user, Set<Long> allowedGroups, Date dateRestriction) throws SQLException {
		List<WallPostComment> wallPostComments = DatabaseUtils.getCommentsBySigner(user);
		return wallPostComments
				.stream()
				.filter(a -> allowedGroups.contains(a.getGroupId() * (-1)) && !a.getDate().before(dateRestriction))
				.collect(Collectors.toList());
	}

	private static List<WallPostComment> findCommentsByReply(long user, Set<Long> allowedGroups, Date dateRestriction) throws SQLException {
		List<WallPostComment> wallPostComments = DatabaseUtils.getCommentsByReply(user);
		return wallPostComments
				.stream()
				.filter(a -> allowedGroups.contains(a.getGroupId() * (-1)) && !a.getDate().before(dateRestriction))
				.collect(Collectors.toList());
	}
	
	private static List<WallPostLike> findLikesByUser(long user, Set<Long> allowedGroups, Date dateRestriction) throws SQLException {
		List<WallPostLike> likes = DatabaseUtils.getLikesByUser(user);
		return likes
				.stream()
				.filter(a -> allowedGroups.contains(a.getOwnerId() * (-1)) && !a.getDate().before(dateRestriction))
				.collect(Collectors.toList());
	}

	private static List<WallPost> findPostsByPattern(String regex, Set<Long> allowedGroups, Date dateRestriction) throws SQLException {
		List<WallPost> wallPosts = DatabaseUtils.getPostsByPattern(regex, dateRestriction);
		return wallPosts
				.stream()
				.filter(a -> allowedGroups.contains(a.getGroupId() * (-1)))
				.collect(Collectors.toList());
	}

	private static List<WallPostComment> findCommentsByPattern(String regex, Set<Long> allowedGroups, Date dateRestriction) throws SQLException {
		List<WallPostComment> wallPostComments = DatabaseUtils.getCommentsByPattern(regex, dateRestriction);
		return wallPostComments
				.stream()
				.filter(a -> allowedGroups.contains(a.getGroupId() * (-1)))
				.collect(Collectors.toList());
	}
}
