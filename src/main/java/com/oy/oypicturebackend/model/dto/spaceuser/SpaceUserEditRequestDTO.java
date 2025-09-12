package com.oy.oypicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间成员请求DTO，只能编辑成员的角色
 */
@Data
public class SpaceUserEditRequestDTO implements Serializable {
    //记录的id
    private Long id;
    //空间角色：viewer/editor/admin
    private String spaceRole;

    private static final long serialVersionUID = 1L;

}
