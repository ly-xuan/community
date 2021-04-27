package com.liu.community.controller;

import com.liu.community.entity.Event;
import com.liu.community.entity.User;
import com.liu.community.event.EventProducer;
import com.liu.community.service.LikeService;
import com.liu.community.util.Code;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.HostHolder;
import com.liu.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

@Controller
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private EventProducer eventProducer;

    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType, int entityId,int entityUserId,int postId){
        User user = hostHolder.getUsers();

        //点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        //数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        //状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        HashMap<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        //触发点赞事件
        if (likeStatus==1){
            Event event = new Event()
                    .setTopic("like")
                    .setUserId(hostHolder.getUsers().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId",postId);
            eventProducer.handleEvent(event);
        }

        if (entityType== Code.ENTITY_TYPE_POST.getCode()){
            redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), postId);
        }

        return CommunityUtil.getJson(0,null,map);
    }

}
