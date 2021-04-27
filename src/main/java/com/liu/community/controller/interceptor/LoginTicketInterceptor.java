package com.liu.community.controller.interceptor;

import com.liu.community.entity.LoginTicket;
import com.liu.community.entity.User;
import com.liu.community.service.MessageService;
import com.liu.community.service.UserService;
import com.liu.community.util.HostHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Date;

@Component
@Slf4j
public class LoginTicketInterceptor implements HandlerInterceptor {

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;


    @Override
    /**
     * 对静态资源外的所有请求检查是否登录，有则把user信息存入容器，有无都放行
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String cookieValue = null;
        //将cookies数组转为流过滤得到 name为"ticket" 的cookie的value
        if (!ObjectUtils.isEmpty(request.getCookies())){
            cookieValue = Arrays.stream(request.getCookies())
                    .filter(cookie -> "ticket".equals(cookie.getName()) )
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse("");
        }

        if (!StringUtils.isEmpty(cookieValue)){
            LoginTicket loginTicket = userService.findLoginTicket(cookieValue);
            if (!ObjectUtils.isEmpty(loginTicket) && loginTicket.getStatus()==0 && loginTicket.getExpired().after(new Date())){
                User user = userService.findById(loginTicket.getUserId());
                //request.setAttribute("user",user);
                //将user存入容器内
                hostHolder.setUsers(user);

                //构建用户认证的结果，并存入securityContext，以便于security进行授权
                Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId())
                );
                SecurityContextHolder.setContext(new SecurityContextImpl(authenticationToken));

            }
        }
        return true;
    }

    @Override
    /**
     * 检查是否登录（容器内有user信息），有则将其放入model内，用于thymeleaf模板引用
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUsers();
        if (user != null && modelAndView != null){
            System.out.println(user);
            modelAndView.addObject("loginUser",user);
        }
        if (user != null && modelAndView != null){
            int messageUnreadCount = 0;
            messageUnreadCount = messageService.findMessageUnreadCount(user.getId());
            modelAndView.addObject("messageUnreadCount",messageUnreadCount);
        }

    }

    @Override
    /**
     * 在模板引擎用完后删除user
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
