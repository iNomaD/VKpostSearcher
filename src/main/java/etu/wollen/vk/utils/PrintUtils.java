package etu.wollen.vk.utils;

import etu.wollen.vk.model.conf.Board;
import etu.wollen.vk.model.conf.SearchOptions;
import etu.wollen.vk.model.conf.User;
import etu.wollen.vk.model.database.BaseSearchableEntity;
import etu.wollen.vk.model.database.BoardComment;
import etu.wollen.vk.model.database.WallPost;
import etu.wollen.vk.model.database.WallPostComment;
import etu.wollen.vk.model.database.WallPostLike;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
        try (PrintStream out = new PrintStream(new FileOutputStream(outputFileName), true, StandardCharsets.UTF_8)) {
            switch (searchType){
                case BY_ID_SINGLE:
                    out.println("User: " + usersMap.values().iterator().next() + LINE_SEPARATOR);
                    break;
                case BY_ID_MULTIPLE:
                    StringBuilder users = new StringBuilder();
                    for (User friend : usersMap.values()){
                        users.append(friend).append(", ");
                    }
                    out.println("Users: " + users + LINE_SEPARATOR);
                    break;
                case BY_PATTERN:
                    out.println("Pattern: [" + so.getFindPattern() + "]" + LINE_SEPARATOR);
                    break;
            }

            if (cluster) {
                out.println(posts.size() + " posts found!" + LINE_SEPARATOR);
                out.println(comments.size() + " comments found!" + LINE_SEPARATOR);
                out.println(boardComments.size() + " board comments found!" + LINE_SEPARATOR);
                if (so.getSearchType() != SearchOptions.SearchType.BY_PATTERN) {
                    out.println(answers.size() + " answers found!" + LINE_SEPARATOR);
                    out.println(likes.size() + " post likes found!" + LINE_SEPARATOR);
                }
                for (Map.Entry<Long, String> group : so.getGroupNames().entrySet()) {
                    long groupId = -group.getKey(); // group value without minus here
                    String name = group.getValue();
                    List<WallPost> selectedPosts = posts.stream().filter(post -> post.getGroupId() == groupId).collect(Collectors.toList());
                    List<WallPostComment> selectedComments = comments.stream().filter(comment -> comment.getGroupId() == groupId).collect(Collectors.toList());
                    List<WallPostComment> selectedAnswers = answers.stream().filter(comment -> comment.getGroupId() == groupId).collect(Collectors.toList());
                    List<WallPostLike> selectedLikes = likes.stream().filter(like -> like.getOwnerId() == groupId).collect(Collectors.toList());

                    if (!selectedPosts.isEmpty() || !selectedComments.isEmpty() || !selectedAnswers.isEmpty() || !selectedLikes.isEmpty()) {
                        out.println("<<< " + name + " >>>");
                        printItems(out, selectedPosts, searchType, a -> usersMap.get(((WallPost)a).getSignerId()));
                        printItems(out, selectedComments, searchType, a -> usersMap.get(((WallPostComment)a).getFromId()));
                        if (so.getSearchType() != SearchOptions.SearchType.BY_PATTERN) {
                            printItems(out, selectedAnswers, searchType, a -> usersMap.get(((WallPostComment)a).getReplyToUser()));
                            printItems(out, selectedLikes, searchType, a -> usersMap.get(((WallPostLike)a).getUser()));
                        }
                        out.println(LINE_SEPARATOR + LINE_SEPARATOR);
                    }
                }
                for (Map.Entry<Board, String> boardEntry : so.getTopicTitles().entrySet()) {
                    Board board = boardEntry.getKey();
                    String name = boardEntry.getValue();
                    List<BoardComment> selectedBoardComments = boardComments.stream().filter(comment -> comment.getGroupId() == board.getGroupId() && comment.getTopicId() == board.getTopicId()).collect(Collectors.toList());

                    if (!selectedBoardComments.isEmpty()) {
                        out.println("<<< " + name + " >>>");
                        printItems(out, selectedBoardComments, searchType, a -> usersMap.get(((BoardComment) a).getFromId()));
                    }
                }
            } else {
                out.println(posts.size() + " posts found!" + LINE_SEPARATOR);
                printItems(out, posts, searchType, a -> usersMap.get(((WallPost)a).getSignerId()));

                out.println(LINE_SEPARATOR + LINE_SEPARATOR + comments.size() + " comments found!" + LINE_SEPARATOR);
                printItems(out, comments, searchType, a -> usersMap.get(((WallPostComment)a).getFromId()));

                out.println(LINE_SEPARATOR + LINE_SEPARATOR + boardComments.size() + " board comments found!" + LINE_SEPARATOR);
                printItems(out, boardComments, searchType, a -> usersMap.get(((BoardComment)a).getFromId()));

                if (so.getSearchType() != SearchOptions.SearchType.BY_PATTERN) {
                    out.println(LINE_SEPARATOR + LINE_SEPARATOR + answers.size() + " answers found!" + LINE_SEPARATOR);
                    printItems(out, answers, searchType, a -> usersMap.get(((WallPostComment)a).getReplyToUser()));

                    out.println(LINE_SEPARATOR + LINE_SEPARATOR + likes.size() + " post likes found!" + LINE_SEPARATOR);
                    printItems(out, likes, searchType, a -> usersMap.get(((WallPostLike)a).getUser()));
                }
            }
        }
    }

    private static void printItems(PrintStream out, List<? extends BaseSearchableEntity> items, SearchOptions.SearchType searchType, Function<BaseSearchableEntity, User> f) {
        for (BaseSearchableEntity w : items) {
            if (searchType == SearchOptions.SearchType.BY_ID_MULTIPLE && f != null) {
                out.println(f.apply(w));
            }
            w.print(out);
            out.print(System.lineSeparator());
        }
    }
}
