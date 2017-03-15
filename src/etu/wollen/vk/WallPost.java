package etu.wollen.vk;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class WallPost{
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
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy 'at' HH:mm:ss");
		System.out.println(getPostUrl());
		System.out.println(dateFormat.format(date));
		System.out.println(text);
	}
	
	public String getPostUrl(){
		return "http://vk.com/wall" + group_id + "_" + post_id;
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
	
	public static List<WallPost> getByGroupId(List<WallPost> items, long groupId){
		List<WallPost> result = new LinkedList<>();
		for(WallPost item : items){
			if(item.getGroup_id() == groupId){
				result.add(item);
			}
		}
		return result;
	}
}

class WPcomparator implements Comparator<WallPost>{

	@Override
	public int compare(WallPost w1, WallPost w2) {
		if(w1.getDate().getTime()-w2.getDate().getTime() < 0){
			return -1;
		}
		else if(w1.getDate().getTime()-w2.getDate().getTime() == 0){
			return 0;
		}
		else{
			return 1;
		}
	}
	
}
