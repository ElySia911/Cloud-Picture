package com.oy.oypicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * 使用Url将远程图片上传，继承父类模板
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    private static final long MAX_SIZE = 2L * 1024 * 1024;

    //校验输入源
    @Override
    protected void validPicture(Object inputSource) {
        //输入源转换成String类型
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        //校验Url格式
        try {
            URL url = new URL(fileUrl);//尝试将 fileUrl 解析为 Java 标准的 URL 对象
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        //校验Url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持HTTP或HTTPS协议的文件地址");
        //发送HEAD请求验证文件是否存在，使用HEAD请求会返回响应头，不下载文件内容，能大幅减少网络传输量
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            //校验HTTP响应状态码：仅200（OK）表示地址可访问
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //校验文件类型：通过响应头 Content-Type 判断是否为允许的图片类型
            String contentType = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                //允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //校验文件大小：通过响应头 Content-Length 判断是否超过2MB
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    ThrowUtils.throwIf(contentLength > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
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

    //获取原始文件名
    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        String cleanUrl = fileUrl.split("\\?")[0]; // 去掉 ? 及后面的参数，因为AI扩图之后返回的图片url中带有?及参数
        //String originalFilename = FileUtil.getName(fileUrl);//使用getName才能拿到带后缀的文件名
        String originalFilename = FileUtil.getName(cleanUrl);//使用getName才能拿到带后缀的文件名
        return originalFilename;
    }

    //
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
