package etu.wollen.vk.database;

import etu.wollen.vk.model.database.BoardComment;
import etu.wollen.vk.model.database.WallPost;
import etu.wollen.vk.model.database.WallPostComment;
import etu.wollen.vk.model.database.WallPostLike;

import java.sql.*;
import java.util.Date;
import java.util.*;

public class DatabaseUtils {
	private static final String POSTS_TABLE_NAME = "POSTS";
	private static final String POST_ID_NAME = "POST_ID";
	private static final String POST_GROUP_NAME = "GROUP_ID";
	private static final String POST_SIGNER_ID_NAME = "POST_SIGNER_ID";
	private static final String POST_DATE_NAME = "POST_DATE";
	private static final String POST_TEXT_NAME = "POST_TEXT";
	private static final String POST_GROUP_INDEX = "GROUP_INDEX";
	private static final String POST_SIGNER_ID_INDEX = "POST_SIGNER_ID_INDEX";

	private static final String COMMENTS_TABLE_NAME = "COMMENTS";
	private static final String COMMENT_ID_NAME = "COMMENT_ID";
	private static final String COMMENT_FROM_ID_NAME = "COMMENT_FROM_ID";
	private static final String COMMENT_DATE_NAME = "COMMENT_DATE";
	private static final String COMMENT_TEXT_NAME = "COMMENT_TEXT";
	private static final String COMMENT_GROUP_ID_NAME = "COMMENT_GROUP_ID";
	private static final String COMMENT_POST_ID_NAME = "COMMENT_POST_ID";
	private static final String COMMENT_REPLY_NAME = "COMMENT_REPLY";
	private static final String COMMENT_FROM_ID_INDEX = "COMMENT_FROM_ID_INDEX";
	private static final String COMMENT_REPLY_INDEX = "COMMENT_REPLY_INDEX";

	private static final String LIKES_TABLE_NAME = "LIKES";
	private static final String LIKES_USER_NAME = "LIKES_USER";
	private static final String LIKES_TYPE_NAME = "LIKES_TYPE";
	private static final String LIKES_OWNER_NAME = "LIKES_OWNER";
	private static final String LIKES_ITEM_NAME = "LIKES_ITEM";
	private static final String LIKES_DATE_NAME = "LIKES_DATE";
	private static final String LIKES_USER_INDEX = "LIKES_USER_INDEX";

	private DatabaseUtils(){}

