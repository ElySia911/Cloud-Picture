package com.oy.oypicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {

    USER("用户", "user"),
    ADMIN("管理员", "admin");


    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举常量
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        /*UserRoleEnum.values()：获取枚举的所有实例（这里是 USER 和 ADMIN）
         * 遍历每个枚举常量，检查其value属性是否与传入的value相等，若匹配，返回对应的枚举常量*/
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }
        /*若枚举常量有成千上万个，用for循环太耗时，改成用Map去查找*/
        /*Map<String,UserRoleEnum> map=new HashMap<>();
        map.put("admin",ADMIN);
        map.put("user",USER);*/

        return null;
    }
}
