package etu.wollen.vk.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static etu.wollen.vk.conf.Config.PRINT_DATE_FORMAT;

public class WallComment extends BaseSearchableEntity{
	private long comment_id;
	private long from_id;
	private String text;
	private long group_id;
	private long post_id;
	private long reply_to_user;

	public WallComment(long comment_id, long from_id, Date date, String text, long group_id, long post_id, long reply_to_user) {
		super(date);
		this.comment_id = comment_id;
		this.from_id = from_id;
		this.text = text;
		this.group_id = group_id;
		this.post_id = post_id;
		this.reply_to_user=reply_to_user;
	}

	public void print(){
		SimpleDateFormat dateFormat = new SimpleDateFormat(PRINT_DATE_FORMAT);
		System.out.println(getPostUrl());
		System.out.println(dateFormat.format(date));
		System.out.println(text);
	}
	
	public String getPostUrl(){
		return "http://vk.com/wall"+group_id+"_"+post_id;
	}

	public long getFrom_id() {
		return from_id;
	}

	public void setFrom_id(long from_id) {
		this.from_id = from_id;
	}

	public long getComment_id() {
		return comment_id;
	}

	public void setComment_id(long comment_id) {
		this.comment_id = comment_id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public long getGroup_id() {
		return group_id;
	}

	public void setGroup_id(long group_id) {
		this.group_id = group_id;
	}

	public long getPost_id() {
		return post_id;
	}

	public void setPost_id(long post_id) {
		this.post_id = post_id;
	}
	
	public long getReply_to_user() {
		return reply_to_user;
	}

	public void setReply_to_user(long reply_to_user) {
		this.reply_to_user = reply_to_user;
	}
	
	public static List<WallComment> getByGroupId(List<WallComment> items, long groupId){
		List<WallComment> result = new LinkedList<>();
		for(WallComment item : items){
			if(item.getGroup_id() == groupId){
				result.add(item);
			}
		}
		return result;
	}
}