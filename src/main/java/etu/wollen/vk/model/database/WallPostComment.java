package etu.wollen.vk.model.database;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static etu.wollen.vk.conf.Config.PRINT_DATE_FORMAT;

public class WallPostComment extends BaseSearchableEntity {
	private long commentId;
	private long fromId;
	private String text;
	private long groupId;
	private long postId;
	private long replyToUser;

	public WallPostComment(long commentId, long fromId, Date date, String text, long groupId, long postId, long replyToUser) {
		super(date);
		this.commentId = commentId;
		this.fromId = fromId;
		this.text = text;
		this.groupId = groupId;
		this.postId = postId;
		this.replyToUser = replyToUser;
	}
	
	public String getPostUrl(){
		return "http://vk.com/wall"+ groupId +"_"+ postId;
	}

	public long getFromId() {
		return fromId;
	}

	public void setFromId(long fromId) {
		this.fromId = fromId;
	}

	public long getCommentId() {
		return commentId;
	}

	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public long getPostId() {
		return postId;
	}

	public void setPostId(long postId) {
		this.postId = postId;
	}
	
	public long getReplyToUser() {
		return replyToUser;
	}

	public void setReplyToUser(long replyToUser) {
		this.replyToUser = replyToUser;
	}

	@Override
	public void print(PrintStream printStream) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(PRINT_DATE_FORMAT);
		printStream.println(getPostUrl());
		printStream.println(dateFormat.format(date));
		printStream.println(text);
	}
}