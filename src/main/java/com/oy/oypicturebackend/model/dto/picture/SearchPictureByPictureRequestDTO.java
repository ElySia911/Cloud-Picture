package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以图搜图请求DTO
 */
@Data
public class SearchPictureByPictureRequestDTO implements Serializable {

    /**
     * 图片id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
