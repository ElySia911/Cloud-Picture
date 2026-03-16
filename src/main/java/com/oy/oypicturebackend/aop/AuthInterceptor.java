package com.oy.oypicturebackend.aop;

import com.oy.oypicturebackend.annotation.AuthCheck;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.UserRoleEnum;
import com.oy.oypicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect//声明这是一个切面类
@Component//spring的bean，让spring来管理
public class AuthInterceptor {

    @Resource
    private UserService userService;


    /**
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     * @return
     */
    @Around("@annotation(authCheck)")
    //@Around("@annotation(authCheck)")：定义一个环绕通知（@Around），切入点是所有被 @AuthCheck 注解标记的方法。这意味着当程序执行被 @AuthCheck 标注的方法时，会先进入该拦截器的逻辑
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable { // 参数joinPoint代表被拦截的目标方法，可以通过它执行原方法（joinPoint.proceed()）。参数AuthCheck authCheck：自动注入目标方法上的@AuthCheck注解实例，用于获取注解中配置的mustRole属性值
        String mustRole = authCheck.mustRole();

        //因为@Around注解的方法，参数只能是切入点和注解实例，不能随意添加HttpServletRequest，所以通过请求上下文工具类（RequestContextHolder）取出当前请求对应的上下文属性（RequestAttribute）
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();//转换类型后，通过getRequest拿到HttpServletRequest

        //获取当前登录用户，getLoginUser方法里面进行了是否登录的校验，所以这个自定义注解不仅能校验角色，还能校验是否登录
        User loginUser = userService.getLoginUser(request);

        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);//根据字符串类型的值获取枚举常量，即获取注解中要求的角色
        //如果不需要权限，就放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();//proceed方法表示让方法继续执行
        }
        //以下的代码：必须有权限 ，才通过
        String userRole = loginUser.getUserRole();//获取登录用户的角色身份
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);//根据字符串类型的值获取枚举常量
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //能顺利活到这里，就放行
        return joinPoint.proceed();
    }
}
