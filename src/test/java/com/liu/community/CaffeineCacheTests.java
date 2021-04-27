package com.liu.community;

import com.liu.community.service.DiscussPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CaffeineCacheTests {

    @Autowired
    private DiscussPostService discussPostService;

    @Test
    public void test(){
        System.out.println(discussPostService.findDiscussPosts(0,0,10,true));
        System.out.println(discussPostService.findDiscussPosts(0,0,10,true));
        System.out.println(discussPostService.findDiscussPosts(0,0,10,true));
        System.out.println(discussPostService.findDiscussPosts(0,0,10,false));
    }

}
