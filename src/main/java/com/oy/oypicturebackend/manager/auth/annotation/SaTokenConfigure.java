package com.oy.oypicturebackend.manager.auth.annotation;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;

/**
 * Sa-Token 开启注解和配置，要想使用注解鉴权，就必须手动将Sa-Token的拦截器注册到Spring MVC中
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {//实现WebMvcConfigurer接口，这是Spring MVC提供的配置接口，通过他可以配置拦截器等

    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 往Spring MVC里面注册一个 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
        //new SaInterceptor():创建Sa-Token框架的拦截器实例
        //.addPathPatterns("/**"):表示对项目中所有接口路径（/**代表所有路径）都生效
        //简单来说，没有这句代码的话，在接口上写的Sa-Token注解不会起作用
    }


    //增强注解识别能力
    @PostConstruct//在Spring创建好这个配置类对象之后，立刻执行这个方法
    public void rewriteSaStrategy() {
        // 重写Sa-Token的注解处理器，增加注解合并功能 
        SaAnnotationStrategy.instance.getAnnotation = (element, annotationClass) -> {
            return AnnotatedElementUtils.getMergedAnnotation(element, annotationClass);
        };
    }

    /*
        原来Sa-Token默认只能识别自己的官方注解，即写在接口上的注解（比如接口上直接标官方的 @SaCheckLogin），但对于包含了官方注解的自定义注解识别能力有限
        重写之后，能识别组合注解：比如自定义的一个 @SaSpaceCheckPermission注解 ，然后在@SaSpaceCheckPermission里面包含@SaCheckLogin,这时Sa-Token也能识别出这是需要检查登录的
        AnnotatedElementUtils.getMergedAnnotation是 Spring 提供的工具，专门用来处理这种 "注解套注解" 的情况， 能 "穿透" 自定义注解，识别到里面包含的官方注解

     */


}
