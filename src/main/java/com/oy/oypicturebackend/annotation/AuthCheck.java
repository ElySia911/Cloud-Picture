package com.oy.oypicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//意思就是该注解只能修饰方法，即只能标注在类的方法上
@Retention(RetentionPolicy.RUNTIME)//该注解在运行时仍然保留，意味程序可以在运行时通过反射获取该注解信息
public @interface AuthCheck {

    //自定义注解的属性，后续使用自定义注解时，通过这个属性指定角色，默认是空字符串，使用注解时不指定mustRole，就默认不需要特定角色
    String mustRole() default "";

}


