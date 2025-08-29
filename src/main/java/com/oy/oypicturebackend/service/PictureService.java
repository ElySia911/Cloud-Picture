package com.oy.oypicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oy.oypicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.oy.oypicturebackend.model.dto.picture.*;
import com.oy.oypicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author ouziyang
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-01 01:29:33
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource             文件输入源
     * @param pictureUploadRequestDTO id用于修改
     * @param loginUser               用户，判断用户是否有权限上传
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequestDTO pictureUploadRequestDTO, User loginUser);

    /**
     * 获取查询对象，根据前端传入的查询条件（PictureQueryRequestDTO），构建一个MP的查询条件包装器（QueryWrapper）用于数据库查询时动态拼接接待查询的条件
     *
     * @param pictureQueryRequestDTO
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequestDTO pictureQueryRequestDTO);

    /**
     * 获取单个图片封装
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * 将数据库查询到的分页图片数据（Page<Picture>）转换为包含用户信息的前端分页视图数据（Page<PictureVO>）
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 编写图片数据校验方法，用于更新和修改图片时候进行判断
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequestDTO
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequestDTO pictureEditByBatchRequestDTO, User loginUser);




    /*-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/


    /**
     * 图片审核请求
     *
     * @param pictureReviewRequestDTO 审核请求
     * @param loginUser               登录用户，系统需要知道是哪一个管理员审核图片的
     */
    void doPictureReview(PictureReviewRequestDTO pictureReviewRequestDTO, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequestDTO
     * @param loginUser
     * @return 成功创建的图片数量
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequestDTO pictureUploadByBatchRequestDTO, User loginUser);


    /**
     * 清理对象存储中的图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);


    /**
     * 删除图片，管理员或图片作者可以删
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑（更新）图片 （用户使用）
     *
     * @param pictureEditRequestDTO
     * @param loginUser
     */
    void editPicture(PictureEditRequestDTO pictureEditRequestDTO, User loginUser);


    /*-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/


    /**
     * 填充审核参数：待审核  审核通过  拒绝
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 校验空间图片的权限 传进来的参数就是图片和当前登录的这个人
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequestDTO
     * @param loginUser
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequestDTO createPictureOutPaintingTaskRequestDTO, User loginUser);
}
