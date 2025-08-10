package com.oy.oypicturebackend.manager;

import com.oy.oypicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/*
Manager是人为约定的一种写法，表示通用的，可复用的能力，可供其他代码（比如service）调用
这个类跟业务逻辑没有一点关系，专注于提供与腾讯云COS对象存储的基础交互能力（如上传，下载，带图片解析的上传）
这个类引入对象存储配置和COS客户端，用于和COS进行交互
*/
@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键 key，key由文件路径 + 文件名组成
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        //new一个上传对象请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        //调用COS客户端执行putObject执行上传
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        //new一个下载对象请求
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);//去我配置的bucket里面，找到例如：key="xxx.jpg"的文件
        //调用COS客户端执行getObject执行下载
        return cosClient.getObject(getObjectRequest);//以流的方式返回
    }

    /**
     * 通用的上传并解析图片的方法
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        //创建一个上传对象请求 参数分别是 存储桶名，key：由路径加文件名组成 file：具体要上传的文件对象
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        //对图片进行处理（获取基本信息也被视作一种图片处理）
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);//1表示返回原图信息

        putObjectRequest.setPicOperations(picOperations);//把图片处理指令通过上传请求发给COS
        return cosClient.putObject(putObjectRequest);
    }
}
