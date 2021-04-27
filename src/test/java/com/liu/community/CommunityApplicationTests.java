package com.liu.community;

import com.liu.community.dao.DiscussPostMapper;
import com.liu.community.dao.LoginTicketMapper;
import com.liu.community.dao.UserMapper;
import com.liu.community.entity.DiscussPost;
import com.liu.community.entity.User;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
class CommunityApplicationTests {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void test0(){
        String s = CommunityUtil.md5("liuyixuan24" + "83773");
        System.out.println(s);
    }

    @Test
    public void test() {
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(149, 0, 10, false);
        discussPosts.forEach(System.out::println);

        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);

        User user = userMapper.selectById(11);
        System.out.println(user);

    }
    @Test
    public void test1() {
//        LoginTicket loginTicket = new LoginTicket();
//        loginTicket.setUserId(101);
//        loginTicket.setTicket("abc");
//        loginTicket.setStatus(0);
//        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000*60*10));
//
//        System.out.println(loginTicketMapper.insertLoginTicket(loginTicket));


        System.out.println(loginTicketMapper.updateStatus("abc",1));

        System.out.println(loginTicketMapper.selectByTicket("abc"));


    }
    @Test
    public void testTextMail(){
        mailClient.sendMail("1244494083@qq.com","java测试发送文本内容邮件",LocalDateTime.now()+" : spring-Mail配合新浪邮箱账号测试发送邮件");
    }
    @Test
    public void testHtmlMail(){
        Context context = new Context();
        context.setVariable("username","老磕");

        String content = templateEngine.process("/mail/demo", context);
        System.out.println(content);

        mailClient.sendMail("1244494083@qq.com","java测试发送html内容邮件",content);
    }

    //一个特殊的邮件
    @Test
    public void testHtmlMail2(){
        Context context = new Context();

        String content = templateEngine.process("/mail/specialDemo",context);
        System.out.println(content);

        mailClient.sendMail("1244494083@qq.com","Mamba Forever",content);
    }
}
