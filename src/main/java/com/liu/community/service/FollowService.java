package com.liu.community.service;

import com.liu.community.entity.User;
import com.liu.community.util.Code;
import com.liu.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    //关注
    public void follow(int userId, int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {

                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

                redisOperations.multi();
                //作者添加粉丝
                redisOperations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                //粉丝添加关注目标
                redisOperations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());


                return redisOperations.exec();
            }
        });
    }
    //取关
    public void unFollow(int userId, int entityType, int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {

                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

                redisOperations.multi();
                //作者删除粉丝
                redisOperations.opsForZSet().remove(followerKey, userId);
                //粉丝删除关注目标
                redisOperations.opsForZSet().remove(followeeKey, entityId);


                return redisOperations.exec();
            }
        });
    }
    //粉丝数量
    public long followerCount(int entityType, int entityId){
        return redisTemplate.opsForZSet().size(RedisKeyUtil.getFollowerKey(entityType,entityId));
    }
    //关注了数量
    public long followeeCount(int userId, int entityType){
        return redisTemplate.opsForZSet().size(RedisKeyUtil.getFolloweeKey(userId,entityType));
    }
    //是否关注了
    public boolean isFollow(int userId, int entityType, int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId) != null;
    }
    //查询关注的人
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit){
        List<Map<String, Object>> list = new ArrayList<>();
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, Code.ENTITY_TYPE_USER.getCode());
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(followeeKey, offset, offset + limit - 1);
        for (ZSetOperations.TypedTuple objectTypedTuple : set) {
            Map<String, Object> map = new HashMap<>();
            int targetId = (int)objectTypedTuple.getValue();
            User byId = userService.findById(targetId);
            Assert.notNull(byId,"该用户不存在");
            map.put("user",byId);
            long score = objectTypedTuple.getScore().longValue();
            map.put("followTime",new Date(score));

            list.add(map);
        }
        return list;
    }
    //查询粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit){
        List<Map<String, Object>> list = new ArrayList<>();
        String followerKey = RedisKeyUtil.getFollowerKey(Code.ENTITY_TYPE_USER.getCode(),userId);
        Set<ZSetOperations.TypedTuple<Object>> set = redisTemplate.opsForZSet().reverseRangeWithScores(followerKey, offset, offset + limit - 1);
        for (ZSetOperations.TypedTuple objectTypedTuple : set) {
            Map<String, Object> map = new HashMap<>();
            int targetId = (int)objectTypedTuple.getValue();
            User byId = userService.findById(targetId);
            Assert.notNull(byId,"该用户不存在");
            map.put("user",byId);
            long score = objectTypedTuple.getScore().longValue();
            map.put("followTime",new Date(score));

            list.add(map);
        }
        return list;
    }
}
