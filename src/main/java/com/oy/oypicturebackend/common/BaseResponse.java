package com.oy.oypicturebackend.common;

import com.oy.oypicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

//统一响应结果类

@Data
public class BaseResponse<T> implements Serializable {

    private int code;//响应码

    private T data;//这里用泛型是因为不同的接口返回给前端的数据类型是不一样的，是模糊的，所以具体的类型由是使用的时候决定

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }


    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
