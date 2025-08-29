package com.oy.oypicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间级别枚举，定义每个级别的空间对应的限额
 */
@Getter
public enum SpaceLevelEnum {

    //普通版100MB 专业版1GB 旗舰版10GB
    COMMON("普通版", 0, 100, 100L * 1024 * 1024),

    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),

    FLAGSHIP("旗舰版", 2, 10000, 10000L * 1024 * 1024);

    private final String text;

    private final int value;

    private final long maxCount;//空间图片的最大数量（空间最多可以存多少张图片）

    private final long maxSize;//空间图片的最大总大小（空间总容量）


    /**
     * @param text     文本
     * @param value    值
     * @param maxCount 空间图片的最大数量（空间最多可以存多少张图片）
     * @param maxSize  空间图片的最大总大小（空间总容量）
     */
    SpaceLevelEnum(String text, int value, long maxCount, long maxSize) {
        this.text = text;
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 根据value获取枚举
     */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }

        /*SpaceLevelEnum.values()是枚举类自带的方法，会返回一个包含所有枚举实例的数组，比如当前定义了三个实例，
          它就返回[COMMON，PROFESSIONAL，FLAGSHIP]，遍历数组中每个实例
          逐个判断，当前实例的value是否和传入的参数value（外部传入的，比如查询时spaceLevel=0）相等
          一旦找到value相等的枚举实例，就直接返回它，否则返回null*/
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (spaceLevelEnum.value == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
