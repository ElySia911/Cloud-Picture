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
//Around表示这个切面在方法执行前后都插入逻辑，切入点表达式是拦截所有被@AuthCheck注解标记的方法，并且能拿到注解本身authCheck对象，用于获取注解参数
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();

        //因为AOP不在controller中，所有不能直接@RequestParam或@RequestBody拿请求，所以通过RequestContextHolder（可以理解成请求上下文）获取当前线程绑定的请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();//这样就拿到了HttpServletRequest

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
