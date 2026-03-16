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
        // 注册Sa-Token的拦截器（SaInterceptor）到Spring容器，指定拦截范围：/** 代表拦截项目中所有的请求
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }


    //增强注解识别能力
    @PostConstruct//Spring初始化当前类后立即执行这个方法
    public void rewriteSaStrategy() {
        // 重写Sa-Token的注解处理器，增加注解合并功能 
        SaAnnotationStrategy.instance.getAnnotation = (element, annotationClass) -> {
            //element：要解析的目标（比如方法、类）  annotationClass：要解析的注解类型

            //用Spring的AnnotatedElementUtils.getMergedAnnotation替换原生逻辑
            //不仅找当前element上的注解，还会找到继承/组合的注解，实现注解合并
            return AnnotatedElementUtils.getMergedAnnotation(element, annotationClass);
        };
    }

    /*SaToken原生的注解解析能力有限，只能解析当前类或当前方法直接标注的注解，无法识别继承/组合的注解
    例如：标注在父类方法上的注解，子类继承后调用父类方法，注解不生效 或者 自定义一个组合注解，原生的SaToken解析不到自定义注解里面嵌套的Satoken注解
    所以重写getAnnotation方法，用spring的getMergedAnnotation替换掉satoken原生的解析注解的逻辑*/
}
