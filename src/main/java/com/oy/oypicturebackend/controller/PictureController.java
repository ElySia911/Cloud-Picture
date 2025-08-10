package com.oy.oypicturebackend.controller;


import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oy.oypicturebackend.annotation.AuthCheck;
import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.DeleteRequest;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.constant.UserConstant;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;

import com.oy.oypicturebackend.model.dto.picture.*;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.PictureReviewStatusEnum;
import com.oy.oypicturebackend.model.vo.PictureTagCategory;
import com.oy.oypicturebackend.model.vo.PictureVO;
import com.oy.oypicturebackend.service.PictureService;
import com.oy.oypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片 （可重新上传）
     *
     * @param multipartFile
     * @param pictureUploadRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/upload")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequestDTO pictureUploadRequestDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequestDTO, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过Url上传图片 （可重新上传）
     *
     * @param pictureUploadRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequestDTO pictureUploadRequestDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequestDTO.getFileUrl();

        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequestDTO, loginUser);
        return ResultUtils.success(pictureVO);
    }


    /**
     * 删除图片，管理员或者图片作者可删
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {

        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        //从删除请求中提取出要删除的图片id
        Long id = deleteRequest.getId();

        //判断是否存在，根据id查询数据库，若不存在这张图片则抛出未找到异常
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //仅本人或管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //操作数据库
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 更新图片（管理员）
     *
     * @param pictureUpdateRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequestDTO pictureUpdateRequestDTO, HttpServletRequest request) {
        if (pictureUpdateRequestDTO == null || pictureUpdateRequestDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将DTO转换成实体
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequestDTO, picture);

        //实体类的tags是json数组以字符串的形式存在，而DTO的tags是List  两者之间要进行转换
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequestDTO.getTags()));

        //对picture进行数据校验，检查核心字段是否符合要求
        pictureService.validPicture(picture);

        long id = pictureUpdateRequestDTO.getId();
        //根据id查询数据库有没有这张图片
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(oldPicture, loginUser);

        //操作数据库，需要传入Picture实体对象
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片（管理员）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Picture picture = pictureService.getById(id);
        //判断查出来的picture是不是空
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //返回
        return ResultUtils.success(picture);

    }

    /**
     * 根据id获取图片（封装类），用户可用
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        //接口要返回VO类型的数据，这里进行转换
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        return ResultUtils.success(pictureVO);
    }


    /**
     * 分页获取图片列表（仅管理员可用）
     * 接收前端的查询条件，通过分页方式从数据库获取图片数据并返回
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequestDTO pictureQueryRequestDTO) {
        //提取当前页
        long current = pictureQueryRequestDTO.getCurrent();
        //提取每页记录数
        long size = pictureQueryRequestDTO.getPageSize();

        //创建分页参数对象，指定当前页码和每页记录数
        Page<Picture> pageParam = new Page<>(current, size);

        //生成查询条件
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequestDTO);

        // 调用page方法，传入分页参数和查询条件，进行查询
        Page<Picture> picturePage = pictureService.page(pageParam, queryWrapper);

        return ResultUtils.success(picturePage);
    }


    /**
     * 分页获取图片列表（封装类，脱敏，用户可用）
     *
     * @param pictureQueryRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequestDTO pictureQueryRequestDTO, HttpServletRequest request) {
        long current = pictureQueryRequestDTO.getCurrent();
        long pageSize = pictureQueryRequestDTO.getPageSize();
        //限制爬虫，防止用户一次查询20张图片
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);

        //将请求DTO中的图片状态字段设置为审核通过，这样构造的sql就只查通过审核的，普通用户只能查看审核通过的图片
        pictureQueryRequestDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //构造分页参数
        Page<Picture> pageParam = new Page<>(current, pageSize);

        //创建查询条件包装器
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequestDTO);

        //查询数据库
        Page<Picture> picturePage = pictureService.page(pageParam, queryWrapper);

        //脱敏获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑（更新）图片 （用户使用）
     *
     * @param pictureEditRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequestDTO pictureEditRequestDTO, HttpServletRequest request) {
        //校验前端发过来请求是否为空 或者请求里面的id是否为空，如果id为空，数据库不知道编辑哪一张图片
        if (pictureEditRequestDTO == null || pictureEditRequestDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //操作数据库需要使用Picture实体类，new一个，然后属性复制
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequestDTO, picture);

        //不要忘记将DTO的tags从List类型转换成数据库要求的json数组类型
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequestDTO.getTags()));

        //编辑时间要同步更新，这里是更新数据库的editTime字段，不是updateTime字段，updateTime字段是数据库记录发生改变时候由数据库来更新
        picture.setEditTime(new Date());

        //校验数据，内部会对id进行必要性校验，没有id，数据库不知道要更新哪一张图片
        pictureService.validPicture(picture);

        //获取当前登录用户的信息
        User loginUser = userService.getLoginUser(request);

        //补充审核参数，将图片的状态设置为待审核
        pictureService.fillReviewParams(picture, loginUser);

        //获取要修改的图片的id
        long id = pictureEditRequestDTO.getId();

        //根据id查询数据库有没有这种图片
        Picture oldPicture = pictureService.getById(id);

        //校验，若为空，提示请求数据不存在
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //校验身份，仅图片创作者和管理员可编辑更新
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 向前端返回图片相关的标签和分类数据，提供一个固定的标签与分类列表
     * 核心目的：
     * 展示可选的标签（比如用户上传图片时，可从这些标签中选择）。
     * 展示分类导航（比如按 “模板”“海报” 等分类浏览图片）
     *
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {

        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        //定义标签列表tagList，通过asList创建一个包含预设标签的列表，这些是图片可能关联的标签
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "风景", "艺术", "校园", "赛博朋克", "汽车", "创意");
        //定义分类列表categoryList，这些是图片的大类别，用于对图片进行归类
        List<String> categoryList = Arrays.asList("抽象", "自然", "表情包", "素材", "现实");
        //封装并返回
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }


    /**
     * 管理员审核图片
     *
     * @param pictureReviewRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequestDTO pictureReviewRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequestDTO, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 批量抓取并上传创建图片
     *
     * @param pictureUploadByBatchRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequestDTO pictureUploadByBatchRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequestDTO, loginUser);
        return ResultUtils.success(uploadCount);
    }
}


