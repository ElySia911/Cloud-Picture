package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureLikeRequestDTO implements Serializable {
    /**
     * 图片id
     */
    private Long pictureId;

    /**
     * 图片名
     */
    private String pictureName;
}
