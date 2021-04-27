package com.liu.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.liu.community.entity.Message;
import com.liu.community.entity.Page;
import com.liu.community.entity.User;
import com.liu.community.service.MessageService;
import com.liu.community.service.UserService;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    //私信列表
    @GetMapping("/letter/list")
    public String getLetters(Model model, Page page){
        User user = hostHolder.getUsers();
        //分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        //会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList!=null){
            for (Message conversation : conversationList) {
                Map<String, Object> map = new HashMap<>();
                //每个会话的未读私信数量
                map.put("unreadCount",messageService.findLetterUnreadCount(
                        user.getId(),conversation.getConversationId()));
                //每个会话的最新私信
                map.put("conversation", conversation);
                //每个会话的消息数量
                map.put("letterCount",messageService.findLetterCount(conversation.getConversationId()));
                //每个会话对面用户的头像
                map.put("target",userService.findById(
                        user.getId()==conversation.getFromId()?conversation.getToId():conversation.getFromId()));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations",conversations);

        //查询未读消息的数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        //查询未读消息的数量
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);
        return "/site/letter";
    }

    @GetMapping("/letter/detail/{conversationId}")
    public String letterDetail(
            @PathVariable("conversationId") String conversationId, Model model, Page page){
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        List<Message> letterList = messageService.findLetters(
                conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList!=null){
            for (Message message : letterList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("letter",message);
                map.put("fromUser",userService.findById(message.getFromId()));

                letters.add(map);
            }

            //点击私信详情页时，将会话中未读的消息改为已读
            List<Integer> readList = letterList.stream()
                    .filter(message -> message.getToId() == hostHolder.getUsers().getId() && message.getStatus() == 0)
                    .map(Message::getId)
                    .collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(readList)) {
                messageService.readMessage(readList);
            }
        }
        model.addAttribute("letters",letters);
        //私信目标
        model.addAttribute("target",getLetterTarget(conversationId));

        return "/site/letter-detail";
    }
    private User getLetterTarget(String conversationId){
        String[] userIds = conversationId.split("_");
        int d0 = Integer.parseInt(userIds[0]);
        int d1 = Integer.parseInt(userIds[1]);

        if (hostHolder.getUsers().getId()==d0){
            return userService.findById(d1);
        }else {
            return userService.findById(d0);
        }

    }

    @PostMapping("/letter/send")
    @ResponseBody
    public String sendLetter(String toName, String content){
        User target = userService.findByName(toName);
        if (target==null){
            return CommunityUtil.getJson(1,"目标用户不存在！");
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUsers().getId());
        message.setToId(target.getId());

        if (message.getFromId() < message.getToId()){
            message.setConversationId(message.getFromId() + "_" +message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" +message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJson(0);
    }

    @GetMapping("/notice/list")
    public String getNotice(Model model){
        User user = hostHolder.getUsers();

        //查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), "comment");
        if (message!=null){
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.putAll(data);
            messageVo.put("user",userService.findById((Integer) data.get("userId")));

            int count = messageService.findNoticeCount(user.getId(),"comment");
            messageVo.put("count",count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(),"comment");
            messageVo.put("unreadCount",unreadCount);

            model.addAttribute("commentNotice",messageVo);
        }
        //查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), "like");
        if (message!=null){
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.putAll(data);
            messageVo.put("user",userService.findById((Integer) data.get("userId")));

            int count = messageService.findNoticeCount(user.getId(),"like");
            messageVo.put("count",count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(),"like");
            messageVo.put("unreadCount",unreadCount);

            model.addAttribute("likeNotice",messageVo);
        }
        //查询关注类通知
        message = messageService.findLatestNotice(user.getId(), "follow");
        if (message!=null){
            Map<String, Object> messageVo = new HashMap<>();
            messageVo.put("message",message);
            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.putAll(data);
            messageVo.put("user",userService.findById((Integer) data.get("userId")));

            int count = messageService.findNoticeCount(user.getId(),"follow");
            messageVo.put("count",count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(),"follow");
            messageVo.put("unreadCount",unreadCount);

            model.addAttribute("followNotice",messageVo);
        }

        //查询未读消息的数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        //查询未读消息的数量
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);
        return "site/notice";
    }
    @GetMapping("/notice/detail/{topic}")
    public String detail(@PathVariable("topic") String topic, Page page, Model model){
        User user = hostHolder.getUsers();

        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));

        List<Message> noticeList = messageService.findNotices(
                user.getId(),topic,page.getOffset(),page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList!=null){
            for (Message notice : noticeList) {
                Map<String, Object> noticeVo = new HashMap<>();
                //通知
                noticeVo.put("notice",notice);
                //内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map data = JSONObject.parseObject(content, HashMap.class);
                noticeVo.putAll(data);
                noticeVo.put("user",userService.findById((Integer) data.get("userId")));
                //发送通知的作者
                noticeVo.put("fromUser",userService.findById(notice.getFromId()));

                noticeVoList.add(noticeVo);
            }
        }
        model.addAttribute("notices",noticeVoList);

        //设置已读
        assert noticeList != null;
        List<Integer> ids = noticeList.stream().map(Message::getId).collect(Collectors.toList());
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "site/notice-detail";
    }
}
