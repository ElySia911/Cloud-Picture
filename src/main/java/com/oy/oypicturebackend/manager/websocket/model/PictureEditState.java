package com.oy.oypicturebackend.manager.websocket.model;

import lombok.Data;

@Data
public class PictureEditState {
    private int rotation = 0;//旋转角度
    private int scale = 0;//缩放次数
}
