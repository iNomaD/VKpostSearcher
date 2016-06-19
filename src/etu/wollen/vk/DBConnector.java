package etu.wollen.vk;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBConnector {
	private static Connection conn = null;

	public static final String POSTS_TABLE_NAME = "POSTS";
	public static final String POST_ID_NAME = "POST_ID";
	public static final String POST_GROUP_NAME = "GROUP_ID";
	public static final String POST_SIGNER_ID_NAME = "POST_SIGNER_ID";
	public static final String POST_DATE_NAME = "POST_DATE";
	public static final String POST_TEXT_NAME = "POST_TEXT";

	public static final String COMMENTS_TABLE_NAME = "COMMENTS";
	public static final String COMMENT_ID_NAME = "COMMENT_ID";
	public static final String COMMENT_FROM_ID_NAME = "COMMENT_FROM_ID";
	public static final String COMMENT_DATE_NAME = "COMMENT_DATE";
	public static final String COMMENT_TEXT_NAME = "COMMENT_TEXT";
	public static final String COMMENT_GROUP_ID_NAME = "COMMENT_GROUP_ID";
	public static final String COMMENT_POST_ID_NAME = "COMMENT_POST_ID";
	public static final String COMMENT_REPLY_NAME = "COMMENT_REPLY";
	
	public static final String LIKES_TABLE_NAME = "LIKES";
	public static final String LIKES_USER_NAME = "LIKES_USER";
	public static final String LIKES_TYPE_NAME = "LIKES_TYPE";
	public static final String LIKES_OWNER_NAME = "LIKES_OWNER";
	public static final String LIKES_ITEM_NAME = "LIKES_ITEM";
	public static final String LIKES_DATE_NAME = "LIKES_DATE";

	public static void connect() throws ClassNotFoundException, SQLException {
		Class.forName("org.sqlite.JDBC");
		conn = getConnection();
		System.out.println("DB connected!");
	}

	private static Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite:vkgroups.s3db");
	}

	public static void deleteDB() throws SQLException {
		Statement stat = conn.createStatement();
		stat.executeUpdate("drop table if exists " + POSTS_TABLE_NAME + ";");
		stat.executeUpdate("drop table if exists " + COMMENTS_TABLE_NAME + ";");
		stat.executeUpdate("drop table if exists " + LIKES_TABLE_NAME + ";");

		System.out.println("DB Tables deleted.");
	}

	public static void createDB() throws SQLException {
		Statement stat = conn.createStatement();

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

	}

	public static void insertPost(WallPost wp) throws SQLException {
		PreparedStatement prep = conn.prepareStatement("INSERT INTO " + POSTS_TABLE_NAME + " (" + POST_ID_NAME + ", "
				+ POST_GROUP_NAME + ", " + POST_SIGNER_ID_NAME + ", " + POST_DATE_NAME + ", " + POST_TEXT_NAME
				+ ") VALUES (?, ?, ?, ?, ?); ");

		prep.setLong(1, wp.getPost_id());
		prep.setLong(2, wp.getGroup_id());
		prep.setLong(3, wp.getSigner_id());
		prep.setLong(4, wp.getDate().getTime());
		prep.setString(5, wp.getText());
		prep.addBatch();
		prep.executeBatch();
	}

	public static void insertPosts(List<WallPost> wpl) throws SQLException {
		PreparedStatement prep = conn.prepareStatement("INSERT INTO " + POSTS_TABLE_NAME + " (" + POST_ID_NAME + ", "
				+ POST_GROUP_NAME + ", " + POST_SIGNER_ID_NAME + ", " + POST_DATE_NAME + ", " + POST_TEXT_NAME
				+ ") VALUES (?, ?, ?, ?, ?); ");

		for (WallPost wp : wpl) {
			prep.setLong(1, wp.getPost_id());
			prep.setLong(2, wp.getGroup_id());
			prep.setLong(3, wp.getSigner_id());
			prep.setLong(4, wp.getDate().getTime());
			prep.setString(5, wp.getText());
			prep.addBatch();
		}
		prep.executeBatch();
	}

	public static void insertComment(WallComment wc) throws SQLException {
		PreparedStatement prep = conn
				.prepareStatement("INSERT INTO " + COMMENTS_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

		prep.setLong(2, wc.getComment_id());
		prep.setLong(3, wc.getFrom_id());
		prep.setLong(4, wc.getDate().getTime());
		prep.setString(5, wc.getText());
		prep.setLong(6, wc.getGroup_id());
		prep.setLong(7, wc.getPost_id());
		prep.setLong(8, wc.getReply_to_user());
		prep.addBatch();
		prep.executeBatch();
	}

	public static void insertComments(List<WallComment> wcl) throws SQLException {
		PreparedStatement prep = conn
				.prepareStatement("INSERT INTO " + COMMENTS_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?, ?, ?);");

		for (WallComment wc : wcl) {
			prep.setLong(2, wc.getComment_id());
			prep.setLong(3, wc.getFrom_id());
			prep.setLong(4, wc.getDate().getTime());
			prep.setString(5, wc.getText());
			prep.setLong(6, wc.getGroup_id());
			prep.setLong(7, wc.getPost_id());
			prep.setLong(8, wc.getReply_to_user());
			prep.addBatch();
		}
		prep.executeBatch();
	}
	
	public static void insertLike(Like like) throws SQLException {
		PreparedStatement prep = conn.prepareStatement("INSERT INTO " + LIKES_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?);");

		prep.setLong(2, like.getUser());
		prep.setString(3, like.getType());
		prep.setLong(4, like.getOwner_id());
		prep.setLong(5, like.getItem_id());
		prep.setLong(6, like.getDate().getTime());
		prep.addBatch();
		prep.executeBatch();
	}

	public static void insertLikes(List<Like> lkl) throws SQLException {
		PreparedStatement prep = conn.prepareStatement("INSERT INTO " + LIKES_TABLE_NAME + " VALUES (?, ?, ?, ?, ?, ?);");

		for (Like like : lkl) {
			prep.setLong(2, like.getUser());
			prep.setString(3, like.getType());
			prep.setLong(4, like.getOwner_id());
			prep.setLong(5, like.getItem_id());
			prep.setLong(6, like.getDate().getTime());
			prep.addBatch();
		}
		prep.executeBatch();
	}

	public static void insertPostsWithComments(List<WallPost> wpl, List<WallComment> wcl, List<Like> likes) throws SQLException {
		synchronized (conn) {
			conn.setAutoCommit(false);
			try {
				insertPosts(wpl);
				insertComments(wcl);
				insertLikes(likes);
			} catch (SQLException e) {
				conn.rollback();
				throw e;
			} finally {
				conn.commit();
				conn.setAutoCommit(true);
			}
		}
	}

	public static List<WallPost> getAllPosts() throws SQLException {
		List<WallPost> posts = new ArrayList<WallPost>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery("SELECT * FROM " + POSTS_TABLE_NAME);
		while (resSet.next()) {
			long post_id = resSet.getLong(POST_ID_NAME);
			long group_id = resSet.getLong(POST_GROUP_NAME);
			long signer_id = resSet.getLong(POST_SIGNER_ID_NAME);
			long date = resSet.getLong(POST_DATE_NAME);
			String text = resSet.getString(POST_TEXT_NAME);
			posts.add(new WallPost(post_id, group_id, signer_id, new Date(date), text));
		}
		return posts;
	}

	public static List<WallPost> getPostsFromWall(long wall_id) throws SQLException {
		List<WallPost> posts = new ArrayList<WallPost>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat
				.executeQuery("SELECT * FROM " + POSTS_TABLE_NAME + " WHERE " + POST_GROUP_NAME + " = " + wall_id);
		while (resSet.next()) {
			long post_id = resSet.getLong(POST_ID_NAME);
			long group_id = resSet.getLong(POST_GROUP_NAME);
			long signer_id = resSet.getLong(POST_SIGNER_ID_NAME);
			long date = resSet.getLong(POST_DATE_NAME);
			String text = resSet.getString(POST_TEXT_NAME);
			posts.add(new WallPost(post_id, group_id, signer_id, new Date(date), text));
		}
		return posts;
	}

	public static List<WallPost> getPostsBySinger(long signer_id) throws SQLException {
		List<WallPost> posts = new ArrayList<WallPost>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery(
				"SELECT * FROM " + POSTS_TABLE_NAME + " WHERE " + POST_SIGNER_ID_NAME + " = " + signer_id);
		while (resSet.next()) {
			long post_id = resSet.getLong(POST_ID_NAME);
			long group_id = resSet.getLong(POST_GROUP_NAME);
			long date = resSet.getLong(POST_DATE_NAME);
			String text = resSet.getString(POST_TEXT_NAME);
			posts.add(new WallPost(post_id, group_id, signer_id, new Date(date), text));
		}
		return posts;
	}

	public static List<WallComment> getAllComments() throws SQLException {
		List<WallComment> posts = new ArrayList<WallComment>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery("SELECT * FROM " + COMMENTS_TABLE_NAME);
		while (resSet.next()) {

			long comment_id = resSet.getLong(COMMENT_ID_NAME);
			long from_id = resSet.getLong(COMMENT_FROM_ID_NAME);
			long date = resSet.getLong(COMMENT_DATE_NAME);
			String text = resSet.getString(COMMENT_TEXT_NAME);
			long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
			long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
			long reply = resSet.getLong(COMMENT_REPLY_NAME);
			posts.add(new WallComment(comment_id, from_id, new Date(date), text, group_id, post_id, reply));
		}
		return posts;
	}

	public static List<WallComment> getCommentsBySigner(long signer_id) throws SQLException {
		List<WallComment> posts = new ArrayList<WallComment>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery(
				"SELECT * FROM " + COMMENTS_TABLE_NAME + " WHERE " + COMMENT_FROM_ID_NAME + " = " + signer_id);
		while (resSet.next()) {

			long comment_id = resSet.getLong(COMMENT_ID_NAME);
			long date = resSet.getLong(COMMENT_DATE_NAME);
			String text = resSet.getString(COMMENT_TEXT_NAME);
			long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
			long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
			long reply = resSet.getLong(COMMENT_REPLY_NAME);
			posts.add(new WallComment(comment_id, signer_id, new Date(date), text, group_id, post_id, reply));
		}
		return posts;
	}

	public static List<WallComment> getCommentsByReply(long signer_id) throws SQLException {
		List<WallComment> posts = new ArrayList<WallComment>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery(
				"SELECT * FROM " + COMMENTS_TABLE_NAME + " WHERE " + COMMENT_REPLY_NAME + " = " + signer_id);
		while (resSet.next()) {

			long comment_id = resSet.getLong(COMMENT_ID_NAME);
			long date = resSet.getLong(COMMENT_DATE_NAME);
			String text = resSet.getString(COMMENT_TEXT_NAME);
			long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
			long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
			long reply = resSet.getLong(COMMENT_REPLY_NAME);
			posts.add(new WallComment(comment_id, signer_id, new Date(date), text, group_id, post_id, reply));
		}
		return posts;
	}
	
	public static List<Like> getLikesByUser(long user) throws SQLException {
		List<Like> likes = new ArrayList<>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery(
				"SELECT * FROM " + LIKES_TABLE_NAME + " WHERE " + LIKES_USER_NAME + " = " + user);
		while (resSet.next()) {
			String type = resSet.getString(LIKES_TYPE_NAME);
			long owner = resSet.getLong(LIKES_OWNER_NAME);
			long item = resSet.getLong(LIKES_ITEM_NAME);
			long date = resSet.getLong(LIKES_DATE_NAME);
			likes.add(new Like(user, type, owner,item, new Date(date)));
		}
		return likes;
	}
}
