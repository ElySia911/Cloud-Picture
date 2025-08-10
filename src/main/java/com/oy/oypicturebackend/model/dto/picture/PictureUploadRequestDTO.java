package com.oy.oypicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

//由于图片需要支持重复上传（基础信息不变，只改变图片文件），所以要添加图片 id 参数
//这个类叫做 图片上传请求DTO
@Data
public class PictureUploadRequestDTO implements Serializable {


    private static final long serialVersionUID = 1L;


    /**
     * 图片id（用于修改）
     */
    private Long id;
    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 图片名称（用于在批量抓取并上传的时候可以自定义抓取到的图片名称前缀）
     */
    private String picName;


}
