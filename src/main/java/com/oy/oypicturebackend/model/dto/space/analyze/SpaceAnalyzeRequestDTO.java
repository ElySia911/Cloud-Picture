package com.oy.oypicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的空间分析请求DTO，用于给每个具体条件的查询请求进行继承
 */
@Data
public class SpaceAnalyzeRequestDTO implements Serializable {
    //空间id
    private Long spaceId;
    //是否查询公共图库
    private boolean queryPublic;
    //全空间分析
    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}
