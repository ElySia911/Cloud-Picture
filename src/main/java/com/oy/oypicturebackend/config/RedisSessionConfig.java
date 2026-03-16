package com.oy.oypicturebackend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration // 配置类，加上才能使自定义的序列化器生效
public class RedisSessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        //ObjectMapper是Jackson核心类，负责对象转JSON、JSON转对象
        ObjectMapper objectMapper = new ObjectMapper();

        //开启类型信息功能，例如把一个User对象存进Redis，序列化时会额外存上User的类名，反序列化时能准确还原成User对象。NON_FINAL表示只给非 final 类加类型信息
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        //注册JavaTimeModule，Jackson默认不支持Java 8的时间类型(如LocalDateTime LocalDate) 注册后能正常序列化或反序列化这些时间类型
        objectMapper.registerModule(new JavaTimeModule());

        //禁用把日期转换成时间戳功能，让日期以字符串形式存储
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        //GenericJackson2JsonRedisSerializer是Spring提供的基于Jackson的Redis序列化器，作用是把Java对象转成JSON格式存入Redis，比默认的JDK列化器更易读
        return new GenericJackson2JsonRedisSerializer(objectMapper);

    }

}
