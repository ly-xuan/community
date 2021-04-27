package com.liu.community.controller.interceptor;

import com.liu.community.service.DataService;
import com.liu.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

//        System.out.println(request.getRemoteHost());
//        System.out.println(request.getRemoteUser());
//        System.out.println(request.getRemoteAddr());

        //根据ip统计uv(独立访问者)
        String ip = request.getRemoteHost();
        dataService.recordUv(ip);

        //根据登录用户的id统计dau(日活)
        if (hostHolder.getUsers()!=null){
            int id = hostHolder.getUsers().getId();
            dataService.recordDau(id);
        }

        return true;
    }
}
