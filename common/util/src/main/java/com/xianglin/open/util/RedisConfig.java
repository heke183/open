package com.xianglin.open.util;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.session}")
    private String sessionHost;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.password}")
    private String passWord;
//    @Value("${spring.redis.pool.max-idle}")
    private int maxIdl;
//    @Value("${spring.redis.pool.min-idle}")
    private int minIdl;
    @Value("${spring.redis.database}")
    private int database;
//    @Value("${spring.redis.keytimeout}")
    private long keytimeout;
//    @Value("${spring.redis.timeout}")
    private int timeout;
    @Value("${spring.redis.cache}")
    private String cacheHost;

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(sessionHost);
        config.setPort(port);
//        config.setDatabase(database);
        if(!passWord.isEmpty()){
            config.setPassword(RedisPassword.of(passWord));
        }
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(config);

        return jedisConnectionFactory;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory2(){
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(cacheHost);
        config.setPort(port);
//        config.setDatabase(database);
        if(!passWord.isEmpty()){
            config.setPassword(RedisPassword.of(passWord));
        }
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(config);
        return jedisConnectionFactory;
    }

    @Bean(name = "redisSession")
    public RedisTemplate<String, String> redisTemplateObject() throws Exception {
        StringRedisTemplate template = new StringRedisTemplate(redisConnectionFactory());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "redisCache")
    public RedisTemplate<String, String> redisTemplateObject2() throws Exception {
        StringRedisTemplate template = new StringRedisTemplate(redisConnectionFactory2());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;

    }

}
