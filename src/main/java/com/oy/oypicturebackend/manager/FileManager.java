package com.oy.oypicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.config.CosClientConfig;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * 这个类是一个比CosManager类更贴近项目业务的服务类，主要的业务是进行文件上传并返回图片解析信息的方法
 */
@Slf4j
@Service
@Deprecated //已废弃，改为使用upload包的模板方法优化
public class FileManager {
    @Resource//注入工具类，是为了从CosClientConfig拿配置信息
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片业务方法，接收前端文件，返回包含业务信息的结果
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀，即key的路径部分，key由路径+文件名组成
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //校验图片
        validPicture(multipartFile);
        //生成16位随机字符串作为uuid，用于保证文件名唯一性
        String uuid = RandomUtil.randomString(16);
        //获取前端上传文件的原始文件名
        String originalFilename = multipartFile.getOriginalFilename();

        //生成唯一文件名：格式为“当前日期_uuid.文件后缀”，确保文件名不重复
        //format方法是字符串格式化方法，可以用占位符%s %d等把多个变量拼接成一个有格式的字符串
        String uploadFileName = String.format(
                "%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));//获取原始文件的后缀

        //生成上传到COS的完整路径（key） ：格式为“/路径前缀/文件名”
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;
        try {
            //创建临时文件，用上传地址作为文件名前缀，后缀为null
            file = File.createTempFile(uploadFilePath, null);
            // 将MultipartFile类型的文件转换为File类型并写入临时文件
            multipartFile.transferTo(file);
            //调用CosManager的putObject方法上传文件到COS，返回上传结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);

            //从上传结果中获取图片信息对象（包含宽、高、格式等信息）
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            System.out.println(imageInfo);
            //提取图片宽度
            int picWidth = imageInfo.getWidth();
            //提取图片高度
            int picHeight = imageInfo.getHeight();
            //计算图片宽高比，保留两位小数
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            //创建上传图片结果对象，当图片上传完并返回上传结果之后就会用这个类来接收
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            //设置图片可访问URL（COS主机地址+上传路径）
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
            //设置图片名称，mainName是将原始文件名的后缀去掉后返回
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            //设置图片大小，通过临时文件获取
            uploadPictureResult.setPicSize(FileUtil.size(file));
            //设置图片宽度
            uploadPictureResult.setPicWidth(picWidth);
            //设置图片高度
            uploadPictureResult.setPicHeight(picHeight);
            //设置图片宽高比
            uploadPictureResult.setPicScale(picScale);
            //设置图片格式，getFormat是获取格式的意思
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            //将上传结果封装好返回
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到COS对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //清理临时文件
            this.deleteTempFile(file);
        }

    }


    /**
     * 校验图片规则，对大小、类型进行校验
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        //首先不能为空，若为空抛出参数错误异常
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        //不为空则校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024; //1MB
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");//大于2MB就抛出错误

        //获取前端上传的文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());//根据文件名称获取文件的后缀，例如文件名：photo.jpg 返回的后缀就是jpg

        //定义允许上传的文件后缀有哪些？用列表表示
        final List<String> ALLOW_FORMAT_LIST = new ArrayList<>();
        ALLOW_FORMAT_LIST.add("jpeg"); // 逐个添加
        ALLOW_FORMAT_LIST.add("png");
        ALLOW_FORMAT_LIST.add("jpg");
        ALLOW_FORMAT_LIST.add("webp");
        /*
        final List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpeg","png","jpg","webp");这种写法与上面那种写法在创建固定列表方面几乎等效，但这种写法是固定大小的，不能add和remove
         */
        //校验一下列表里面是否包含上传图片的后缀，若不在则抛出参数错误异常
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");

    }


    /**
     * 通过url上传图片
     *
     * @param fileUrl          文件地址
     * @param uploadPathPrefix 对象存储的前缀
     * @return
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        //通过url校验文件
        validPicture(fileUrl);
        //生成16位随机字符串作为uuid，用于保证文件名唯一性
        String uuid = RandomUtil.randomString(16);

        String originalFilename = FileUtil.mainName(fileUrl);

        //生成唯一文件名：格式为“当前日期_uuid.文件后缀”，确保文件名不重复
        //format方法是字符串格式化方法，可以用占位符%s %d等把多个变量拼接成一个有格式的字符串
        String uploadFileName = String.format(
                "%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));//获取原始文件的后缀

        //生成上传到COS的完整路径（key） ：格式为“/路径前缀/文件名”
        String uploadFilePath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);

        File file = null;

        try {
            //创建临时文件
            file = File.createTempFile(uploadFilePath, null);
            HttpUtil.downloadFile(fileUrl, file);

            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadFilePath, file);

            //从上传结果中获取图片信息对象（包含宽、高、格式等信息）
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            System.out.println(imageInfo);
            //提取图片宽度
            int picWidth = imageInfo.getWidth();
            //提取图片高度
            int picHeight = imageInfo.getHeight();
            //计算图片宽高比，保留两位小数
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            //创建上传图片结果对象，当图片上传完并返回上传结果之后就会用这个类来接收
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            //设置图片可访问URL（COS主机地址+上传路径）
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadFilePath);
            //设置图片名称，mainName是将原始文件名的后缀去掉后返回
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            //设置图片大小，通过临时文件获取
            uploadPictureResult.setPicSize(FileUtil.size(file));
            //设置图片宽度
            uploadPictureResult.setPicWidth(picWidth);
            //设置图片高度
            uploadPictureResult.setPicHeight(picHeight);
            //设置图片宽高比
            uploadPictureResult.setPicScale(picScale);
            //设置图片格式，getFormat是获取格式的意思
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            //将上传结果封装好返回
            return uploadPictureResult;

        } catch (Exception e) {
            log.error("图片上传到COS对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            //清理临时文件
            this.deleteTempFile(file);
        }
    }


    /**
     * 根据url校验文件规则，对大小、类型进行校验
     *
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
        //校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        //校验URL格式
        try {
            URL url = new URL(fileUrl);//验证是否合法的url
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        //校验URL协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");


        //发送HEAD请求验证文件是否存在，使用HEAD请求会返回响应头，常用于检查文件是否存在（通过状态码判断） 获取文件大小 类型 等等
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                //检查返回的状态码是不是OK ，若不是，未正常返回，直接return
                return;
            }
            //文件存在，文件类型校验
            String contentType = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                //允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
                }
            }
        } finally {
            //释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
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
