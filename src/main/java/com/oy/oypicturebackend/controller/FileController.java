package com.oy.oypicturebackend.controller;

import com.oy.oypicturebackend.annotation.AuthCheck;
import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.constant.UserConstant;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile Spring 提供的文件上传封装类，专门用于接收前端上传的文件
     * @return
     * @RequestPart("file")代表接收前端传过来的表单里面字段名叫file的数据
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();//拿到前端传过来的原始文件名
        String filePath = String.format("test/%s", fileName);//构造要上传到腾讯云COS里的路径，固定放在test/目录下，这个路径是这个文件在COS的唯一标识，也就是key

        File file = null;
        try {
            //上传文件
            file = File.createTempFile(filePath, null);//创建一个本地临时文件，createTempFile方法接收两个参数，第一个是文件名前缀，第二个是文件名后缀，这里用filePath做前缀，后缀为null
            multipartFile.transferTo(file);//把前端上传的MultipartFile内容写入到刚才的临时文件中，此时临时文件就和前端上传的文件内容一致了，因为腾讯云COS的上传方法需要接收File类型的参数
            cosManager.putObject(filePath, file);//上传到cos中，上传到的位置就是filePath

            //返回可访问的地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload error,filePath={}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                //删除临时文件，避免堆积
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error,filePath={}", filePath);
                }
            }
        }
    }


    /**
     * 测试文件下载
     * 这个方法返回类型是void，但前端依然能拿到数据，是因为通过HttpServletResponse 的输出流返回了文件内容
     *
     * @param filepath 要下载的文件在COS中的路径（key）
     * @param response 响应对象，用于向浏览器发送响应，包括文件流
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInputStream = null; //声明一个COS的文件输入流，并初始化为null
        try {
            COSObject cosObject = cosManager.getObject(filepath);//根据filepath从COS获取文件对象COSObject（包含文件的元数据和内容流）
            cosObjectInputStream = cosObject.getObjectContent();//获取文件的输入流

            byte[] bytes = IOUtils.toByteArray(cosObjectInputStream);//将输入流转换成字节数组，因为把文件写入输出流需要文件是字节数组的类型

            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");//setContentType用于设置HTTP响应的“内容类型”，这里是application/octet-stream表示 “二进制流数据”，告诉浏览器：“这是一个文件，需要下载而不是直接显示”
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);//设置响应头，attachment表示以附件形式下载，浏览器会触发文件保存弹窗，filename=filepath表示下载的文件名就是filepath

            response.getOutputStream().write(bytes);//将前面得到的字节数组写入response的输出流。
            response.getOutputStream().flush();//flush是刷新一下，确保数据立刻发送出去，不是等缓冲区满了才发
        } catch (Exception e) {
            log.error("文件下载错误，文件路径=" + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            //关闭释放流
            if (cosObjectInputStream != null) {
                cosObjectInputStream.close();
            }

        }
    }
}
