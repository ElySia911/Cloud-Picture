package com.oy.oypicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间编辑请求（普通用户，目前仅允许编辑空间名称）
 */
@Data
public class SpaceEditRequestDTO implements Serializable {


    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    private static final long serialVersionUID = 1L;
}
