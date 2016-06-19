package etu.wollen.vk;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class Like {
	private long user;
	private String type; //post / comment
	private long owner_id;
	private long item_id;
	private Date date;
	
	public Like(long user, String type, long owner_id, long item_id, Date date) {
		this.user = user;
		this.type = type;
		this.owner_id = owner_id;
		this.item_id = item_id;
		this.date = date;
	}
	
	public void print(){
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy 'at' HH:mm:ss");
		//System.out.println(type);
		System.out.println(getPostUrl());
		System.out.println(dateFormat.format(date));
	}
	
	public String getPostUrl(){
		return "http://vk.com/wall" + owner_id + "_" + item_id;
	}
	
	public long getUser() {
		return user;
	}
	public void setUser(long user) {
		this.user = user;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public long getOwner_id() {
		return owner_id;
	}
	public void setOwner_id(long owner_id) {
		this.owner_id = owner_id;
	}
	public long getItem_id() {
		return item_id;
	}
	public void setItem_id(long item_id) {
		this.item_id = item_id;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
}

class Lcomparator implements Comparator<Like>{

	@Override
	public int compare(Like l1, Like l2) {
		if(l1.getDate().getTime()-l2.getDate().getTime() < 0){
			return -1;
		}
		else if(l1.getDate().getTime()-l2.getDate().getTime() == 0){
			return 0;
		}
		else{
			return 1;
		}
	}
	
}