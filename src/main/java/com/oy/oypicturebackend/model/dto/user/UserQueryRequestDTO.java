package com.oy.oypicturebackend.model.dto.user;

import com.oy.oypicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求DTO，可以通过以下字段来查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequestDTO extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String userName;

    private String userAccount;

    private String userProfile;//简介

    private String userRole;


}
