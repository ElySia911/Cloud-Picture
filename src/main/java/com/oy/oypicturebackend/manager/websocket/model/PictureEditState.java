package com.oy.oypicturebackend.manager.websocket.model;

import lombok.Data;

@Data
public class PictureEditState {
    private int rotation;//旋转角度
    private double scale;//缩放比例
}
