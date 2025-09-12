package com.oy.oypicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//意思就是这个自定义注解的生效范围是针对方法的
@Retention(RetentionPolicy.RUNTIME)//这个注解在程序运行时仍存在
public @interface AuthCheck {

    //自定义注解要接收的参数，用于指定访问被标注方法必须具备的角色，默认值为空，即如果使用注解不指定mustRole，则默认不需要特定角色
    String mustRole() default "";

}


