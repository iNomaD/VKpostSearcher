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
	
	public static String POSTS_TABLE_NAME = "POSTS";
	public static String POST_ID_NAME = "POST_ID";
	public static String POST_GROUP_NAME = "GROUP_ID";
	public static String POST_SIGNER_ID_NAME = "POST_SIGNER_ID";
	public static String POST_DATE_NAME = "POST_DATE";
	public static String POST_TEXT_NAME = "POST_TEXT";

	public static String COMMENTS_TABLE_NAME = "COMMENTS";
	public static String COMMENT_ID_NAME = "COMMENT_ID";
	public static String COMMENT_FROM_ID_NAME = "COMMENT_FROM_ID";
	public static String COMMENT_DATE_NAME = "COMMENT_DATE";
	public static String COMMENT_TEXT_NAME = "COMMENT_TEXT";
	public static String COMMENT_GROUP_ID_NAME = "COMMENT_GROUP_ID";
	public static String COMMENT_POST_ID_NAME = "COMMENT_POST_ID";

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

		System.out.println("Table deleted.");
	}

	public static void createDB() throws SQLException {
		Statement stat = conn.createStatement();

		stat.execute("CREATE TABLE if not exists " + POSTS_TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ POST_ID_NAME + " INTEGER , " + POST_GROUP_NAME + " INTEGER, " + POST_SIGNER_ID_NAME + " INTEGER, "
				+ POST_DATE_NAME + " INTEGER, " + POST_TEXT_NAME + " TEXT);");

		stat.execute("CREATE TABLE if not exists " + COMMENTS_TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COMMENT_ID_NAME + " INTEGER, " + COMMENT_FROM_ID_NAME + " INTEGER, " + COMMENT_DATE_NAME
				+ " INTEGER, " + COMMENT_TEXT_NAME + " TEXT, " + COMMENT_GROUP_ID_NAME + " INTEGER, "
				+ COMMENT_POST_ID_NAME + " INTEGER);");

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
	
		for(WallPost wp : wpl){
			prep.setLong(1, wp.getPost_id());
			prep.setLong(2, wp.getGroup_id());
			prep.setLong(3, wp.getSigner_id());
			prep.setLong(4, wp.getDate().getTime());
			prep.setString(5, wp.getText());
			prep.addBatch();
		}
		prep.executeBatch();
	}
	
	public static void insertComment(WallComment wc) throws SQLException{
		PreparedStatement prep = conn.prepareStatement("INSERT INTO "+COMMENTS_TABLE_NAME+ " VALUES (?, ?, ?, ?, ?, ?, ?);");
		
		prep.setLong(2, wc.getComment_id());
		prep.setLong(3, wc.getFrom_id());
		prep.setLong(4, wc.getDate().getTime());
		prep.setString(5, wc.getText());
		prep.setLong(6, wc.getGroup_id());
		prep.setLong(7, wc.getPost_id());
		prep.addBatch();
		prep.executeBatch();
	}
	
	public static void insertComments(List<WallComment> wcl) throws SQLException{
		PreparedStatement prep = conn.prepareStatement("INSERT INTO "+COMMENTS_TABLE_NAME+ " VALUES (?, ?, ?, ?, ?, ?, ?);");
		
		for(WallComment wc : wcl){
			prep.setLong(2, wc.getComment_id());
			prep.setLong(3, wc.getFrom_id());
			prep.setLong(4, wc.getDate().getTime());
			prep.setString(5, wc.getText());
			prep.setLong(6, wc.getGroup_id());
			prep.setLong(7, wc.getPost_id());
			prep.addBatch();
		}
		prep.executeBatch();
	}
	
	public static void insertPostsWithComments(List<WallPost> wpl, List<WallComment> wcl) throws SQLException{
		synchronized(conn){
			conn.setAutoCommit(false);
			try {
			    insertPosts(wpl);
			    insertComments(wcl);
			} catch(SQLException e) {
			    conn.rollback();
			    throw e;
			} finally {
			    conn.commit();
			    conn.setAutoCommit(true);
			}
		}
	}

	public static ArrayList<WallPost> getAllPosts() throws SQLException {
		ArrayList<WallPost> posts = new ArrayList<WallPost>();
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

	public static ArrayList<WallPost> getPostsFromWall(long wall_id) throws SQLException {
		ArrayList<WallPost> posts = new ArrayList<WallPost>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery("SELECT * FROM " + POSTS_TABLE_NAME + " WHERE "+POST_GROUP_NAME+" = " + wall_id);
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
	
	public static ArrayList<WallPost> getPostsBySinger(long signer_id) throws SQLException {
		ArrayList<WallPost> posts = new ArrayList<WallPost>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery("SELECT * FROM " + POSTS_TABLE_NAME + " WHERE "+POST_SIGNER_ID_NAME+" = " + signer_id);
		while (resSet.next()) {
			long post_id = resSet.getLong(POST_ID_NAME);
			long group_id = resSet.getLong(POST_GROUP_NAME);
			long date = resSet.getLong(POST_DATE_NAME);
			String text = resSet.getString(POST_TEXT_NAME);
			posts.add(new WallPost(post_id, group_id, signer_id, new Date(date), text));
		}
		return posts;
	}
	
	public static ArrayList<WallComment> getAllComments() throws SQLException {
		ArrayList<WallComment> posts = new ArrayList<WallComment>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery("SELECT * FROM " + COMMENTS_TABLE_NAME);
		while (resSet.next()) {
			
			long comment_id = resSet.getLong(COMMENT_ID_NAME);
			long from_id = resSet.getLong(COMMENT_FROM_ID_NAME);
			long date = resSet.getLong(COMMENT_DATE_NAME);
			String text = resSet.getString(COMMENT_TEXT_NAME);
			long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
			long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
			posts.add(new WallComment(comment_id, from_id, new Date(date), text, group_id, post_id));
		}
		return posts;
	}
	
	public static ArrayList<WallComment> getCommentsBySigner(long signer_id) throws SQLException {
		ArrayList<WallComment> posts = new ArrayList<WallComment>();
		Statement stat = conn.createStatement();
		ResultSet resSet = stat.executeQuery("SELECT * FROM " + COMMENTS_TABLE_NAME + " WHERE "+COMMENT_FROM_ID_NAME+" = " + signer_id);
		while (resSet.next()) {
			
			long comment_id = resSet.getLong(COMMENT_ID_NAME);
			long date = resSet.getLong(COMMENT_DATE_NAME);
			String text = resSet.getString(COMMENT_TEXT_NAME);
			long group_id = resSet.getLong(COMMENT_GROUP_ID_NAME);
			long post_id = resSet.getLong(COMMENT_POST_ID_NAME);
			posts.add(new WallComment(comment_id, signer_id, new Date(date), text, group_id, post_id));
		}
		return posts;
	}
}
