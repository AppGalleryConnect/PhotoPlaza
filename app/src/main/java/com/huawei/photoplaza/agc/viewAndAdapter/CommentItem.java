package com.huawei.photoplaza.agc.viewAndAdapter;

public class CommentItem {

        private String userName;

        private String comment;

        public CommentItem(String userID,
                           String commentID,
                           String commentText,
                           String name,
                           String commentData){
            this.userName = name;
            this.comment = commentText;
        }

        public String getName(){
            return userName;
        }

        public String getComment(){
            return comment;
        }
}