package com.oy.oypicturebackend.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置Mybatis-Plus的分页插件，让项目支持Mp提供的便捷分页查询功能，无需手动写分页的SQL
 * 支持在Service层使用Page<T>进行分页查询，并自动生成limit语句，无需手动写sql分页逻辑
 */
@Configuration//表示这是配置类，Spring会自动扫描并加载该类中的配置，如@Bean方法定义的组件
@MapperScan("com.oy.oypicturebackend.mapper")//扫描mapper接口所在的包 并为他们创建代理对象（实现类），注入到Spring容器中，无需在每个接口上写@Mapper
public class MyBatisPlusConfig {

    /**
     * 拦截器配置
     *
     * @return {@link MybatisPlusInterceptor}
     */
    @Bean
//将方法返回的对象注册到Spring容器中，之后在整个项目中任何地方需要用到MybatisPlusInterceptor时候，不用手动new，直接从Spring容器中获取，例如通过@Autowired注入。若没有@Bean注册，框架就找不到这个对象，分页功能就失效
    public MybatisPlusInterceptor mybatisPlusInterceptor() {

        // MybatisPlusInterceptor是MyBatis-Plus提供的一个拦截器容器，所有MyBatis-Plus的插件（分页、乐观锁、防全表更新等），都需要添加到这个容器中才能生效
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件的具体实现，指定数据库类型为MySql，确保分页插件生成的分页SQL符合对应的数据库语法。将其添加到MybatisPlusInterceptor后，MyBatis-Plus会自动拦截分页查询SQL，实现分页功能
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;//把配置好的拦截器返回出去，供spring使用
    }
}

//MyBatis原生不支持分页，需要手动写LIMIT ? offset ? 或使用复杂的分页插件。而MyBatis-Plus的PaginationInnerInterceptor可以自动处理分页逻辑
//当使用MyBatis-Plus提供的IPage接口进行查询时，（如 page(new Page<>(pageNum, pageSize), queryWrapper)）
//插件会自动在 SQL 后拼接分页条件（如 LIMIT），并计算总条数，简化分页代码的编写
//这样做的好处是整个项目中只有一个MybatisPlusInterceptor 实例，避免重复创建
//框架（如 MyBatis-Plus）内部也能自动从 Spring 容器中找到这个对象，从而启用分页等功能
//本质上，这是 Spring 核心的 “控制反转（IOC）” 思想的体现 —— 对象的创建和管理权从开发者手中转移到了 Spring 容器
//未来如果需要添加其他功能，如乐观锁OptimisticLockerInnerInterceptor/防注入 BlockAttackInnerInterceptor,只需继续调用 addInnerInterceptor 即可，无需修改现有逻辑