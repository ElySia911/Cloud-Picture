package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片审核请求DTO （管理员）
 */
@Data
public class PictureReviewRequestDTO implements Serializable {
    /**
     * 图片id
     */
    private Long id;

    /**
     * 审核状态：0是待审核 1是通过 2是拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    private static final long serialVersionUID = 1L;

}
