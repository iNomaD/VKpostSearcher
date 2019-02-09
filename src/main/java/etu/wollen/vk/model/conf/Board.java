package etu.wollen.vk.model.conf;

import java.util.Objects;

public class Board {

    private final long groupId;
    private final long topicId;

    public Board(long groupId, long topicId) {
        this.groupId = groupId;
        this.topicId = topicId;
    }

    public long getGroupId() {
        return groupId;
    }

    public long getTopicId() {
        return topicId;
    }

    @Override
    public String toString() {
        return "topic-" + groupId + "_" + topicId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return groupId == board.groupId &&
                topicId == board.topicId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, topicId);
    }
}
