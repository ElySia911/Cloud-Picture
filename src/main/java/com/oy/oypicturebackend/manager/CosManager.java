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
这个类跟业务逻辑没有一点关系，专注于提供与腾讯云COS对象存储的基础交互能力（如上传文件，下载文件，带图片解析的上传）
这个类引入对象存储配置和COS客户端，用于和COS进行交互
*/
@Component
@Slf4j
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;
    //理论上通过cosClientConfig.cosClient()获取客户端也可以，但这样每次调用这个方法都会重新创建一个新的COSClient对象（因为它只是一个普通方法，不是 Spring 管理的单例）
    //而@Bean默认创建的是单例对象（整个应用中只有一个实例），直接注入COSClient能保证每次用的都是同一个对象，避免频繁创建 / 销毁客户端导致的资源浪费
    @Resource
    private COSClient cosClient;

    /**
     * 上传对象，例如图片 音乐 视频都可以
     *
     * @param key  唯一键 key，key由文件路径 + 文件名组成
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        //创建上传请求对象，需要三个参数：存储桶名称、文件在COS的唯一标识key（路径+名称）、传入的本地待上传文件
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //调用COS客户端执行putObject执行上传
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 下载对象，例如图片 音乐 视频都可以
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        //创建下载请求对象，
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);//去我配置的bucket里面，找到例如：key="xxx.jpg"的文件
        //调用COS客户端执行getObject执行下载
        return cosClient.getObject(getObjectRequest);//以流的方式返回
    }

//----------------------------------------------------------------------------------------------------------------

    /**
     * 这是一个上传图片到COS并解析图片通用的方法，上传原图的同时，生成了webp格式的压缩图和缩略图
     * 这个方法是底层工具类方法 ，专注于和COS的交互，提供给业务层调用
     *
     * @param key  key就是文件在COS中的唯一地址，就是上传文件在Bucket中的”路径“+”文件名“
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        //创建一个上传对象请求 使用配置好的Bucket名称，key，file创建一个上传请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        //PicOperations类用于记录图像操作，主要的成员有：isPicInfo、rules
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);//1表示返回原图信息
        List<PicOperations.Rule> rules = new ArrayList<>(); //创建一个空的图片处理规则列表，用来存放规则（可以同时定义多个规则，COS 会按顺序执行）

        //取出key的主文件名（去掉后缀）加上.webp      若key是"image/photo.jpg"，则主文件名为"image/photo"
        String webpKey = FileUtil.mainName(key) + ".webp";

        //1.先创建一条规则，这条规则是将图片原来的格式转成webp格式
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setBucket(cosClientConfig.getBucket());//处理后的图片存储到哪个桶
        compressRule.setFileId(webpKey);//表示处理后保存成什么文件名（持久化），指定转换后的文件以该键存储
        compressRule.setRule("imageMogr2/format/webp");//imageMogr2是腾讯云的图片处理接口，format/webp表示将图片格式转为webp
        rules.add(compressRule);//将这一条规则添加到规则列表中

        //2.再创建另一条规则，这条规则是将图片进行缩略处理，仅对>20KB的图片进行缩略，因为通过测试发现，比较小的图片，若是也进行缩略处理，得到的缩略后的图片的大小会比原图的大小还要大
        if (file.length() > 20 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);//COS中缩略图的路径，例如原图avatar.png，缩略图为avatar_thumbnail.png（保留原图后缀，仅文件名加_thumbnail）
            thumbnailRule.setBucket(cosClientConfig.getBucket());//处理后的图片存储到哪个桶
            thumbnailRule.setFileId(thumbnailKey);//表示处理后保存成什么文件名（持久化）
            //缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理，即原图尺寸比目标尺寸小，就不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 128, 128));//规则是官方规定这样写的，生成的缩略图最大不会超过128*128
            rules.add(thumbnailRule);//将这一条规则添加到列表
        }

        picOperations.setRules(rules);//将规则列表设置给图片处理配置类
        putObjectRequest.setPicOperations(picOperations);//把图片处理配置类通过上传请求发给COS
        return cosClient.putObject(putObjectRequest);//执行上传，对象存储会生成三张图片，同时返回上传结果（包含原图和处理后的图片信息）
    }

//----------------------------------------------------------------------------------------------------------------

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
