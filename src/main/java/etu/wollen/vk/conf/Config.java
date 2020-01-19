package etu.wollen.vk.conf;

import etu.wollen.vk.model.conf.Board;
import etu.wollen.vk.model.conf.User;

import java.util.Date;
import java.util.List;

public final class Config {
    public static final String VK_API_URL = "https://api.vk.com/method/";
    public static final String VERSION = "5.74";
    public static final String PRINT_DATE_FORMAT = "dd.MM.yyyy 'at' HH:mm:ss";
    public static final int HTTP_DELAY = 334;
    public static final int HTTP_MAX_ATTEMPTS = 11;
    public static final int HTTP_CONNECTION_TIMEOUT_SECONDS = 15;

    public static int PRIMARY_THREADS = 10;
    public static int SECONDARY_THREADS = 50;

    private User findUser;
    private String findPattern;
    private boolean byId;
    private List<String> groupList;
    private List<Board> boardList;
    private Date dateRestriction;
    private String accessToken;

    public Config(User findUser, String findPattern, boolean byId, List<String> groupList, List<Board> boardList,
                  Date dateRestriction, String accessToken) {
        this.findUser = findUser;
        this.findPattern = findPattern;
        this.byId = byId;
        this.groupList = groupList;
        this.boardList = boardList;
        this.dateRestriction = dateRestriction;
        this.accessToken = accessToken;
    }

    public User getFindUser() {
        return findUser;
    }

    public String getFindPattern() {
        return findPattern;
    }

    public boolean isById() {
        return byId;
    }

    public List<String> getGroupList() {
        return groupList;
    }

    public List<Board> getBoardList() {
        return boardList;
    }

    public Date getDateRestriction() {
        return dateRestriction;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
