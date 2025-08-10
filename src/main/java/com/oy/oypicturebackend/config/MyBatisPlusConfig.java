package com.oy.oypicturebackend.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为项目配置Mybatis-Plus分页插件功能，从而在服务中使用分页查询变得简单
 * 支持在Service层使用Page<T>进行分页查询，并自动生成limit语句，无需手动写sql分页逻辑
 */
@Configuration//表示这是配置类，Spring boot启动时会自动识别并加载
@MapperScan("com.oy.oypicturebackend.mapper")//扫描mapper接口所在的包 所有@Mapper接口都会自动注册为Spring bean，无需在每个接口上写@Mapper
public class MyBatisPlusConfig {

    /**
     * 拦截器配置
     *
     * @return {@link MybatisPlusInterceptor}
     */
    @Bean//将方法返回的对象交给Spring管理（注册成一个bean）后面自动注入使用
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        //指定当前数据类型为mysql
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;//把配置好的拦截器返回出去，供spring使用
    }
}
