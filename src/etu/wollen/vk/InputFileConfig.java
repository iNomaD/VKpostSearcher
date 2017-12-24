package etu.wollen.vk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class InputFileConfig {
	private User find_user = null;
	private String find_pattern = "";
	private boolean byId = true;
	private ArrayList<String> grList;
	private Date dateRestr = new Date();
	private String access_token = "";
	
	public boolean parseFileGroups(String filename) throws IOException {
		grList = new ArrayList<String>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			String id_str = br.readLine();
			find_user = getUserFromStr(id_str);
			if(find_user == null){
				byId = false;
				find_pattern = id_str;
			}

			String date_str = br.readLine();
			String[] pts = date_str.split("\\.");
			Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(pts[2]), Integer.parseInt(pts[1]) - 1, Integer.parseInt(pts[0]));
			dateRestr = cal.getTime();
			
			access_token = br.readLine();

			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.equals("")) {
					line = line.replaceAll(".*/", "");
					grList.add(line);
				}
			}
			return true;
		} catch (Exception e) {
			System.out.println("Error reading file");
			e.printStackTrace();
		} finally {
			br.close();
		}
		return false;
	}
	
	private User getUserFromStr(String id_str){
		String request = "https://api.vk.com/method/users.get?user_ids="+id_str+"&v=5.52";
		try {
			String response = HttpClient.sendGETtimeout(request, 11);
			JSONParser jp = new JSONParser();
			JSONObject jsonresponse = (JSONObject) jp.parse(response);
			JSONArray resp = (JSONArray) jsonresponse.get("response");
			if(resp != null){
				JSONObject user = (JSONObject)resp.get(0);
				long id = (long) user.get("id");
				String first = (String) user.get("first_name");
				String last = (String) user.get("last_name");
				return new User(id, first, last);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public User getFind_id() {
		return find_user;
	}

	public String getFind_pattern() {
		return find_pattern;
	}

	public Date getDateRestr() {
		return dateRestr;
	}
	
	public ArrayList<String> getGrList() {
		return grList;
	}
	
	public boolean isById() {
		return byId;
	}
	
	public String getAcessToken(){
		return access_token;
	}
}
