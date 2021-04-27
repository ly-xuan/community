package com.liu.community.util;

import com.liu.community.entity.User;
import org.springframework.stereotype.Component;

@Component
//容器 代替session 根据线程隔离
public class HostHolder {

    private ThreadLocal<User> user = new ThreadLocal<>();

    public void setUsers(User setUser){
        user.set(setUser);
    }

    public User getUsers(){
        return user.get();
    }

    public void clear(){
        user.remove();
    }
}
