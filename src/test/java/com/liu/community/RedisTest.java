package com.liu.community;

import com.liu.community.util.AjaxResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testString(){
        String strKey = "test:count";
        redisTemplate.opsForValue().set(strKey, 1);

        System.out.println(redisTemplate.opsForValue().get(strKey));
        System.out.println(redisTemplate.opsForValue().increment(strKey));
        System.out.println(redisTemplate.opsForValue().decrement(strKey));

        String strKey1 = "test:json";
        redisTemplate.opsForValue().set(strKey1, new AjaxResult(1,"msg",new AjaxResult(1,"msg2",null)));

        System.out.println(redisTemplate.opsForValue().get(strKey1));


    }

    @Test
    public void testHash(){
        String hashKey = "test:user0";

        redisTemplate.opsForHash().put(hashKey, "id", 1);
        redisTemplate.opsForHash().put(hashKey, "username", "wangwu");
        redisTemplate.opsForHash().put(hashKey, "age", 18);

        System.out.println(redisTemplate.opsForHash().get(hashKey,"id"));
        System.out.println(redisTemplate.opsForHash().get(hashKey,"username"));
        System.out.println(redisTemplate.opsForHash().get(hashKey,"age"));

        String hashKey1 = "test:user1";
        Map<String, String> map = new HashMap<>();
        map.put("id","1");
        map.put("username","zhaoliu");
        map.put("age","20");
        redisTemplate.opsForHash().putAll(hashKey1,map);

        System.out.println(redisTemplate.opsForHash().get(hashKey1,"id"));
        System.out.println(redisTemplate.opsForHash().get(hashKey1,"username"));
        System.out.println(redisTemplate.opsForHash().get(hashKey1,"age"));

    }

    @Test
    public void testList(){
        String listKey = "test:list";

        redisTemplate.opsForList().leftPushAll(listKey,101,102,103);
        System.out.println(redisTemplate.opsForList().index(listKey,0));
        System.out.println(redisTemplate.opsForList().range(listKey,0,-1));
        System.out.println(redisTemplate.opsForList().size(listKey));

        System.out.println(redisTemplate.opsForList().leftPop(listKey));

    }

    @Test
    public void testSet(){
        String setKey = "test:set";

        redisTemplate.opsForSet().add(setKey, "张三","李四","王五","赵六");
        System.out.println(redisTemplate.opsForSet().pop(setKey));;
        System.out.println(redisTemplate.opsForSet().members(setKey));

    }

    @Test
    public void testZSet(){
        String zSetKey = "computer:language";

        redisTemplate.opsForZSet().add(zSetKey,"java",1000);
        redisTemplate.opsForZSet().add(zSetKey,"c",998);
        redisTemplate.opsForZSet().add(zSetKey,"php",500);
        redisTemplate.opsForZSet().add(zSetKey,"golang",999);

        System.out.println(redisTemplate.opsForZSet().range(zSetKey,0,-1));
        System.out.println(redisTemplate.opsForZSet().reverseRange(zSetKey,0,-1));
        System.out.println(redisTemplate.opsForZSet().reverseRangeByScore(zSetKey,0,999));
        System.out.println(redisTemplate.opsForZSet().reverseRank(zSetKey,"golang"));
        System.out.println(redisTemplate.opsForZSet().score(zSetKey,"golang"));

        System.out.println(redisTemplate.opsForZSet().reverseRangeWithScores(zSetKey,0,2));

    }

    @Test
    public void testKey(){
        redisTemplate.delete("test:someKey");

        System.out.println(redisTemplate.hasKey("test:someKey"));

        redisTemplate.expire("test:someKey",60, TimeUnit.SECONDS);
    }

    //多次访问同一个key,绑定一个key-value，方便持续操作
    @Test
    public void testBoundValueOperationsKey(){
        BoundValueOperations boundValueOperations = redisTemplate.boundValueOps("test:count");

        boundValueOperations.increment(50.1);
        boundValueOperations.expire(10,TimeUnit.SECONDS);
        System.out.println(boundValueOperations.get());
    }

    //编程式事务
    @Test
    public void testTransactional(){
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String key = "test:tx";
                //事务开始
                operations.multi();

                operations.opsForSet().add(key, "zhangsan","lisi","wangwu");
                operations.opsForSet().pop(key);

                //在事务中做查询无效
                System.out.println(operations.opsForSet().members(key));

                //事务结束
                return operations.exec();
            }
        });
        System.out.println(obj);
    }
    /*
    redis高级数据类型：
        1.HyperLogLog 占用资源低 统计超多重复数据的独立数 有小规模误差统计时,可以忽略
        2.BitMap 占用资源低 记录一串连续的以0(false),1(true)为数据的连串数据 例如:签到，打卡
     */
    //统计20万个含有重复数据的独立总数
    @Test
    public void testHyperLogLog(){
        String redisKey = "hyperLogLog:1";
        for (int i = 1;i <= 100000;i++) {//[1, 100000]
            redisTemplate.opsForHyperLogLog().add(redisKey,i);
        }
        for (int i = 1;i <= 100000;i++) {
            int a = (int)(Math.random() * 100000 + 1);//[1, 100001)
            redisTemplate.opsForHyperLogLog().add(redisKey,a);
        }
        //总数据里的独立数据数数量应为100000,实测99533
        Long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }
    //将三组数据合并后，再统一合并生成的重复数据的独立数
    @Test
    public void testHyperLogLog1(){
        String redisKey2 = "hyperLogLog:2";
        for (int i = 1;i <= 10000;i++) {    //[1, 10000]
            redisTemplate.opsForHyperLogLog().add(redisKey2,i);
        }
        String redisKey3 = "hyperLogLog:3";
        for (int i = 5001;i <= 15000;i++) {    //[5001, 15000]
            redisTemplate.opsForHyperLogLog().add(redisKey3,i);
        }
        String redisKey4 = "hyperLogLog:4";
        for (int i = 10001;i <= 20000;i++) {    //[10001, 20000]
            redisTemplate.opsForHyperLogLog().add(redisKey4,i);
        }

        String unionKey = "hyperLogLog:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);

        Long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        //三组数据合并后重复数据的独立数量应为20000个([1,20000]),实测19833
        System.out.println(size);
    }
    //统计一直数据的布尔值
    @Test
    public void testBitMap(){
        String redisKey = "BitMap:1";

        //存值 不设置的均为false
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 3, true);
        redisTemplate.opsForValue().setBit(redisKey, 5, true);
        //查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 5));

        //统计为true的数量
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        // 3
        System.out.println(obj);
    }
    //统计三组数据的布尔值，并对这三组数据做or运算
    //true || false = true
    //true && false = false
    public void testBitMapOperation(){
        String redisKey2 = "BitMap:2";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);

        String redisKey3 = "BitMap:3";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);

        String redisKey4 = "BitMap:4";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);

        String redisKeyOR = "BitMap:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKeyOR.getBytes(),
                        redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes()
                );
                return connection.bitCount(redisKeyOR.getBytes());
            }
        });

        /*
            7
            全为true
            因为: true || false = true  true && false = false
         */
        System.out.println(obj);

        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOR, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOR, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOR, 2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOR, 3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOR, 4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOR, 5));
        System.out.println(redisTemplate.opsForValue().getBit(redisKeyOR, 6));

    }
}
