package com.oy.oypicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间角色枚举
 */
@Getter
public enum SpaceRoleEnum {

    VIEWER("浏览者", "viewer"),

    EDITOR("编辑者", "editor"),

    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     *
     * @param value
     * @return
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceRoleEnum anEnum : SpaceRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(SpaceRoleEnum.values())//values()方法获取枚举数组，然后 转换为流
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
        //这个方法就是将枚举数组中的每个枚举的text属性取出来，然后收集到一个List中
    }

    /**
     * 获取所有枚举的值列表
     *
     * @return
     */
    public static List<String> getAllValues() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
