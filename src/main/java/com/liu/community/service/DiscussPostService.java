package com.liu.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.liu.community.dao.DiscussPostMapper;
import com.liu.community.entity.DiscussPost;
import com.liu.community.util.SensitiveFilter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.maxSize}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    //  Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache
    @PostConstruct
    public void init(){
        //初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        Assert.isTrue(!StringUtils.isEmpty(key),"参数错误！");
                        String[] params = key.split(":");
                        Assert.isTrue(params.length==2,"参数错误！");
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        //这里可以用redis 二级缓存：coffee -> redis -> mysql

                        log.debug("load post list from DB to Cache");
                        return discussPostMapper.selectDiscussPosts(0,offset,limit,true);
                    }
                });
        //初始化帖子列表缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        log.debug("load post rows from DB to Cache");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }
    //帖子列表的缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    //帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, boolean isByScore){
        if ((userId == 0 && isByScore)){
            return postListCache.get(offset + ":" +limit);
        }

        log.debug("load post list from DB");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, isByScore);
    }

    public int findDiscussPostRows(int userId){
        if ((userId == 0)){
            return postRowsCache.get(userId);
        }

        log.debug("load post rows from DB");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int addDiscussPost(DiscussPost post){
        if (post==null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        //转义HTML标记
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //过滤敏感词
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));

        return discussPostMapper.insertDiscussPost(post);

    }

    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount);
    }
    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type);
    }
    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id, status);
    }
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
