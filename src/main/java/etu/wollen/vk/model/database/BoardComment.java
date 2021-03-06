package etu.wollen.vk.model.database;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static etu.wollen.vk.conf.Config.PRINT_DATE_FORMAT;

public class BoardComment extends BaseSearchableEntity {

    private long commentId;
    private long groupId;
    private long topicId;
    private long fromId;
    private String text;

    public BoardComment(Date date, long commentId, long groupId, long topicId, long fromId, String text) {
        super(date);
        this.commentId = commentId;
        this.groupId = groupId;
        this.topicId = topicId;
        this.fromId = fromId;
        this.text = text;
    }

    public long getCommentId() {
        return commentId;
    }

    public long getGroupId() {
        return groupId;
    }

    public long getTopicId() {
        return topicId;
    }

    public long getFromId() {
        return fromId;
    }

    public String getText() {
        return text;
    }

    public String getUrl(){
        return "https://vk.com/topic-"+groupId+"_"+topicId+"?post="+commentId;
    }

    @Override
    public void print(PrintStream printStream) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(PRINT_DATE_FORMAT);
        printStream.println(getUrl());
        printStream.println(dateFormat.format(date));
        printStream.println(text);
    }
}
