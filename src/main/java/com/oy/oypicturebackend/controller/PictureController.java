package com.oy.oypicturebackend.controller;


import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;

import com.oy.oypicturebackend.annotation.AuthCheck;
import com.oy.oypicturebackend.api.aliyunai.AliYunAiApi;
import com.oy.oypicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.oy.oypicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.oy.oypicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.oy.oypicturebackend.api.imagesearch.model.ImageSearchResult;
import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.DeleteRequest;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.constant.UserConstant;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;

import com.oy.oypicturebackend.manager.auth.SpaceUserAuthManager;
import com.oy.oypicturebackend.manager.auth.StpKit;
import com.oy.oypicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.oy.oypicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.oy.oypicturebackend.model.dto.picture.*;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.PictureReviewStatusEnum;
import com.oy.oypicturebackend.model.vo.PictureTagCategory;
import com.oy.oypicturebackend.model.vo.PictureVO;
import com.oy.oypicturebackend.service.PictureLikeService;
import com.oy.oypicturebackend.service.PictureService;
import com.oy.oypicturebackend.service.SpaceService;
import com.oy.oypicturebackend.service.UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PictureLikeService pictureLikeService;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 构造本地缓存（正常来说需要封装起来）
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)//初始容量分配的内存
            .maximumSize(10000L)//最大10000条数据
            .expireAfterWrite(5L, TimeUnit.MINUTES)// 缓存5分钟后移除
            .build();


    /**
     * 上传本地图片 （可重新上传）
     *
     * @param multipartFile
     * @param pictureUploadRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)//本质是@SaCheckPermission(type="space", value="picture:upload")
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
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequestDTO pictureUploadRequestDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequestDTO.getFileUrl();

        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequestDTO, loginUser);
        return ResultUtils.success(pictureVO);
    }


    /**
     * 删除图片，管理员或者图片作者可删
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {

        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取当前登录用户信息
        User loginUser = userService.getLoginUser(request);
        //从删除请求中提取出要删除的图片id
        Long id = deleteRequest.getId();
        //调用删除图片方法
        pictureService.deletePicture(id, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 更新图片（管理员）
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
        //根据id查询数据库有没有这张图片，因为图片存在才能更新啊
        long id = pictureUpdateRequestDTO.getId();
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
     * 编辑图片，和上面的更新图片接口(/update)差不多一样 （用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequestDTO pictureEditRequestDTO, HttpServletRequest request) {
        //校验前端发过来请求是否为空 或者请求里面的id是否为空，如果id为空，数据库不知道编辑哪一张图片
        if (pictureEditRequestDTO == null || pictureEditRequestDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取当前登录用户的信息
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequestDTO, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片（管理员）
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
     * 这个方法没有使用注解鉴权是因为，注解鉴权必须强制要求用户登录，而项目中的首页是不需要登录也能看到公共图库的图片的
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        //查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //空间权限校验，校验查出来的这张照片所属的空间是不是当前登录用户自己的空间
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            //检查当前用户在space体系下，是否拥有"picture:view"这个权限
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            //不等于null，就代表这张图片是私人的，要进行校验
            //User loginUser = userService.getLoginUser(request);
            //pictureService.checkPictureAuth(loginUser, picture);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        //获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);

        //接口要返回VO类型的数据，这里进行转换
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
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
     * 分页获取图片列表（封装类，脱敏，用户可用，首页图片展示用的是这个接口）
     * 这个方法没有使用注解鉴权是因为，注解鉴权必须强制要求用户登录，而项目中的首页是不需要登录也能看到公共图库的图片的
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequestDTO pictureQueryRequestDTO, HttpServletRequest request) {
        //从请求DTO中获取当前页码和每页条数（继承自PageRequest基类的属性）
        long current = pictureQueryRequestDTO.getCurrent();
        long pageSize = pictureQueryRequestDTO.getPageSize();
        //限制爬虫，防止用户一次查询20张图片
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);

        //空间权限校验
        Long spaceId = pictureQueryRequestDTO.getSpaceId();
        if (spaceId == null) {
            //空间id为空，将nullSpaceId字段 设置为true，明确查询公共图库，sql会拼接上 spaceId is null的条件
            pictureQueryRequestDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());//将DTO中审核状态字段设置为审核通过
            pictureQueryRequestDTO.setNullSpaceId(true);
        } else {
            //私有空间
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            //已改用其他鉴权方法
            /*User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);//从数据库查出这个空间的信息，若空间不存在就报错，还查个屁的图片
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }*/
        }

        //构造分页参数
        Page<Picture> pageParam = new Page<>(current, pageSize);

        //创建查询条件包装器 （getQueryWrapper内部会根据nullSpaceId生成条件：space_id IS NULL；根据spaceId生成：space_id = ?）
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequestDTO);

        //查询数据库，执行分页查询，获取数据库原始数据
        Page<Picture> picturePage = pictureService.page(pageParam, queryWrapper);

        //脱敏获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        return ResultUtils.success(pictureVOPage);
    }


    /**
     * 分页获取图片列表（封装类，脱敏，用户可用，使用了本地缓存Caffeine和Redis缓存）
     *
     * @param pictureQueryRequestDTO
     * @param request
     * @return
     */
    @Deprecated//考虑到私有空间的图片更新频率不好把握，这里编写的缓存分页查询图片接口暂时废弃
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequestDTO pictureQueryRequestDTO, HttpServletRequest request) {
        long current = pictureQueryRequestDTO.getCurrent();//页号
        long pageSize = pictureQueryRequestDTO.getPageSize();//每页记录数
        //限制爬虫，防止用户一次查询20张图片
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //将请求DTO中的图片状态字段设置为审核通过，这样构造的sql就只查通过审核的，普通用户只能查看审核通过的图片
        pictureQueryRequestDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //要想查询缓存，那肯定要根据key去查value，那么先构造key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequestDTO);//先将查询条件序列化为JSON字符串
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());//将序列化后的查询条件使用md5哈希处理，得到一个字符串（哈希值）避免原始JSON字符串过长导致缓存键冗余。
        String cacheKey = String.format("listPictureVOByPage:%S", hashKey);//查询的条件不同，对应的hashKey也不同，最终cacheKey由固定前缀+hashKey组成

        //1.先查本地缓存
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            //如果本地缓存命中，直接返回
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }

        //2.本地缓存未命中，查询Redis分布式缓存
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();//先拿到一个可以操作redis的String类型对象valueOps
        cachedValue = valueOps.get(cacheKey);//根据key去查询
        if (cachedValue != null) {
            //如果Redis缓存命中，说明redis中有缓存，则更新本地缓存，返回结果，返回的结果是经过反序列的
            LOCAL_CACHE.put(cacheKey, cachedValue);//更新本地缓存
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }

        //3.若本地缓存和Redis缓存都没命中，则去查数据库
        Page<Picture> pageParam = new Page<>(current, pageSize);//构造分页参数
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequestDTO);//构造查询条件包装器
        Page<Picture> picturePage = pictureService.page(pageParam, queryWrapper);//查数据库
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);//脱敏获取封装类

        //4.将转换后的PictureVO分页对象序列化为JSON字符串用来更新缓存，要更新Redis缓存和本地缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        //更新Redis缓存
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);//300秒（5分钟）基础上，随机增加0-300秒（0-5分钟）总过期时间5-10分钟过期，防止雪崩，雪崩即某一时间段内，大量缓存数据同时过期失效，导致原本应该由缓存处理的请求全部涌向数据库
        valueOps.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        return ResultUtils.success(pictureVOPage);
    }


    /**
     * 向前端返回图片相关的标签和分类数据，提供一个固定的标签与分类列表
     * 核心目的：展示可选的标签（比如用户上传图片时，可从这些标签中选择）。展示分类导航（比如按 “模板”“海报” 等分类浏览图片）
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

//0.<

    /**
     * 批量抓取并上传创建图片（管理员）
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

    /**
     * 以图搜图
     *
     * @param searchPictureByPictureRequestDTO
     * @return
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequestDTO searchPictureByPictureRequestDTO) {
        ThrowUtils.throwIf(searchPictureByPictureRequestDTO == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequestDTO.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 按照颜色搜索
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequestDTO searchPictureByColorRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequestDTO == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequestDTO.getPicColor();
        Long spaceId = searchPictureByColorRequestDTO.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 批量编辑图片的分类和标签
     *
     * @param pictureEditByBatchRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequestDTO pictureEditByBatchRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequestDTO, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 用户对图片进行点赞
     */
    @PostMapping("/like")
    public BaseResponse<Boolean> likePicture(@RequestBody PictureLikeRequestDTO pictureLikeRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureLikeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        pictureLikeService.likePicture(pictureLikeRequestDTO, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取用户点赞过的图片id列表
     *
     * @param request
     * @return
     */
    @GetMapping("/my/like/picture")
    public BaseResponse<List<Long>> listMyLikedPictureIds(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        List<Long> myLikedPictureIds = pictureLikeService.listMyLikedPictureIds(loginUser.getId());
        return ResultUtils.success(myLikedPictureIds);
    }


    /**
     * 创建AI拓图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequestDTO createPictureOutPaintingTaskRequestDTO, HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequestDTO == null || createPictureOutPaintingTaskRequestDTO.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequestDTO, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询AI拓图任务结果
     *
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StringUtils.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTaskResponse(taskId);
        return ResultUtils.success(task);
    }
}


