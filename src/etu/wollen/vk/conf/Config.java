package etu.wollen.vk.conf;

import etu.wollen.vk.models.User;

import java.util.ArrayList;
import java.util.Date;

public class Config {
    public static final int groupThreads = 4;
    public static final int commentThreads = 8;
    public static final String version = "5.74";
    public static final String printDateFormat = "dd.MM.yyyy 'at' HH:mm:ss";

    private User findUser;
    private String findPattern;
    private boolean byId;
    private ArrayList<String> groupList;
    private Date dateRestriction;
    private String accessToken;

    public Config(User findUser, String findPattern, boolean byId, ArrayList<String> groupList, Date dateRestriction, String accessToken) {
        this.findUser = findUser;
        this.findPattern = findPattern;
        this.byId = byId;
        this.groupList = groupList;
        this.dateRestriction = dateRestriction;
        this.accessToken = accessToken;
    }

    public User getFindUser() {
        return findUser;
    }

    public void setFindUser(User findUser) {
        this.findUser = findUser;
    }

    public String getFindPattern() {
        return findPattern;
    }

    public void setFindPattern(String findPattern) {
        this.findPattern = findPattern;
    }

    public boolean isById() {
        return byId;
    }

    public void setById(boolean byId) {
        this.byId = byId;
    }

    public ArrayList<String> getGroupList() {
        return groupList;
    }

    public void setGroupList(ArrayList<String> groupList) {
        this.groupList = groupList;
    }

    public Date getDateRestriction() {
        return dateRestriction;
    }

    public void setDateRestriction(Date dateRestriction) {
        this.dateRestriction = dateRestriction;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
