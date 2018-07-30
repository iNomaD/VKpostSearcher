package etu.wollen.vk.conf;

import etu.wollen.vk.models.User;

import java.util.ArrayList;
import java.util.Date;

public class Config {
    public static final String VERSION = "5.74";
    public static final String PRINT_DATE_FORMAT = "dd.MM.yyyy 'at' HH:mm:ss";

    public static int PRIMARY_THREADS = 10;
    public static int SECONDARY_THREADS = 50;

    private User findUser;
    private String findPattern;
    private boolean byId;
    private ArrayList<String> groupList;
    private Date dateRestriction;
    private String accessToken;

    public Config(User findUser, String findPattern, boolean byId, ArrayList<String> groupList, Date dateRestriction,
                  String accessToken, int primaryThreads, int secondaryThreads) {
        this.findUser = findUser;
        this.findPattern = findPattern;
        this.byId = byId;
        this.groupList = groupList;
        this.dateRestriction = dateRestriction;
        this.accessToken = accessToken;

        if(primaryThreads > 0) PRIMARY_THREADS = primaryThreads;
        if(secondaryThreads > 0) SECONDARY_THREADS = secondaryThreads;
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
