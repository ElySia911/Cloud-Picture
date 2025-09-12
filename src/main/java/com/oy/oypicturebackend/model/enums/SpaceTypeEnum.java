package com.oy.oypicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间类型枚举，有私人空间 和 团队空间
 */
@Getter
public enum SpaceTypeEnum {

    PRIVATE("私有空间", 0),
    TEAM("团队空间", 1);

    private final String text;

    private final int value;

    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        /*values()是枚举类自带的方法，会返回一个包含所有枚举实例的数组，比如当前定义了两个实例，就返回[PRIVATE，TEAM]，遍历数组中每个实例
          逐个判断，当前实例的value是否和传入的参数value（外部传入的，比如查询时spaceType=0）相等
          一旦找到value相等的枚举实例，就直接返回它，否则返回null*/
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}

