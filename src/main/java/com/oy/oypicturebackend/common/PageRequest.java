package com.oy.oypicturebackend.common;

import lombok.Data;

//通用的分页请求的类
@Data
public class PageRequest {


    private int current = 1;//当前页号
    private int pageSize = 10;//每页记录数
    private String sortField;//排序字段，按哪个排序
    private String sortOrder = "descend";//排序顺序，按什么规则排序，默认升序
}
