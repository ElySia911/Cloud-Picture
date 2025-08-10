package com.oy.oypicturebackend.exception;

import lombok.Getter;

/**
 * 自定义业务异常，用于抛出带有错误码的运行时异常
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造方法，传入错误码和错误信息
     *
     * @param code
     * @param message
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造方法，传入ErrorCode枚举对象
     *
     * @param errorCode
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造方法，传入ErrorCode枚举对象和自定义提示信息
     *
     * @param errorCode
     * @param message
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
