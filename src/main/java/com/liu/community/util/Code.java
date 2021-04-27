package com.liu.community.util;

public enum Code {

    //激活成功
    ACTIVATION_SUCCESS(0,"激活成功"),
    //激活重复
    ACTIVATION_REPEAT(1,"激活重复"),
    //激活失败
    ACTIVATION_FAILURE(2,"激活失败"),

    //实体类型：帖子
    ENTITY_TYPE_POST(1,"帖子"),
    //实体类型：帖子
    ENTITY_TYPE_COMMENT(2,"评论"),
    //实体类型：用户
    ENTITY_TYPE_USER(3,"用户");


    private int code;
    private String msg;

    Code(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
