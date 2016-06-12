package etu.wollen.vk;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class InputFileParser {
	private long find_id = 0;
	private String find_pattern = "";
	private boolean byId = true;
	private ArrayList<String> grList;
	private Date dateRestr = new Date();
	
	public void parseFileGroups(String filename) throws IOException {
		grList = new ArrayList<String>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			String id_str = br.readLine();
			find_id = Long.parseLong(id_str);

			String date_str = br.readLine();
			String[] pts = date_str.split("\\.");
			Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(pts[2]), Integer.parseInt(pts[1]) - 1, Integer.parseInt(pts[0]));
			dateRestr = cal.getTime();

			String line = null;
			while ((line = br.readLine()) != null) {
				if (!line.equals("")) {
					line = line.replaceAll(".*/", "");
					grList.add(line);
				}
			}
		} catch (IOException e) {
			System.out.println("Error reading file");
			e.printStackTrace();
		} finally {
			br.close();
		}
	}
	
	public long getFind_id() {
		return find_id;
	}

	public void setFind_id(long find_id) {
		this.find_id = find_id;
	}

	public String getFind_pattern() {
		return find_pattern;
	}

	public void setFind_pattern(String find_pattern) {
		this.find_pattern = find_pattern;
	}

	public Date getDateRestr() {
		return dateRestr;
	}

	public void setDateRestr(Date dateRestr) {
		this.dateRestr = dateRestr;
	}
	
	public ArrayList<String> getGrList() {
		return grList;
	}

	public void setGrList(ArrayList<String> grList) {
		this.grList = grList;
	}
	
	public boolean isById() {
		return byId;
	}

	public void setById(boolean byId) {
		this.byId = byId;
	}
}