	public static void createDB() throws SQLException {
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement()) {

			stat.execute("CREATE TABLE if not exists " + POSTS_TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ POST_ID_NAME + " INTEGER , " + POST_GROUP_NAME + " INTEGER, " + POST_SIGNER_ID_NAME + " INTEGER, "
					+ POST_DATE_NAME + " INTEGER, " + POST_TEXT_NAME + " TEXT);");

			stat.execute("CREATE TABLE if not exists " + COMMENTS_TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ COMMENT_ID_NAME + " INTEGER, " + COMMENT_FROM_ID_NAME + " INTEGER, " + COMMENT_DATE_NAME
					+ " INTEGER, " + COMMENT_TEXT_NAME + " TEXT, " + COMMENT_GROUP_ID_NAME + " INTEGER, "
					+ COMMENT_POST_ID_NAME + " INTEGER, " + COMMENT_REPLY_NAME + " INTEGER);");

			stat.execute("CREATE TABLE if not exists " + LIKES_TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ LIKES_USER_NAME + " INTEGER , " + LIKES_TYPE_NAME + " TEXT, " + LIKES_OWNER_NAME + " INTEGER, "
					+ LIKES_ITEM_NAME + " INTEGER, " + LIKES_DATE_NAME + " INTEGER);");

			stat.execute("CREATE INDEX IF NOT EXISTS " + POST_GROUP_INDEX + " ON " + POSTS_TABLE_NAME + " (" + POST_GROUP_NAME + ");");
			stat.execute("CREATE INDEX IF NOT EXISTS " + POST_SIGNER_ID_INDEX + " ON " + POSTS_TABLE_NAME + " (" + POST_SIGNER_ID_NAME + ");");
			stat.execute("CREATE INDEX IF NOT EXISTS " + COMMENT_FROM_ID_INDEX + " ON " + COMMENTS_TABLE_NAME + " (" + COMMENT_FROM_ID_NAME + ");");
			stat.execute("CREATE INDEX IF NOT EXISTS " + COMMENT_REPLY_INDEX + " ON " + COMMENTS_TABLE_NAME + " (" + COMMENT_REPLY_NAME + ");");
			stat.execute("CREATE INDEX IF NOT EXISTS " + LIKES_USER_INDEX + " ON " + LIKES_TABLE_NAME + " (" + LIKES_USER_NAME + ");");
		}
	}

	public static void deleteDB() throws SQLException {
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement()) {
			stat.executeUpdate("drop table if exists " + POSTS_TABLE_NAME + ";");
			stat.executeUpdate("drop table if exists " + COMMENTS_TABLE_NAME + ";");
			stat.executeUpdate("drop table if exists " + LIKES_TABLE_NAME + ";");
			System.out.println("DB Tables deleted.");
		}
	}

	public static void closeDB() throws SQLException{
		DatabaseWrapper.getInstance().closeConnection();
	}
	
	public static Set<Long> getPostsIdSetFromWall(long wall_id) throws SQLException {
		Set<Long> posts = new HashSet<>();
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement();
			ResultSet resSet = stat.executeQuery("SELECT " + POST_ID_NAME + " FROM " + POSTS_TABLE_NAME + " WHERE " + POST_GROUP_NAME + " = " + wall_id)) {
				while (resSet.next()) {
					long post_id = resSet.getLong(POST_ID_NAME);
					posts.add(post_id);
				}
		}
		return posts;
	}

	public static Set<Long> getBoardCommentsIdSetFromWall(long groupId, long boardId) throws SQLException {
		// TODO
		return new HashSet<>();
	}

	public static List<WallPost> getPostsByPattern(String regex, Date dateRestriction) throws SQLException {
		List<WallPost> posts = new ArrayList<>();
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement();
			ResultSet resSet = stat.executeQuery("SELECT * FROM " + POSTS_TABLE_NAME + " WHERE " + POST_DATE_NAME + " >= " + dateRestriction.getTime() + " AND " + POST_TEXT_NAME + " REGEXP '" + regex + "'")) {
				while (resSet.next()) {
					long post_id = resSet.getLong(POST_ID_NAME);
					long group_id = resSet.getLong(POST_GROUP_NAME);
					long signer_id = resSet.getLong(POST_SIGNER_ID_NAME);
					long date = resSet.getLong(POST_DATE_NAME);
					String text = resSet.getString(POST_TEXT_NAME);
					posts.add(new WallPost(post_id, group_id, signer_id, new Date(date), text));
				}
			}
		return posts;
	}

	public static List<WallPost> getPostsBySigner(long user) throws SQLException {
		List<WallPost> posts = new ArrayList<>();
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement();
			ResultSet resSet = stat.executeQuery("SELECT * FROM " + POSTS_TABLE_NAME + " WHERE " + POST_SIGNER_ID_NAME + " = " + user)) {
				while (resSet.next()) {
					long post_id = resSet.getLong(POST_ID_NAME);
					long group_id = resSet.getLong(POST_GROUP_NAME);
					long date = resSet.getLong(POST_DATE_NAME);
					String text = resSet.getString(POST_TEXT_NAME);
					posts.add(new WallPost(post_id, group_id, user, new Date(date), text));
				}
			}
		return posts;
	}
	
	public static List<WallPostComment> getCommentsByPattern(String regex, Date dateRestriction) throws SQLException {
		List<WallPostComment> posts = new ArrayList<>();
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement();
			ResultSet resSet = stat.executeQuery("SELECT * FROM " + COMMENTS_TABLE_NAME + " WHERE " + COMMENT_DATE_NAME + " >= " + dateRestriction.getTime() + " AND " + COMMENT_TEXT_NAME + " REGEXP '" + regex + "'")) {
				while (resSet.next()) {
					long comment_id = resSet.getLong(COMMENT_ID_NAME);
					long from_id = resSet.getLong(COMMENT_FROM_ID_NAME);
					long date = resSet.getLong(COMMENT_DATE_NAME);
					String text = resSet.getString(COMMENT_TEXT_NAME);
					long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
					long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
					long reply = resSet.getLong(COMMENT_REPLY_NAME);
					posts.add(new WallPostComment(comment_id, from_id, new Date(date), text, group_id, post_id, reply));
				}
		}
		return posts;
	}

	public static List<WallPostComment> getCommentsBySigner(long user) throws SQLException {
		List<WallPostComment> posts = new ArrayList<>();
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement();
			ResultSet resSet = stat.executeQuery("SELECT * FROM " + COMMENTS_TABLE_NAME + " WHERE " + COMMENT_FROM_ID_NAME + " = " + user)) {
				while (resSet.next()) {
					long comment_id = resSet.getLong(COMMENT_ID_NAME);
					long date = resSet.getLong(COMMENT_DATE_NAME);
					String text = resSet.getString(COMMENT_TEXT_NAME);
					long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
					long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
					long reply = resSet.getLong(COMMENT_REPLY_NAME);
					posts.add(new WallPostComment(comment_id, user, new Date(date), text, group_id, post_id, reply));
				}
		}
		return posts;
	}

	public static List<WallPostComment> getCommentsByReply(long user) throws SQLException {
		List<WallPostComment> posts = new ArrayList<>();
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement();
			ResultSet resSet = stat.executeQuery("SELECT * FROM " + COMMENTS_TABLE_NAME + " WHERE " + COMMENT_REPLY_NAME + " = " + user)) {
				while (resSet.next()) {
					long comment_id = resSet.getLong(COMMENT_ID_NAME);
					long date = resSet.getLong(COMMENT_DATE_NAME);
					String text = resSet.getString(COMMENT_TEXT_NAME);
					long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
					long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
					long reply = resSet.getLong(COMMENT_REPLY_NAME);
					posts.add(new WallPostComment(comment_id, user, new Date(date), text, group_id, post_id, reply));
				}
		}
		return posts;
	}
	
	public static List<WallPostLike> getLikesByUser(long user) throws SQLException {
		List<WallPostLike> likes = new ArrayList<>();
		try(Statement stat = DatabaseWrapper.getInstance().getConnection().createStatement();
			ResultSet resSet = stat.executeQuery("SELECT * FROM " + LIKES_TABLE_NAME + " WHERE " + LIKES_USER_NAME + " = " + user)){
			while (resSet.next()) {
				String type = resSet.getString(LIKES_TYPE_NAME);
				long owner = resSet.getLong(LIKES_OWNER_NAME);
				long item = resSet.getLong(LIKES_ITEM_NAME);
				long date = resSet.getLong(LIKES_DATE_NAME);
				likes.add(new WallPostLike(user, type, owner, item, new Date(date)));
			}
		}
		return likes;
	}

	public static synchronized void insertPostsWithData(List<WallPost> posts, List<WallPostComment> comments, List<WallPostLike> likes) throws SQLException {
		Connection conn = DatabaseWrapper.getInstance().getConnection();
		conn.setAutoCommit(false);
		try {
			if(!posts.isEmpty())
				insertPosts(posts);
			if(!comments.isEmpty())
				insertComments(comments);
			if(!likes.isEmpty())
				insertLikes(likes);
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			conn.commit();
			conn.setAutoCommit(true);
		}
	}

	public static synchronized void insertBoardCommentsWithData(List<BoardComment> boardComments) throws SQLException {
		Connection conn = DatabaseWrapper.getInstance().getConnection();
		conn.setAutoCommit(false);
		try {
			if(!boardComments.isEmpty())
				insertBoardComments(boardComments);
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			conn.commit();
			conn.setAutoCommit(true);
		}
	}

	private static void insertPosts(List<WallPost> wpl) throws SQLException {
		try(PreparedStatement prep = DatabaseWrapper.getInstance().getConnection().prepareStatement("INSERT INTO " + POSTS_TABLE_NAME + " (" + POST_ID_NAME + ", "
				+ POST_GROUP_NAME + ", " + POST_SIGNER_ID_NAME + ", " + POST_DATE_NAME + ", " + POST_TEXT_NAME
				+ ") VALUES (?, ?, ?, ?, ?); ")) {

			for (WallPost wp : wpl) {
				prep.setLong(1, wp.getPostId());
				prep.setLong(2, wp.getGroupId());
				prep.setLong(3, wp.getSignerId());
				prep.setLong(4, wp.getDate().getTime());
				prep.setString(5, wp.getText());
				prep.addBatch();
			}
			prep.executeBatch();
		}
	}

	private static void insertBoardComments(List<BoardComment> bcl) throws SQLException {
		// TODO
	}

	private static void insertComments(List<WallPostComment> wcl) throws SQLException {
		try(PreparedStatement prep = DatabaseWrapper.getInstance().getConnection()
				.prepareStatement("INSERT INTO " + COMMENTS_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {

			for (WallPostComment wc : wcl) {
				prep.setLong(2, wc.getCommentId());
				prep.setLong(3, wc.getFromId());
				prep.setLong(4, wc.getDate().getTime());
				prep.setString(5, wc.getText());
				prep.setLong(6, wc.getGroupId());
				prep.setLong(7, wc.getPostId());
				prep.setLong(8, wc.getReplyToUser());
				prep.addBatch();
			}
			prep.executeBatch();
		}
	}

	private static void insertLikes(List<WallPostLike> lkl) throws SQLException {
		try(PreparedStatement prep = DatabaseWrapper.getInstance().getConnection()
				.prepareStatement("INSERT INTO " + LIKES_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?);")) {

			for (WallPostLike like : lkl) {
				prep.setLong(2, like.getUser());
				prep.setString(3, like.getType());
				prep.setLong(4, like.getOwnerId());
				prep.setLong(5, like.getItemId());
				prep.setLong(6, like.getDate().getTime());
				prep.addBatch();
			}
			prep.executeBatch();
		}
	}
}
