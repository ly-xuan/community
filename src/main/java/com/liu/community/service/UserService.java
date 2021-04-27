package com.liu.community.service;

import com.liu.community.dao.UserMapper;
import com.liu.community.entity.LoginTicket;
import com.liu.community.entity.User;
import com.liu.community.util.Code;
import com.liu.community.util.CommunityUtil;
import com.liu.community.util.MailClient;
import com.liu.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService{

    @Autowired
    private UserMapper userMapper;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //缓存user信息,重构 findById(int id)
    //1.优先从缓冲中取值
    private User getCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User)redisTemplate.opsForValue().get(userKey);
    }
    //2.取不到时初始化缓存
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey, user, 1, TimeUnit.HOURS);
        return user;
    }
    //3.数据变更时清楚缓存数据
    private void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

    public User findById(int id){
//        return userMapper.selectById(id);
        User user = getCache(id);
        if (user==null){
            user = initCache(id);
        }
        return user;
    }

    public User findByName(String name){
        return userMapper.selectByName(name);
    }

    public Map<String,Object> register(User user){
        Map<String,Object> map = new HashMap<>();
        if (user == null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isEmpty(user.getUsername())){
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        if (StringUtils.isEmpty(user.getEmail())){
            map.put("emailMsg","邮箱不能为空！");
            return map;
        }
        if (StringUtils.isEmpty(user.getPassword())){
            map.put("passwordMsg","密码不能为空！");
            return map;
        }
        if (!ObjectUtils.isEmpty(userMapper.selectByName(user.getUsername()))){
            map.put("usernameMsg","该账号已经存在!");
            return map;
        }
        if (!ObjectUtils.isEmpty(userMapper.selectByEmail(user.getEmail()))){
            map.put("emailMsg","该邮箱已存在！");
            return map;
        }
        //注册
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //激活邮件
        Context context = new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活账号",content);

        return map;
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds){
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isEmpty(username)){
            map.put("usernameMsg","账号不能为空!");
            return map;
        }
        if (StringUtils.isEmpty(password)){
            map.put("passwordMsg","密码不能为空!");
            return map;
        }
        //验证账号
        User byName = userMapper.selectByName(username);
        if (byName == null) {
            map.put("usernameMsg","账号不存在!");
            return map;
        }
        if (byName.getStatus() == 0){
            map.put("usernameMsg","该账号未激活!");
            return map;
        }

        //验证密码
        password = CommunityUtil.md5(password + byName.getSalt());
        assert password != null;
        if (!password.equals(byName.getPassword())){
            map.put("passwordMsg","密码不正确!");
            return map;
        }
        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket(
                byName.getId(),
                CommunityUtil.generateUUID(),
                0,
                new Date(System.currentTimeMillis() + expiredSeconds * 1000)
        );
//        loginTicketMapper.insertLoginTicket(loginTicket);
        //存入redis
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public Code active(int userId, String code){
        User user = userMapper.selectById(userId);
        if (user !=null){
            if (user.getStatus() == 1){
                return Code.ACTIVATION_FAILURE;
            } else if (code.equals(user.getActivationCode())){
                userMapper.updateStatus(user.getId(),1);
                clearCache(userId);
                return Code.ACTIVATION_SUCCESS;
            }
        }
        return Code.ACTIVATION_FAILURE;
    }
    //退出
    public void logout(String ticket){
//        loginTicketMapper.updateStatus(ticket,1);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket)redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
    }
    public LoginTicket findLoginTicket(String ticket){
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    public User findByEmail(String email){
        return userMapper.selectByEmail(email);
    }

    public int addUser(User user){
        return userMapper.insertUser(user);
    }

    public int saveHeader(int id,String header){
//        return userMapper.updateHeader(id,header);
        int rows = userMapper.updateHeader(id, header);
        clearCache(id);
        return rows;
    }

    public int saveStatus(int id,int status){
        int rows =  userMapper.updateStatus(id,status);
        clearCache(id);
        return rows;
    }

    public int savePassword(int id,String password){
        int rows = userMapper.updatePassword(id,password);
        clearCache(id);
        return rows;
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId){
        User user = userMapper.selectById(userId);

        ArrayList<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return "admin";
                    case 2:
                        return "moderator";
                    default:
                        return "user";
                }
            }
        });
        return list;
    }

}
