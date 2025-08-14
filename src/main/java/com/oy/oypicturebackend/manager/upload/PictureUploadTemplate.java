package com.oy.oypicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.oy.oypicturebackend.config.CosClientConfig;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.manager.CosManager;
import com.oy.oypicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;


/**
 * 图片上传模板 定义统一的上传流程
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource//注入工具类，是为了从CosClientConfig拿配置信息
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片模板方法，返回包含业务信息的结果
     *
     * @param inputSource      输入源，这个项目的输入源要不就是本地图片，要不就是字符串（远程URL）
     * @param uploadPathPrefix 上传路径前缀，即key的路径部分，key由路径+文件名组成
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //1.校验
        validPicture(inputSource);

        //2.构造上传路径
        String uuid = RandomUtil.randomString(16);//生成16位随机字符串作为uuid，充当文件名的其中一部分
        String originalFilename = getOriginalFilename(inputSource);//获取前端上传文件的原始文件名，获取到的原始文件名是带后缀的，例如 image.jpg
        //拼唯一文件名：格式为“当前日期_uuid.文件后缀”，确保文件名不重复
        String uploadFileName = String.format(//format方法是字符串格式化方法，可以用占位符%s %d等把多个变量拼接成一个有格式的字符串
                "%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));//获取原始文件的后缀
        //再拼COS的Key ：格式为“/路径前缀/唯一文件名”
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;
        try {
            //3创建临时文件，用上传地址作为文件名前缀，后缀为null
            file = File.createTempFile(uploadFilePath, null);
            // processFile(...) 把输入源落地到临时文件
            processFile(inputSource, file);
            //4.调用CosManager的putPictureObject方法上传文件到COS，返回上传结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);

            //5.从上传结果中获取图片信息对象（包含宽、高、格式等信息）
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            //6.从上传结果中获取COS对图片转换格式后和进行缩略后的持久化处理结果对象processResults
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();//从processResults中getObjectList字段，这里面包含了图片处理后的信息
            if (CollUtil.isNotEmpty(objectList)) {
                //1.不为空就获取转换格式后的图片信息
                CIObject compressCiObject = objectList.get(0);
                CIObject thumbnailCiObject=compressCiObject;//先让缩略图对象thumbnailCiObject默认等于转格式后的图片信息（此时还不确定图片是否进行了缩略操作）
                //若大于1就代表图片在COS进行了缩略操作
                if (objectList.size() > 1) {
                    //2.将缩略后的图片信息取出来赋给thumbnailCiObject
                     thumbnailCiObject = objectList.get(1);
                }

                //封装压缩图返回结果，参数是原始文件名，转换格式后的图片信息，缩略后的图片信息
                return buildResult(originalFilename, compressCiObject, thumbnailCiObject);
            }
            return buildResult(originalFilename, file, uploadFilePath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到COS对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //6.清理临时文件
            this.deleteTempFile(file);
        }

    }

    /**
     * 校验输入源（本地文件或URL） （被子类实现）
     *
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原文件名 （被子类实现）
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件 （被子类实现）
     *
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;


    /**
     * 将图片上传后返回的信息封装起来进行返回
     *
     * @param originFilename
     * @param file
     * @param uploadPath
     * @param imageInfo      图片上传给腾讯对象存储后返回的图片信息
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //提取图片宽度
        int picWidth = imageInfo.getWidth();
        //提取图片高度
        int picHeight = imageInfo.getHeight();
        //计算图片宽高比，保留两位小数
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        //设置图片名称，mainName是将原始文件名的后缀去掉后返回
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        //设置图片宽度
        uploadPictureResult.setPicWidth(picWidth);
        //设置图片高度
        uploadPictureResult.setPicHeight(picHeight);
        //设置图片宽高比
        uploadPictureResult.setPicScale(picScale);
        //设置图片格式，getFormat是获取格式的意思
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        //设置图片大小，通过临时文件获取
        uploadPictureResult.setPicSize(FileUtil.size(file));
        //设置图片可访问URL（COS主机地址+上传路径）
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 将转换格式后和图片进行缩略后的信息封装起来进行返回
     * @param originFilename 原始文件名
     * @param compressCiObject 图片转换格式后的对象
     * @param thumbnailCiObject 缩略图对象
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressCiObject, CIObject thumbnailCiObject) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //提取压缩图的宽度
        int picWidth = compressCiObject.getWidth();
        //提取压缩图的高度
        int picHeight = compressCiObject.getHeight();
        //计算 压缩图的宽高比
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        //往封装结果类里面设置图片名称
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        //往封装结果类里面设置图片宽度
        uploadPictureResult.setPicWidth(picWidth);
        //往封装结果类里面设置图片高度
        uploadPictureResult.setPicHeight(picHeight);
        //往封装结果类里面设置宽高比
        uploadPictureResult.setPicScale(picScale);
        //往封装结果类里面设置格式
        uploadPictureResult.setPicFormat(compressCiObject.getFormat());
        //往封装结果类里面设置图片大小
        uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
        //往封装结果类里面设置转换格式后的图片的地址Url
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressCiObject.getKey());
        //往封装结果类里面设置缩略图Url
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     *
     * @param file
     */
    private void deleteTempFile(File file) {
        if (file == null) {//若文件对象为空，直接返回
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

}
