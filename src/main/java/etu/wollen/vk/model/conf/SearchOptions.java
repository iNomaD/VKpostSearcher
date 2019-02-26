package etu.wollen.vk.model.conf;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchOptions {

    public enum SearchType {
        BY_ID_SINGLE, BY_ID_MULTIPLE, BY_PATTERN
    }

    private final SearchType searchType;
    private final Map<Long, User> findUsers;
    private final String findPattern;

    private final Date dateRestriction;
    private final Map<Long, String> groupNames;
    private final Map<Board, String> topicTitles;

    private SearchOptions(SearchType searchType, Map<Long, User> findUsers, String findPattern, Date dateRestriction, Map<Long, String> groupNames, Map<Board, String> topicTitles) {
        this.searchType = searchType;
        this.findUsers = findUsers;
        this.findPattern = findPattern;
        this.dateRestriction = dateRestriction;
        this.groupNames = groupNames;
        this.topicTitles = topicTitles;
    }

    public static SearchOptions of(User user, Date dateRestriction, Map<Long, String> groupNames, Map<Board, String> topicTitles) {
        return new SearchOptions(SearchType.BY_ID_SINGLE, Collections.singletonMap(user.getId(), user), null, dateRestriction, groupNames, topicTitles);
    }

    public static SearchOptions of(List<User> friends, Date dateRestriction, Map<Long, String> groupNames, Map<Board, String> topicTitles) {
        return new SearchOptions(SearchType.BY_ID_MULTIPLE, friends.stream().collect(Collectors.toMap(User::getId, user -> user)), null, dateRestriction, groupNames, topicTitles);
    }

    public static SearchOptions of(String findPattern, Date dateRestriction, Map<Long, String> groupNames, Map<Board, String> topicTitles) {
        return new SearchOptions(SearchType.BY_PATTERN, null, findPattern, dateRestriction, groupNames, topicTitles);
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public Map<Long, User> getFindUsers() {
        return findUsers;
    }

    public String getFindPattern() {
        return findPattern;
    }

    public Date getDateRestriction() {
        return dateRestriction;
    }

    public Map<Long, String> getGroupNames() {
        return groupNames;
    }

    public Map<Board, String> getTopicTitles() {
        return topicTitles;
    }
}
