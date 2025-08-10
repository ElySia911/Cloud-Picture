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
     * MultipartFile是 Spring Boot 提供的文件接收类型。
     *
     * @param multipartFile
     * @return
     * @RequestPart("file")代表接收前端传过来的表单里面字段名叫file的数据
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();//拿到原始文件名
        String filePath = String.format("test/%s", fileName);//构造要上传到腾讯云COS里的路径，这个路径是这个文件的唯一标识 也就是key

        File file = null;
        try {
            //上传文件
            file = File.createTempFile(filePath, null);//使用createTempFile方法创建一个临时文件，createTempFile方法接收两个参数，第一个是文件名前缀，第二个是文件名后缀，这里用filePath有可能报错，但目前没遇到
            multipartFile.transferTo(file);//把前端上传的MultipartFile内容写入到刚才的临时文件中，相当于把它保存到磁盘，这样就实现了文件类型的转换
            cosManager.putObject(filePath, file);//上传到cos中，上传到的位置就是filePath

            //返回可访问的地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload error,filePath={}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                //删除临时文件
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
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);//通过指定的key，即filepath拿到文件，cosObject里面就包含文件内容
            cosObjectInputStream = cosObject.getObjectContent();//获取这个文件的输入流，后续通过此输入流读取文件数据

            byte[] bytes = IOUtils.toByteArray(cosObjectInputStream);//将输入流转换成字节数组，因为把文件写入响应需要文件是字节数组的类型

            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");//告诉浏览器，我给你发送的是二进制文件，不是网页 ，不要直接打开。application/octet-stream表示通用的二进制文件类型
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);//告诉浏览器，这个内容你要当作附件下载，不要直接打开，attachment表示强制下载，filename=后面指定下载时默认保存的文件名
            //将字节数组写入response的输出流
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();//flush是确保数据立刻发送出去，不是等缓冲区满了才发
        } catch (Exception e) {
            log.error("file download error,filepath=" + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            //关闭释放流
            if (cosObjectInputStream != null) {
                cosObjectInputStream.close();
            }

        }


    }
}
