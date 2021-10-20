package com.huawei.agc.photoplaza.viewAndAdapter;

public class CommentItem {

    private final String userName;

    private final String comment;

    public CommentItem(
            String userID,
            String commentID,
            String commentText,
            String name,
            String commentData) {
        this.userName = name;
        this.comment = commentText;
    }

    public String getName() {
        return userName;
    }

    public String getComment() {
        return comment;
    }
}