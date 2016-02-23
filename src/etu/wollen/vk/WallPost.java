package etu.wollen.vk;

import java.util.Date;

public class WallPost {
	private long post_id;
	private long group_id;
	private long signer_id;
	private Date date;
	private String text;
	
	public WallPost(long post_id, long group_id, long signer_id, Date date, String text){
		this.post_id=post_id;
		this.group_id=group_id;
		this.signer_id=signer_id;
		this.date=date;
		this.text=text;
	}
	
	public void print(){
		System.out.println(getPostUrl()+" Date = "+date);
		System.out.println(text);
	}
	
	public String getPostUrl(){
		return "http://vk.com/wall"+group_id+"_"+post_id;
	}

	public long getPost_id() {
		return post_id;
	}

	public void setPost_id(long post_id) {
		this.post_id = post_id;
	}

	public long getGroup_id() {
		return group_id;
	}

	public void setGroup_id(long group_id) {
		this.group_id = group_id;
	}

	public long getSigner_id() {
		return signer_id;
	}

	public void setSigner_id(long signer_id) {
		this.signer_id = signer_id;
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
}
