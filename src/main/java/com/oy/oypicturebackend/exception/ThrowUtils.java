package com.oy.oypicturebackend.exception;

/**
 * 异常处理工具类，这个工具类的作用是如果发生异常，不用手动去new一个异常来抛出
 * 而是直接使用 ThrowUtils.throwIf() 然后把参数填进去就行
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition        条件
     * @param runtimeException 异常
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message   错误信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        //调用throwIf方法，throw抛出了异常，创建了一个BusinessException对象
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
