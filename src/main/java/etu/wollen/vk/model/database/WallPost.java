package etu.wollen.vk.model.database;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static etu.wollen.vk.conf.Config.PRINT_DATE_FORMAT;

public class WallPost extends BaseSearchableEntity {
	private long postId;
	private long groupId;
	private long signerId;
	private String text;
	
	public WallPost(long postId, long groupId, long signerId, Date date, String text){
		super(date);
		this.postId = postId;
		this.groupId = groupId;
		this.signerId = signerId;
		this.text=text;
	}
	
	public String getUrl(){
		return "http://vk.com/wall" + groupId + "_" + postId;
	}

	public long getPostId() {
		return postId;
	}

	public void setPostId(long postId) {
		this.postId = postId;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public long getSignerId() {
		return signerId;
	}

	public void setSignerId(long signerId) {
		this.signerId = signerId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void print(PrintStream printStream) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(PRINT_DATE_FORMAT);
		printStream.println(getUrl());
		printStream.println(dateFormat.format(date));
		printStream.println(text);
	}
}
