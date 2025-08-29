package com.oy.oypicturebackend.manager;


import cn.hutool.core.io.FileUtil;
import com.oy.oypicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;


/*
Manager是人为约定的一种写法，表示通用的，可复用的能力，可供其他代码（比如service）调用
这个类跟业务逻辑没有一点关系，专注于提供与腾讯云COS对象存储的基础交互能力（如上传，下载，带图片解析的上传）
这个类引入对象存储配置和COS客户端，用于和COS进行交互
*/
@Component
@Slf4j
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
     * 通用的上传并解析图片的方法，存储了一张原图和webp图和缩略图
     *
     * @param key  key就是文件在COS中的唯一地址，就是上传文件在Bucket中的”路径“+”文件名“
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        //创建一个上传对象请求 使用配置好的Bucket名称，key，file创建一个上传请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        //要想对图片进行处理（获取基本信息也被视作一种图片处理），需要创建"图片处理操作对象"
        PicOperations picOperations = new PicOperations();
        //1表示返回原图信息
        picOperations.setIsPicInfo(1);
        //创建一个空的图片处理规则列表，用来存放规则
        List<PicOperations.Rule> rules = new ArrayList<>();

        //取出key的主文件名（去掉后缀）加上.webp
        String webpKey = FileUtil.mainName(key) + ".webp";

        //1.创建一条规则，这条规则是将图片格式转成webp的
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setBucket(cosClientConfig.getBucket());//桶
        compressRule.setFileId(webpKey);//表示处理后保存成什么文件名（持久化）
        compressRule.setRule("imageMogr2/format/webp");//腾讯COS的图片处理DSL，表示转为webp格式

        rules.add(compressRule);//将这一条规则添加到列表中

        //2.创建另一条规则，这条规则是将图片进行缩略处理，仅对>20KB的图片进行缩略
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);//COS中缩略图的路径
            thumbnailRule.setBucket(cosClientConfig.getBucket());//桶
            thumbnailRule.setFileId(thumbnailKey);
            //缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理，即原图尺寸比目标尺寸小，就不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));//规则是官方规定这样写的，生成的缩略图最大不会超过128*128

            rules.add(thumbnailRule);//将这一条规则添加到列表
        }

        picOperations.setRules(rules);//将规则列表设置给图片处理操作对象
        putObjectRequest.setPicOperations(picOperations);//把图片处理指令通过上传请求发给COS
        return cosClient.putObject(putObjectRequest);//执行上传，对象存储会生成三张图片
    }


    /**
     * 删除对象
     *
     * @param key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }


    /**
     * 旧的图片获取图片主色调
     *
     * @param key
     * @return
     */
   /* public String getImageMainColor(String key) {
        try {
            //使用占位符%s格式化字符串 分别是桶名 地域 对象键
            String url = String.format("https://%s.cos.%s.myqcloud.com/%s?imageAve", cosClientConfig.getBucket(), cosClientConfig.getRegion(), key);

            //创建URL对象，打开网络连接
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);//连接超时
            conn.setReadTimeout(10000);//读超时


            try (
                    //用字符流缓冲器包装，提供缓冲
                    BufferedReader reader = new BufferedReader
                            (
                                    //用InputStreamReader将字节流变成字符流，并指定编码为UTF-8
                                    new InputStreamReader
                                            (
                                                    //获取服务器返回的原始内容，字节流
                                                    conn.getInputStream(), StandardCharsets.UTF_8
                                            )
                            )
            ) {

                StringBuilder sb = new StringBuilder();
                String line;
                //从 reader中读取一行文本赋给line，将每一次读取到的内容追加到StringBuilder对象中，直到读取到流的末尾返回null，循环终止
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                //sb.toString()将sb转换为普通字符串，然后通过构造方法将字符串解析为JSONObject对象，obj就代表整个JSON对象
                JSONObject obj = new JSONObject(sb.toString());
                //根据键取值
                String rgb = obj.getStr("RGB");

                //return "#" + rgb.substring(2); // 转换成 #736246
                return rgb;
            }
        } catch (Exception e) {
            log.error("获取主色调失败, key={}", key, e);
        }
        return null;

    }*/
}
