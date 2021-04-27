package com.liu.community.controller;

import com.liu.community.entity.Comment;
import com.liu.community.entity.DiscussPost;
import com.liu.community.entity.Event;
import com.liu.community.event.EventProducer;
import com.liu.community.service.CommentService;
import com.liu.community.service.DiscussPostService;
import com.liu.community.util.Code;
import com.liu.community.util.HostHolder;
import com.liu.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @PostMapping("/add/{discussPostId}")
    public String addComment(
            @PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUsers().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论事件
        Event event = new Event()
                .setTopic("comment")
                .setUserId(hostHolder.getUsers().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);
        if (comment.getEntityType()== Code.ENTITY_TYPE_POST.getCode()){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());

            redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), discussPostId);
        } else if (comment.getEntityType()== Code.ENTITY_TYPE_COMMENT.getCode()){
            Comment target = commentService.findById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.handleEvent(event);

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
