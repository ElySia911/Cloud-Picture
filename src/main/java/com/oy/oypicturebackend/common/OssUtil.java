package com.oy.oypicturebackend.common;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.oy.oypicturebackend.config.OssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
public class OssUtil {
    @Autowired
    private OssConfig ossConfig;

    public String uploadFile(MultipartFile file) {
        String endpoint = ossConfig.getEndpoint();
        String accessKeyId = ossConfig.getAccessKeyId();
        String accessKeySecret = ossConfig.getAccessKeySecret();
        String bucketName = ossConfig.getBucketName();

        //创建Oss客户端实例，后续可用ossClient调用各种Oss方法
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            //获取上传的文件输入流
            InputStream inputStream = file.getInputStream();
            //构建文件名：文件夹路径+随机名
            String originalFilename = file.getOriginalFilename();
            String fileName = "avatar/" + UUID.randomUUID() + "-" + originalFilename;
            //上传到oss
            ossClient.putObject(bucketName, fileName, inputStream);
            //返回上传后的文件URL
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("上传失败：" + e.getMessage());
        } finally {
            //关闭ossClient
            ossClient.shutdown();
        }
    }
}
