package com.oy.oypicturebackend.model.dto.picture;

import com.oy.oypicturebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户查询图片请求DTO，继承了公共包中的PageRequest支持分页查询，想根据什么查，就加什么字段就行
 */
@Data
public class PictureQueryRequestDTO extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

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
     * 搜索词（同时搜名称、简介等）
     */
    private String searchText;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 审核状态：0是待审核 1是通过 2是拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    //空间id
    private Long spaceId;

    //是否只查询spaceId为空的数据
    private boolean nullSpaceId;
    /*只靠spaceId一个字段，是无法实现只查询公开图片的，因为不填spaceId，数据库就默认是查询整个图片表，
      填了spaceId，就查询指定空间的图片，无法做到查询公开的图片
      所以需要添加nullSpaceId字段，通过 true/false 来判断是否只查询spaceId为空的数据
      当设置nullSpaceId=true时，SQL是SELECT * FROM image WHERE 1=1 AND space_id IS NULL → 精准查询仅公开的图片
    */

    /**
     * 开始编辑时间
     */
    private Date startEditTime;

    /**
     * 结束编辑时间
     */
    private Date endEditTime;


    private static final long serialVersionUID = 1L;
}
