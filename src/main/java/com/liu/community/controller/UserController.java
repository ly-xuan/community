package com.liu.community.controller;

import com.liu.community.annotation.LoginRequired;
import com.liu.community.entity.User;
import com.liu.community.service.FollowService;
import com.liu.community.service.LikeService;
import com.liu.community.service.UserService;
import com.liu.community.util.Code;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.HostHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.upload}")
    private String upload;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    //自定义注解
    @LoginRequired
    @GetMapping("/setting")
    public String getSettingPage(){
        return "/site/setting";
    }

    @LoginRequired
    @PostMapping("/upload")
    public String upload(MultipartFile headerImage, Model model){
        if (headerImage == null){
            model.addAttribute("error","您还没有选择图片!");
            return "/site/setting";
        }

        String FileName = headerImage.getOriginalFilename();
        String suffix = FileName.substring(FileName.lastIndexOf("."));
        if (StringUtils.isEmpty(suffix)){
            model.addAttribute("error","文件的格式不正确!");
            return "/site/setting";
        }

        //生成随机文件名
        FileName = CommunityUtil.generateUUID() + suffix;
        //确定文件存放的路径
        File dest = new File(upload + "/" + FileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            log.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常",e);
        }

        //更新当前用户的头像的路径
        User user = hostHolder.getUsers();
        String headerUrl = domain + contextPath + "/user/header/" + FileName;
        userService.saveHeader(user.getId(),headerUrl);

        return "redirect:/index";
    }
    @GetMapping("/header/{fileName}")
    public void getHeader (@PathVariable("fileName") String fileName, HttpServletResponse response){
        // 服务器存放路径的位置
        fileName = upload + "/" + fileName;
        //文件的后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+suffix);
        try (
                FileInputStream is = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ){

            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b=is.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            log.error("读取头像失败："+e.getMessage());
        }
    }

    @PostMapping("/updatePassword")
    public String updatePassword(String oldPassword,
                                 String newPassword,
                                 Model model,
                                 @CookieValue("ticket") String ticket){
        if (StringUtils.isEmpty(oldPassword)){
            model.addAttribute("updateError","旧密码不能为空！");
            return "/site/setting";
        }
        if (StringUtils.isEmpty(newPassword)){
            model.addAttribute("updateError","新密码不能为空！");
            return "/site/setting";
        }
        User user = hostHolder.getUsers();
        String oldPasswordMd5 = CommunityUtil.md5(oldPassword+user.getSalt());
        if (user!=null && oldPasswordMd5.equals(user.getPassword()) && ticket!=null){
            userService.savePassword(user.getId(),CommunityUtil.md5(newPassword+user.getSalt()));
            userService.logout(ticket);
            return "redirect:/login";
        }else {
            model.addAttribute("updateError","密码错误！修改失败！");
            return "/site/setting";
        }
    }
    @GetMapping("/profile/{targetId}")
    public String profile(
            @PathVariable("targetId") int targetId, Model model){
        User target = userService.findById(targetId);
        if (target==null){
            throw new RuntimeException("该用户不存在");
        }
        //关注了数量
        long followeeCount = followService.followeeCount(target.getId(), Code.ENTITY_TYPE_USER.getCode());
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount = followService.followerCount(Code.ENTITY_TYPE_USER.getCode(),targetId);
        model.addAttribute("followerCount",followerCount);
        //你是否关注了
        boolean isFollow = false;
        if (hostHolder.getUsers()!=null){
            isFollow = followService.isFollow(
                    hostHolder.getUsers().getId(),Code.ENTITY_TYPE_USER.getCode(),targetId);
        }
        model.addAttribute("isFollow",isFollow);
        //用户信息
        model.addAttribute("target",target);
        //点赞信息
        int likeCount = likeService.findUserLikeCount(target.getId());
        model.addAttribute("likeCount",likeCount);

        return "/site/profile";
    }
}
