package com.oy.oypicturebackend.model.dto.space;

import com.oy.oypicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间查询请求
 */
@EqualsAndHashCode(callSuper = true)//让 SpaceQueryRequestDTO 比较对象时，既看自身属性也包含父类 PageRequest 的分页属性，确保不同查询条件（包括分页参数）能被正确区分
@Data
public class SpaceQueryRequestDTO extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间类型： 0-私有 1-团队
     */
    private Integer spaceType;


    private static final long serialVersionUID = 1L;
}
