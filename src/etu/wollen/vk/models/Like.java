package etu.wollen.vk.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static etu.wollen.vk.conf.Config.PRINT_DATE_FORMAT;

public class Like extends BaseSearchableEntity{
	private long user;
	private String type; //post / comment
	private long ownerId;
	private long itemId;
	
	public Like(long user, String type, long ownerId, long itemId, Date date) {
		super(date);
		this.user = user;
		this.type = type;
		this.ownerId = ownerId;
		this.itemId = itemId;
	}
	
	public void print(){
		SimpleDateFormat dateFormat = new SimpleDateFormat(PRINT_DATE_FORMAT);
		//System.out.println(type);
		System.out.println(getPostUrl());
		System.out.println(dateFormat.format(date));
	}
	
	public String getPostUrl(){
		return "http://vk.com/wall" + ownerId + "_" + itemId;
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
	public long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(long ownerId) {
		this.ownerId = ownerId;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
	}
	
	public static List<Like> getByGroupId(List<Like> items, long groupId){
		List<Like> result = new LinkedList<>();
		for(Like item : items){
			if(item.getOwnerId() == groupId){
				result.add(item);
			}
		}
		return result;
	}
}