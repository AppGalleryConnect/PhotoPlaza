/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2020. All rights reserved.
 * Generated by the CloudDB ObjectType compiler.  DO NOT EDIT!
 */
package com.huawei.agc.photoplaza.model;

import com.huawei.agconnect.cloud.database.CloudDBZoneObject;
import com.huawei.agconnect.cloud.database.Text;
import com.huawei.agconnect.cloud.database.annotations.DefaultValue;
import com.huawei.agconnect.cloud.database.annotations.EntireEncrypted;
import com.huawei.agconnect.cloud.database.annotations.NotNull;
import com.huawei.agconnect.cloud.database.annotations.Indexes;
import com.huawei.agconnect.cloud.database.annotations.PrimaryKeys;

import java.util.Date;

/**
 * Definition of ObjectType CommentTable.
 *
 * @since 2021-09-01
 */
@PrimaryKeys({"CommentID"})
@Indexes({"commentID:CommentID"})
public final class CommentTable extends CloudDBZoneObject {
    private String CommentID;

    private String PhotoID;

    private String UserID;

    private String UserName;

    private String CommentText;

    private Date CreateTime;

    @NotNull
    @DefaultValue(booleanValue = true)
    private Boolean SubFlag;

    public CommentTable() {
        super(CommentTable.class);
        this.SubFlag = true;
    }

    public void setCommentID(String CommentID) {
        this.CommentID = CommentID;
    }

    public String getCommentID() {
        return CommentID;
    }

    public void setPhotoID(String PhotoID) {
        this.PhotoID = PhotoID;
    }

    public String getPhotoID() {
        return PhotoID;
    }

    public void setUserID(String UserID) {
        this.UserID = UserID;
    }

    public String getUserID() {
        return UserID;
    }

    public void setUserName(String UserName) {
        this.UserName = UserName;
    }

    public String getUserName() {
        return UserName;
    }

    public void setCommentText(String CommentText) {
        this.CommentText = CommentText;
    }

    public String getCommentText() {
        return CommentText;
    }

    public void setCreateTime(Date CreateTime) {
        this.CreateTime = CreateTime;
    }

    public Date getCreateTime() {
        return CreateTime;
    }

    public void setSubFlag(Boolean SubFlag) {
        this.SubFlag = SubFlag;
    }

    public Boolean getSubFlag() {
        return SubFlag;
    }

}