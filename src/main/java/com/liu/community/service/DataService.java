package com.liu.community.service;

import com.liu.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    //将指定ip计入uv
    public void recordUv(String ip){
        String uvKey = RedisKeyUtil.getUvKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey, ip);
    }
    //统计指定日期范围内的Uv
    public long calculateUv(Date start, Date end){
        //参数为true，否者报错
        Assert.isTrue(start!=null && end != null,"参数范围不能为空");

        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) { //不晚于，早于或等于
            String key = RedisKeyUtil.getUvKey(df.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE,1);
        }

        String unionKey = RedisKeyUtil.getUvKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(unionKey, keyList.toArray());

        return redisTemplate.opsForHyperLogLog().size(unionKey);
    }

    //将指定id计入dau
    public void recordDau(int userId){
        String dauKey = RedisKeyUtil.getDauKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(dauKey, userId, true);
    }
    //统计指定日期范围内的Uv
    public long calculateDau(Date start, Date end){
        Assert.isTrue(start != null && end != null,"参数范围不能为空");

        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            byte[] key = RedisKeyUtil.getDauKey(df.format(calendar.getTime())).getBytes();
            keyList.add(key);
            calendar.add(Calendar.DATE,1);
        }

        //进行or运算
        return  (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String unionKey = RedisKeyUtil.getDauKey(df.format(start), df.format(end));
                connection.bitOp(
                        RedisStringCommands.BitOperation.OR,
                        unionKey.getBytes(),
                        keyList.toArray(new byte[0][0])
                );
                return connection.bitCount(unionKey.getBytes());
            }
        });

    }

}
