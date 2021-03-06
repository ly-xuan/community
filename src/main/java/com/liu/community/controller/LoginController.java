package com.liu.community.controller;

import com.google.code.kaptcha.Producer;
import com.liu.community.entity.User;
import com.liu.community.service.UserService;
import com.liu.community.util.Code;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class LoginController {
    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;


    @GetMapping("/register")
    public String toRegister(){
        return "/site/register";
    }
    @GetMapping("/login")
    public String toLogin(){
        return "/site/login";
    }
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
    @PostMapping("/register")
    public String doRegister(User user , Model model){
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()){
            model.addAttribute("msg","??????????????????????????????????????????????????????????????????????????????????????????");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";
        }

    }
    @PostMapping("/login")
    public String doLogin(String username, String password, String verifyCode, boolean remember_me,
                        @CookieValue("kaptchaOwner") String kaptchaOwner, Model model/*, HttpSession session*/, HttpServletResponse response){
//        String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha = null;
        if (!StringUtils.isEmpty(kaptchaOwner)) {
            String rediskey = RedisKeyUtil.getKaptcha(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(rediskey);
        }
        if (StringUtils.isEmpty(kaptcha) || StringUtils.isEmpty(verifyCode) || !kaptcha.equals(verifyCode)){
            model.addAttribute("codeMsg", "??????????????????!");
            return "/site/login";
        }

        // ??????????????????
        int expiredSeconds = remember_me? 3600 * 24 * 7 : 3600;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        } else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }


    }
    //??????
    @GetMapping("/activation/{id}/{activationCode}")
    public String active(Model model, @PathVariable int id,@PathVariable String activationCode){
        Code active = userService.active(id, activationCode);
        if (active == Code.ACTIVATION_SUCCESS){
            model.addAttribute("msg","????????????,????????????????????????????????????");
            model.addAttribute("target","/login");
        } else if (active == Code.ACTIVATION_REPEAT){
            model.addAttribute("msg","??????????????????????????????????????????");
            model.addAttribute("target","/");
        } else if (active == Code.ACTIVATION_FAILURE){
            model.addAttribute("msg","????????????,?????????????????????????????????");
            model.addAttribute("target","/");
        }
        return "/site/operate-result";
    }
    //???????????????
    @GetMapping("/kaptcha")
    public void getkaptcha(HttpServletResponse response/*, HttpSession session*/){
        //???????????????
        String text = kaptchaProducer.createText().toLowerCase();
        BufferedImage image = kaptchaProducer.createImage(text);

        //??????????????????session
        //session.setAttribute("kaptcha",text);

        //??????????????????
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        //??????????????????redis
        String redisKey = RedisKeyUtil.getKaptcha(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        //???????????????????????????
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        } catch (IOException e) {
            log.error("?????????????????????: "+e.getMessage());
        }
    }
}
