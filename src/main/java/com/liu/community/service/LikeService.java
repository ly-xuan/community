package com.liu.community.service;

import com.liu.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    //点赞 userId:点赞用户 entityUserId:作者
    public void like(int userId, int entityType, int entityId, int entityUserId) {

//        String key = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
//        Boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
//        if (isMember){
//            redisTemplate.opsForSet().remove(key, userId);
//        } else {
//            redisTemplate.opsForSet().add(key,userId);
//        }

        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {

                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);

                redisOperations.multi();

                if (isMember){
                    redisOperations.opsForSet().remove(entityLikeKey, userId);
                    redisOperations.opsForValue().decrement(userLikeKey);
                } else {
                    redisTemplate.opsForSet().add(entityLikeKey,userId);
                    redisOperations.opsForValue().increment(userLikeKey);
                }

                return redisOperations.exec();
            }
        });

    }
    //查询实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId){
        String key = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().size(key);
    }
    //查询某人对某实体的点赞
    public int findEntityLikeStatus(int userId, int entityType, int entityId){
        String key = RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().isMember(key,userId)? 1:0;
    }
    //查询某个用户获得的赞
    public int findUserLikeCount(int entityUserId){
        Integer count = (Integer) redisTemplate.opsForValue().get(RedisKeyUtil.getUserLikeKey(entityUserId));
        return count==null?0:count.intValue();
    }

}
