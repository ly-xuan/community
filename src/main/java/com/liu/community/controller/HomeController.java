package com.liu.community.controller;

import com.liu.community.entity.DiscussPost;
import com.liu.community.entity.Page;
import com.liu.community.entity.User;
import com.liu.community.service.DiscussPostService;
import com.liu.community.service.LikeService;
import com.liu.community.service.MessageService;
import com.liu.community.service.UserService;
import com.liu.community.util.Code;
import com.liu.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserService userService;

    @GetMapping(value = {"/", "/index"})
    public String getIndex(Model model, Page page,
                           @RequestParam(defaultValue = "0") int isByScore){
        page.setPath("/index?isByScore=" + isByScore);
        page.setRows(discussPostService.findDiscussPostRows(0));

        List<DiscussPost> lists = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(),isByScore==1);
        ArrayList<Map<String,Object>> discussPosts = new ArrayList<>();

        if (lists != null){
            for (DiscussPost post : lists) {
                Map<String, Object> map = new HashMap<>();
                map.put("post",post);

                User user = userService.findById(post.getUserId());
                map.put("user",user);

                long likeCount = likeService.findEntityLikeCount(Code.ENTITY_TYPE_POST.getCode(), post.getId());
                map.put("likeCount",likeCount);

                discussPosts.add(map);
            }
        }
        if (hostHolder.getUsers()!=null){
            model.addAttribute("letterUnreadCount",messageService.findLetterUnreadCount(
                    hostHolder.getUsers().getId(),null));
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("isByScore",isByScore);
        //model.addAttribute("page",page);
        /*
        只要在controller参数里有Model，springMvc就会自动初始化并注入model,不用new Model()
        并且参数里的对象，也会被初始化并装载入model中,不用model.addAttribute
         */
        return "/index";
    }

    @GetMapping("/error")
    public String getErrorPage(){
        return "/error/500";
    }

    @GetMapping("/denied")
    public String getDeniedPage(){
        return "/error/404";
    }
}
