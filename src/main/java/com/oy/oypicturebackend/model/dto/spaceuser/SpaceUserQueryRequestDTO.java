package com.oy.oypicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间用户查询请求DTO
 */
@Data
public class SpaceUserQueryRequestDTO implements Serializable {

    private Long id;
    /**
     * 空间id
     */
    private Long spaceId;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 空间角色
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;


}
