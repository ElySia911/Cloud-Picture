package com.oy.oypicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的用于删除请求的类
 */
@Data
public class DeleteRequest implements Serializable {
    private Long id;
    private static final long serialVersionUID = 1L;
}
