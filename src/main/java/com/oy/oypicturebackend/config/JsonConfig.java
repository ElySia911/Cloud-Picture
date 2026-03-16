package com.oy.oypicturebackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Spring MVC Json 配置，解决前后端交互中Long类型数据精度丢失的问题
 */
@JsonComponent
// 告诉Spring这个类中定义了JSON处理的相关配置，Spring会自动扫描并加载这个配置，无需额外注册，让配置生效。
public class JsonConfig {

    /**
     * 添加 Long 转 json 精度丢失的配置
     */
    @Bean//将方法返回的对象注册到Spring容器中，之后在整个项目中任何地方需要用到jacksonObjectMapper时候，不用手动new，直接从Spring容器中获取，例如通过@Autowired注入。
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {//Jackson2ObjectMapperBuilder是Spring提供的ObjectMapper的构建器，默认配置了大部分常用的JSON转换规则，只需要在它的基础上，定制自己需要的特殊规则（这里是 Long 转 String）
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();//createXmlMapper(false)不启用XML映射功能，只专注于JSON处理，.build()：基于构建器的默认配置，创建一个ObjectMapper实例，这个实例已经具备了常规的JSON转换能力

        // 创建一个规则容器，可以用来容纳多个序列化器规则
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance); //将Long包装类，使用ToStringSerializer.instance，序列化时转为 String 类型。
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);//将Long基本类型，使用ToStringSerializer.instance，序列化时转为 String 类型。
        objectMapper.registerModule(module);//将规则容器注册进objectMapper
        return objectMapper;
    }
}
/*
 * Spring 提供现成的 Jackson2ObjectMapperBuilder 构建器，我们用它创建 ObjectMapper 实例，
 * 再创建 SimpleModule 存放 Long 转 String 的自定义规则，
 * 将规则模块注册进 ObjectMapper 实例，最终 Spring 用这个定制版 ObjectMapper 处理 Java 对象和 JSON 之间的转换。
 *
 * */