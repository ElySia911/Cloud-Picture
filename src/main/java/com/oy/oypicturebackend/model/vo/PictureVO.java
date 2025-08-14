package com.oy.oypicturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.oy.oypicturebackend.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

//上传成功后返回给前端的响应类，这是一个视图包装类，额外关联上了传图片的用户信息
@Data
public class PictureVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图Url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 分类
     */
    private String category;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 关联上用户信息（脱敏）
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    //为了让实体类Picture和视图类PictureVO之间更好转换，编写如下两个方法

    /**
     * VO转实体
     *
     * @param pictureVO
     * @return
     */
    public static Picture voToObj(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);//属性拷贝，拷贝字段名和类型一致的字段

        //类型不同需要转换，tags在VO中是List<String>即对象，而在Picture中是用JSON数组存储，所以使用toJsonStr方法实现从对象转换成JSON数组，即序列化
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    /**
     * 实体转VO
     * 将数据库实体类Picture转为前端展示的视图对象PictureVO，负责字段复制和特殊格式处理
     *
     * @param picture
     * @return
     */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);

        //用 JSONUtil.toList() 将 JSON数组反序列化为 List<String>
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;

    }
}
