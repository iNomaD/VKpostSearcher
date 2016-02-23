package etu.wollen.vk;

import java.util.Date;

public class WallComment {
	private long comment_id;
	private long from_id;
	private Date date;
	private String text;
	private long group_id;
	private long post_id;

	public WallComment(long comment_id, long from_id, Date date, String text, long group_id, long post_id) {
		this.comment_id = comment_id;
		this.from_id = from_id;
		this.date = date;
		this.text = text;
		this.group_id = group_id;
		this.post_id = post_id;
	}
	
	public void print(){
		System.out.println(comment_id+" "+from_id+" "+date+" from "+"http://vk.com/wall"+group_id+"_"+post_id);
		System.out.println(text);
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

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
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
}
