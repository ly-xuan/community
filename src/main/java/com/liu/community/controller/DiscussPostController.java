package com.liu.community.controller;

import com.liu.community.annotation.LoginRequired;
import com.liu.community.entity.Comment;
import com.liu.community.entity.DiscussPost;
import com.liu.community.entity.Page;
import com.liu.community.entity.User;
import com.liu.community.service.CommentService;
import com.liu.community.service.DiscussPostService;
import com.liu.community.service.LikeService;
import com.liu.community.service.UserService;
import com.liu.community.util.Code;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.HostHolder;
import com.liu.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("discuss")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private HostHolder hostHolder;

    @PostMapping("/add")
    @ResponseBody
    @LoginRequired
    public String addDiscuss(String title, String content){
        User user = hostHolder.getUsers();
        if (user==null){
            return CommunityUtil.getJson(403);
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, post.getId());

        return CommunityUtil.getJson(0,"发布成功！");
    }
    @GetMapping("/detail/{discussPostId}")
    public String detail(
            @PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        //作者
        User byId = userService.findById(post.getUserId());
        model.addAttribute("user",byId);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(Code.ENTITY_TYPE_POST.getCode(), discussPostId);
        model.addAttribute("likeCount",likeCount);
        //点赞状态
        int likeStatus = hostHolder.getUsers()==null?
                0:likeService.findEntityLikeStatus(hostHolder.getUsers().getId(),Code.ENTITY_TYPE_POST.getCode(),discussPostId);
        model.addAttribute("likeStatus",likeStatus);

        //评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        //评论：给帖子的评论
        //回复：给评论的评论
        List<Comment> comments = commentService.findCommentsByEntity(Code.ENTITY_TYPE_POST.getCode(), post.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (comments!=null) {
            for (Comment comment : comments) {
                HashMap<String, Object> commentVo = new HashMap<>();
                //评论vo
                commentVo.put("comment", comment);
                //作者
                commentVo.put("user", userService.findById(comment.getUserId()));

                //点赞数量
                likeCount = likeService.findEntityLikeCount(Code.ENTITY_TYPE_COMMENT.getCode(), comment.getId());
                commentVo.put("likeCount",likeCount);
                //点赞状态
                likeStatus = hostHolder.getUsers()==null?
                        0:likeService.findEntityLikeStatus(hostHolder.getUsers().getId(),Code.ENTITY_TYPE_COMMENT.getCode(),comment.getId());
                commentVo.put("likeStatus",likeStatus);

                //回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(
                        Code.ENTITY_TYPE_COMMENT.getCode(), comment.getId(), 0, Integer.MAX_VALUE);
                //回复vo列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList!=null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        //回复
                        replyVo.put("reply", reply);
                        //作者
                        replyVo.put("user", userService.findById(reply.getUserId()));
                        //回复目标
                        User taget = reply.getTargetId()==0?null:userService.findById(reply.getTargetId());
                        replyVo.put("target", taget);

                        //点赞数量
                        likeCount = likeService.findEntityLikeCount(Code.ENTITY_TYPE_COMMENT.getCode(), reply.getId());
                        replyVo.put("likeCount",likeCount);
                        //点赞状态
                        likeStatus = hostHolder.getUsers()==null?
                                0:likeService.findEntityLikeStatus(hostHolder.getUsers().getId(),Code.ENTITY_TYPE_COMMENT.getCode(),reply.getId());
                        replyVo.put("likeStatus",likeStatus);

                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);

                //回复数量
                int replyCount = commentService.findCommentCount(Code.ENTITY_TYPE_COMMENT.getCode(), comment.getId());
                commentVo.put("replyCount", replyCount);

                commentVoList.add(commentVo);

            }

        }

        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";

    }
    //置顶
    @PostMapping("/top")
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id, 1);
        return CommunityUtil.getJson(0);
    }
    //加精
    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id, 1);
        redisTemplate.opsForSet().add(RedisKeyUtil.getPostScoreKey(), id);
        return CommunityUtil.getJson(0);
    }
    //删除
    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id, 2);
        return CommunityUtil.getJson(0);
    }

}
