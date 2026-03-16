package com.oy.oypicturebackend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration//配置类 spring扫描加载这个类，类中的@Bean方法才有可能被执行，否则spring可能无法识别这个类进而忽略@Bean的方法
@ConfigurationProperties(prefix = "cos.client")//将配置文件中以cos.client为前缀的配置项，自动映射到当前类的属性中
public class CosClientConfig {
    private String host; //域名
    private String secretId;//密钥id
    private String secretKey;//密钥
    private String region; //存储桶所在的地域，如ap-guangzhou
    private String bucket; //存储桶名称

    @Bean//方法返回的对象会交给Spring来管理，即让Spring来管理这个方法创建的对象，想用直接@Autowired注入就能拿到这个对象
    public COSClient cosClient() {

        // 1 根据配置文件的secretId和secretKey初始化身份凭证（COSCredentials）
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);

        // 2 ClientConfig类为配置信息类，类中包含了设置region（地域），https（协议），超时等等的方法
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);


        // 3 整合身份凭证和客户端配置，生成COSClient客户端实例，交由Spring容器管理。
        COSClient cosClient = new COSClient(cred, clientConfig);
        return cosClient;
    }
}
