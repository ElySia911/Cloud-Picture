package com.oy.oypicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//意思就是这个自定义注解的生效范围是针对方法的
@Retention(RetentionPolicy.RUNTIME)//指定这个注解什么时候生效，一般都是运行时
public @interface AuthCheck {

    //自定义注解要接收的参数
    //表示必须具有某个角色
    String mustRole() default "";


    /* 含义就是这个方法只允许管理员角色（admin） 执行。
    @AuthCheck(mustRole = "admin")
    public void deleteUser() {
    ...
    }
    */


}


