package com.oy.oypicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 将本地文件进行上传，继承父类模板
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    private static final long MAX_SIZE = 2L * 1024 * 1024;//2MB


    //校验输入源  步骤：转MultipartFile；判空；限制大小；限制后缀
    @Override
    protected void validPicture(Object inputSource) {
        //输入源 转换为 文件类型
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        //不为空则校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > MAX_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST = new ArrayList<>();
        ALLOW_FORMAT_LIST.add("jpeg");
        ALLOW_FORMAT_LIST.add("png");
        ALLOW_FORMAT_LIST.add("jpg");
        ALLOW_FORMAT_LIST.add("webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    //获取输入源的本地文件原文件名，即本地文件名
    @Override
    protected String getOriginalFilename(Object inputSource) {
        //输入源 转换为 文件类型
        MultipartFile multipartFile = (MultipartFile) inputSource;
        String originalFilename = multipartFile.getOriginalFilename();
        return originalFilename;
    }

    //处理输入源并生成本地临时文件
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
