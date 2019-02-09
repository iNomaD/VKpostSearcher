package etu.wollen.vk.utils;

import etu.wollen.vk.model.conf.SearchOptions;
import etu.wollen.vk.model.conf.User;
import etu.wollen.vk.model.database.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PrintUtils {

    private static final String LINE_SEPARATOR = System.lineSeparator();

    public static void printToFile(SearchOptions so, boolean cluster, List<WallPost> posts, List<WallPostComment> comments,
                                   List<BoardComment> boardComments, List<WallPostComment> answers, List<WallPostLike> likes,
                                   String outputFileName) throws FileNotFoundException {

        Map<Long, User> usersMap = so.getFindUsers();
        SearchOptions.SearchType searchType = so.getSearchType();
        PrintStream standard = System.out;
        try (PrintStream st = new PrintStream(new FileOutputStream(outputFileName))) {
            System.setOut(st);

            switch (searchType){
                case BY_ID_SINGLE:
                    System.out.println("User: " + usersMap.values().iterator().next() + LINE_SEPARATOR);
                    break;
                case BY_ID_MULTIPLE:
                    StringBuilder users = new StringBuilder();
                    for(User friend : usersMap.values()){
                        users.append(friend).append(", ");
                    }
                    System.out.println("Users: " + users + LINE_SEPARATOR);
                    break;
                case BY_PATTERN:
                    System.out.println("Pattern: [" + so.getFindPattern() + "]" + LINE_SEPARATOR);
                    break;
            }

            if (cluster) {
                System.out.println(posts.size() + " posts found!" + LINE_SEPARATOR);
                System.out.println(comments.size() + " comments found!" + LINE_SEPARATOR);
                System.out.println(boardComments.size() + " board comments found!" + LINE_SEPARATOR);
                if (so.getSearchType() != SearchOptions.SearchType.BY_PATTERN) {
                    System.out.println(answers.size() + " answers found!" + LINE_SEPARATOR);
                    System.out.println(likes.size() + " post likes found!" + LINE_SEPARATOR);
                }
                for (Map.Entry<Long, String> group : so.getGroupNames().entrySet()) {
                    long groupId = -group.getKey(); // group value without minus here
                    String name = group.getValue();
                    List<WallPost> selectedPosts = posts.stream().filter(post -> post.getGroupId() == groupId).collect(Collectors.toList());
                    List<WallPostComment> selectedComments = comments.stream().filter(comment -> comment.getGroupId() == groupId).collect(Collectors.toList());
                    List<BoardComment> selectedBoardComments = boardComments.stream().filter(comment -> comment.getGroupId() == groupId).collect(Collectors.toList());
                    List<WallPostComment> selectedAnswers = answers.stream().filter(comment -> comment.getGroupId() == groupId).collect(Collectors.toList());
                    List<WallPostLike> selectedLikes = likes.stream().filter(like -> like.getOwnerId() == groupId).collect(Collectors.toList());

                    if (!selectedPosts.isEmpty() || !selectedComments.isEmpty() || !selectedAnswers.isEmpty() || !selectedLikes.isEmpty()) {
                        System.out.println("<<< " + name + " >>>");
                        printItems(selectedPosts, searchType, a -> usersMap.get(((WallPost)a).getSignerId()));
                        printItems(selectedComments, searchType, a -> usersMap.get(((WallPostComment)a).getFromId()));
                        printItems(selectedBoardComments, searchType, a -> usersMap.get(((BoardComment)a).getFromId()));
                        if (so.getSearchType() != SearchOptions.SearchType.BY_PATTERN) {
                            printItems(selectedAnswers, searchType, a -> usersMap.get(((WallPostComment)a).getReplyToUser()));
                            printItems(selectedLikes, searchType, a -> usersMap.get(((WallPostLike)a).getUser()));
                        }
                        System.out.println(LINE_SEPARATOR + LINE_SEPARATOR);
                    }
                }
            } else {
                System.out.println(posts.size() + " posts found!" + LINE_SEPARATOR);
                printItems(posts, searchType, a -> usersMap.get(((WallPost)a).getSignerId()));

                System.out.println(LINE_SEPARATOR + LINE_SEPARATOR + comments.size() + " comments found!"
                        + LINE_SEPARATOR);
                printItems(comments, searchType, a -> usersMap.get(((WallPostComment)a).getFromId()));

                System.out.println(LINE_SEPARATOR + LINE_SEPARATOR + boardComments.size() + " board comments found!"
                        + LINE_SEPARATOR);
                printItems(boardComments, searchType, a -> usersMap.get(((BoardComment)a).getFromId()));

                if (so.getSearchType() != SearchOptions.SearchType.BY_PATTERN) {
                    System.out.println(LINE_SEPARATOR + LINE_SEPARATOR + answers.size() + " answers found!"
                            + LINE_SEPARATOR);
                    printItems(answers, searchType, a -> usersMap.get(((WallPostComment)a).getReplyToUser()));

                    System.out.println(LINE_SEPARATOR + LINE_SEPARATOR + likes.size() + " post likes found!"
                            + LINE_SEPARATOR);
                    printItems(likes, searchType, a -> usersMap.get(((WallPostLike)a).getUser()));
                }
            }
        } finally {
            System.setOut(standard);
        }
    }

    private static void printItems(List<? extends BaseSearchableEntity> items, SearchOptions.SearchType searchType, Function<BaseSearchableEntity, User> f) {
        for (BaseSearchableEntity w : items) {
            if (searchType == SearchOptions.SearchType.BY_ID_MULTIPLE && f != null) {
                System.out.println(f.apply(w));
            }
            w.print(System.out);
            System.out.print(System.lineSeparator());
        }
    }
}
