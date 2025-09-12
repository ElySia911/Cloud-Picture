package com.oy.oypicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@EnableAsync//开启异步支持
@MapperScan("com.oy.oypicturebackend.mapper")//自动扫描指定包下的Mapper接口并将它们注册为Spring的Bean，这样就不用每个接口都加@Mapper注解
@EnableAspectJAutoProxy(exposeProxy = true)//让项目中的类在自己内部调用自己的方法时，AOP注解仍能生效，如@Transactional
public class OyPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OyPictureBackendApplication.class, args);
    }

}
