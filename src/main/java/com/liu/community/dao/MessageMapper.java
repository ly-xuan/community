package com.liu.community.dao;

import com.liu.community.entity.Message;

import java.util.List;

public interface MessageMapper {

    //查询当前用户的会话列表，针对每条会话只返回一条最新的数据
    List<Message> selectConversations(int userId, int offset, int limit);

    //查询当前用户的会话数量
    int selectConversationCount(int userId);

    //查询某个会话包含的私信列表
    List<Message> selectLetters(String conversationId, int offset, int limit);

    //查询某个会话所包含的私信数量
    int selectLetterCount(String conversationId);

    //查询未读私信的数量
    int selectLetterUnreadCount(int userId, String conversationId);

    //新增消息
    int insertMessage(Message message);

    //修改消息的状态
    int updateMessage(List<Integer> ids, int status);

    //查询某个主题下的最新的通知
    Message selectLatestNotice(int userId, String topic);
    //查询某个主题所包含的通知的数量
    int selectNoticeCount(int userId, String topic);
    //查询某个未读的通知的数量
    int selectNoticeUnreadCount(int userId, String topic);
    //某个主题所包含的通知列表
    List<Message> selectNotice(int userId, String topic, int offset, int limit);

    //查询未读的通知和消息的总count
    int selectMessageUnreadCount(int toId);
}
