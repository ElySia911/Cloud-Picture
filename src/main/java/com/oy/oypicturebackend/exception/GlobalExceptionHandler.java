package com.oy.oypicturebackend.exception;

import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//全局异常处理类，统一捕获项目中抛出的业务异常BusinessException和运行异常RuntimeException
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("业务异常：", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> businessExceptionHandler(RuntimeException e) {
        log.error("运行异常：", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}
