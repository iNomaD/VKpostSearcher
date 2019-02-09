package etu.wollen.vk.model.database;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static etu.wollen.vk.conf.Config.PRINT_DATE_FORMAT;

public class WallPostLike extends BaseSearchableEntity {
	private long user;
	private String type; // post / comment
	private long ownerId;
	private long itemId;
	
	public WallPostLike(long user, String type, long ownerId, long itemId, Date date) {
		super(date);
		this.user = user;
		this.type = type;
		this.ownerId = ownerId;
		this.itemId = itemId;
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

	@Override
	public void print(PrintStream printStream) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(PRINT_DATE_FORMAT);
		printStream.println(getPostUrl());
		printStream.println(dateFormat.format(date));
	}
}