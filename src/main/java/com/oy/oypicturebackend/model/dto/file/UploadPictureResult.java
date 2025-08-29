package com.oy.oypicturebackend.model.dto.file;

import lombok.Data;

/**
 * 用于接受图片解析信息的包装类
 * 上传图片的结果，调用完上传图片方法之后就会用到这个类
 * 这个类充当数据库实体类和对象存储返回的结果类之间的中间人
 */
@Data
public class UploadPictureResult {

    /**
     * 图片地址
     */
    private String url;

    /**
     * 缩略图Url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;

}
