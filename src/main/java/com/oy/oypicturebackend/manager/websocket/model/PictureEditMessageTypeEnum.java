package com.oy.oypicturebackend.manager.websocket.model;

import lombok.Getter;

/**
 * 图片编辑有关的 消息类型枚举，用于后续根据消息类型进行相应的处理
 */
@Getter
public enum PictureEditMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    ERROR("发送错误", "ERROR"),
    SYNC_STATE("同步当前状态", "SYNC_STATE");


    private final String text;
    private final String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据枚举值获取枚举类型 例如 "INFO" -> PictureEditMessageTypeEnum.INFO
     */
    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (PictureEditMessageTypeEnum type : PictureEditMessageTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
