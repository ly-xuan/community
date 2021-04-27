package com.liu.community.controller;

import com.liu.community.entity.Event;
import com.liu.community.entity.Page;
import com.liu.community.entity.User;
import com.liu.community.event.EventProducer;
import com.liu.community.service.FollowService;
import com.liu.community.service.UserService;
import com.liu.community.util.Code;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityType, int entityId){
        User user = hostHolder.getUsers();
        followService.follow(user.getId(), entityType, entityId);

        //触发关注事件
        Event event = new Event()
                .setTopic("follow")
                .setUserId(hostHolder.getUsers().getId())
                .setEntityType(entityType)
                .setEntityId(entityId);
        eventProducer.handleEvent(event);

        return CommunityUtil.getJson(0,"已经关注");

    }

    @PostMapping("/unFollow")
    @ResponseBody
    public String unFollow(int entityType, int entityId){
        User user = hostHolder.getUsers();
        followService.unFollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJson(0,"已取消关注");

    }
    @GetMapping("/followee/{userId}")
    public String followee(
            @PathVariable("userId") int userId, Page page, Model model){
        User byId = userService.findById(userId);
        Assert.notNull(byId,"用户不存在");
        model.addAttribute("user",byId);

        page.setLimit(5);
        page.setRows((int)followService.followeeCount(userId, Code.ENTITY_TYPE_USER.getCode()));
        page.setPath("/followee/" + userId);
        List<Map<String, Object>> followees = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (followees!=null){
            for (Map<String, Object> map : followees) {
                User user = (User)map.get("user");
                map.put("isFollow",isFollow(user.getId()));
            }
        }
        model.addAttribute("users",followees);
        return "site/followee";

    }
    @GetMapping("/follower/{userId}")
    public String follower(
            @PathVariable("userId") int userId, Page page, Model model){
        User byId = userService.findById(userId);
        Assert.notNull(byId,"用户不存在");
        model.addAttribute("user",byId);

        page.setLimit(5);
        page.setRows((int)followService.followerCount(Code.ENTITY_TYPE_USER.getCode(),userId));
        page.setPath("/follower/" + userId);
        List<Map<String, Object>> followers = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (followers!=null){
            for (Map<String, Object> map : followers) {
                User user = (User)map.get("user");
                map.put("isFollow",isFollow(user.getId()));
            }
        }
        model.addAttribute("users",followers);
        return "site/follower";

    }
    private boolean isFollow(int targetId){
        if (hostHolder.getUsers()==null){
            return false;
        }
        return followService.isFollow(hostHolder.getUsers().getId(),Code.ENTITY_TYPE_USER.getCode(),targetId);
    }

}
