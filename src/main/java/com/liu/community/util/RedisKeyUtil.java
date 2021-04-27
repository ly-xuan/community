package com.liu.community.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";

    private static final String PREFIX_ENTITY_LIKE = "like:entity";

    private static final String PREFIX_USER_ENTITY_LIKE = "like:user";

    //粉丝
    private static final String FOLLOWER = "follower";
    //关注目标
    private static final String FOLLOWEE = "followee";
    //验证码
    private static final String KAPTCHA = "kaptcha";
    //登录凭证
    private static final String TICKET = "ticket";
    //用户信息缓存key
    private static final String USER = "user";

    //独立访问者(ip)
    private static final String UV = "uv";
    //日活跃用户(id)
    private static final String DAU = "dau";


    //某个实体的赞
    //like:entity:entityType:entityId -> set
    public static String getEntityLikeKey(int entityType, int entityId){
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    //某个用户的赞
    public static String getUserLikeKey(int entityUserId){
        return PREFIX_USER_ENTITY_LIKE + SPLIT + entityUserId;
    }

    //某个用户的关注的实体
    //followee:userId:entityType -> ZSet(entityId,now)
    public static String getFolloweeKey(int userId, int entityType){
        return FOLLOWEE + SPLIT + userId + SPLIT +  entityType;
    }

    //某个实体的关注者(粉丝)
    //follower:entityType:entityId -> ZSet(userId,now)
    public static String getFollowerKey(int entityType, int entityId){
        return FOLLOWER + SPLIT + entityType + SPLIT +  entityId;
    }
    //登录验证码
    public static String getKaptcha(String owner){
        return KAPTCHA + SPLIT +owner;
    }
    //登录的凭证
    public static String getTicketKey(String ticket){
        return TICKET + SPLIT + ticket;
    }
    //用户
    public static String getUserKey(int userId){
        return USER + SPLIT + userId;
    }

    //单日UV
    public static String  getUvKey(String date){
        return UV + SPLIT + date;
    }
    //区间UV
    public static String  getUvKey(String start, String end){
        return UV + SPLIT + start + SPLIT + end;
    }
    //单日DAU
    public static String  getDauKey(String date){
        return DAU + SPLIT + date;
    }
    //区间DAU
    public static String  getDauKey(String start, String end){
        return DAU + SPLIT + start + SPLIT + end;
    }

    public static String getPostScoreKey(){
        return "post" + SPLIT + "score";
    }
}
