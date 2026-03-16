package com.oy.oypicturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")////将配置文件中以aliyun.oss为前缀的配置项，自动映射到当前类的属性中
public class OssClientConfig {

    private String endpoint;//地域
    private String accessKeyId;//oss的身份标识
    private String accessKeySecret;//oss的身份密钥
    private String bucketName;//存储桶
    private String avatarPath;//路径

    @PostConstruct
    public void init() {

    }
}
