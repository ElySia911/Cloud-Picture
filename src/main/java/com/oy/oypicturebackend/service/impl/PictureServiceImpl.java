package com.oy.oypicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oy.oypicturebackend.api.aliyunai.AliYunAiApi;
import com.oy.oypicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequestDTO;
import com.oy.oypicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.manager.CosManager;
import com.oy.oypicturebackend.manager.FileManager;
import com.oy.oypicturebackend.manager.upload.FilePictureUpload;
import com.oy.oypicturebackend.manager.upload.PictureUploadTemplate;
import com.oy.oypicturebackend.manager.upload.UrlPictureUpload;
import com.oy.oypicturebackend.model.dto.file.UploadPictureResult;
import com.oy.oypicturebackend.model.dto.picture.*;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.PictureReviewStatusEnum;
import com.oy.oypicturebackend.model.vo.PictureVO;
import com.oy.oypicturebackend.model.vo.UserVO;
import com.oy.oypicturebackend.service.PictureService;
import com.oy.oypicturebackend.mapper.PictureMapper;
import com.oy.oypicturebackend.service.SpaceService;
import com.oy.oypicturebackend.service.UserService;
import com.oy.oypicturebackend.utils.ColorSimilarUtils;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ouziyang
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-01 01:29:33
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private CosManager cosManager;
    @Resource
    private TransactionTemplate transactionTemplate;//编程式事务
    @Resource
    private AliYunAiApi aliYunAiApi;


    /**
     * 上传图片（增）
     *
     * @param inputSource             输入源
     * @param pictureUploadRequestDTO id用于修改
     * @param loginUser               用户，判断用户是否有权限上传
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequestDTO pictureUploadRequestDTO, User loginUser) {
        //1.校验登录用户：未登录用户无法上传
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //2.校验空间：存在性、权限、额度
        Long spaceId = pictureUploadRequestDTO.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //2.1空间存在的话，就校验是否有这个空间的权限，仅这个空间的创建人才能上传图片，登录人id等于空间创建者id才能上传
            //已经改为使用统一的权限校验
            //if (!loginUser.getId().equals(space.getUserId())) {
            //    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            //}
            //2.2校验空间的额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间可用条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足");
            }
        }

        //3.判断是新增还是更新，若pictureId为null，是新增，若pictureId不为null，则提取其中pictureId，表示可能是更新操作
        Long pictureId = pictureUploadRequestDTO.getId();//从请求中尝试提取出图片id字段
        Picture oldPicture = null;

        //更新有关
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            //校验图片的编辑权限：仅当前这一张图的作者或管理员可编辑
            //改为使用统一的权限校验
            //if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            //    throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            //}

            //校验空间是否一致，没传spaceId，则复用原有图片的spaceId，让这张图片默认待在原来的空间里。公共图库的图片就没有spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                //传了spaceId，必须和原图片所属的空间id一致，避免用户传进别人的空间
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id不一致");
                }
            }
        }

        //二.上传处理阶段
        //确定上传路径前缀：公共图库/私人空间
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }

        //根据输入源inputSource的类型选择上传模板
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;//默认：文件上传模板
        if (inputSource instanceof String) {//如果输入源是字符串Url ，切换为URL上传模板
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //三.数据库操作阶段
        //将上传结果的图片信息转换成Picture实体类存入数据库
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);//关联空间id，公共图库为null
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());//设置缩略图Url
        //支持自定义图片名：如果请求传了自定义名称，就用自定义的，否则用上传后的默认名
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequestDTO != null && StrUtil.isNotBlank(pictureUploadRequestDTO.getPicName())) {
            picName = pictureUploadRequestDTO.getPicName();
        }
        picture.setName(picName);
        //设置大小、宽高、比例、格式、颜色、所属用户ID
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        //picture.setPicColor(uploadPictureResult.getPicColor());
        //picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        //不管是新增还是修改更新，只要不是管理员就需要审核，fillReviewParams方法内部会根据登录用户的身份进行图片状态的审核设置
        this.fillReviewParams(picture, loginUser);
        //操作数据库，区分新增/更新：更新需要设置ID和编辑时间
        if (pictureId != null) {
            //如果是更新，需要补充id和编辑时间，若是不补充id，saveOrUpdate方法会把它当作新记录插入导致重复数据
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //开启事务：保证“图片入库 + 空间额度更新”要么都成功，要么都失败
        Long finalSpaceId = spaceId;
        Picture finalPicture = oldPicture;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);//根据picture的id是否为空来决定执行新增/修改
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                //图片关联空间，需要更新空间的额度
                boolean update;
                if (pictureId != null) {
                    update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize = totalSize - " + finalPicture.getPicSize() + " + " + picture.getPicSize())
                            .update();
                } else {
                    update = spaceService.lambdaUpdate()
                            .eq(Space::getId, finalSpaceId)
                            .setSql("totalSize=totalSize+" + picture.getPicSize())
                            .setSql("totalCount=totalCount+1")
                            .update();
                }
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        //若是更新图片，那么更新完数据库的图片信息后，把对象存储中旧的图片清除
        if (pictureId != null && oldPicture != null) {
            this.clearPictureFile(oldPicture);
        }
        return PictureVO.objToVo(picture);
    }

    /**
     * 根据前端传入的查询条件（PictureQueryRequestDTO），构建一个MP的查询条件包装器（QueryWrapper）用于数据库查询时动态拼接接待查询的条件
     *
     * @param pictureQueryRequestDTO
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequestDTO pictureQueryRequestDTO) {
        //初始化一个空的查询条件包装器，Picture是要查询的实体类
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //如果前端查询请求DTO为null（即前端没传任何查询条件），直接返回空查询条件
        if (pictureQueryRequestDTO == null) {
            return queryWrapper;
        }
        //从查询请求DTO中提取所有可能的查询参数（这些参数对应前端可能传入的查询条件）
        Long id = pictureQueryRequestDTO.getId();
        String name = pictureQueryRequestDTO.getName();
        String introduction = pictureQueryRequestDTO.getIntroduction();//简介
        String category = pictureQueryRequestDTO.getCategory();//分类
        List<String> tags = pictureQueryRequestDTO.getTags();//标签
        Long picSize = pictureQueryRequestDTO.getPicSize();//体积
        Integer picWidth = pictureQueryRequestDTO.getPicWidth();
        Integer picHeight = pictureQueryRequestDTO.getPicHeight();
        Double picScale = pictureQueryRequestDTO.getPicScale();//宽高比
        String picFormat = pictureQueryRequestDTO.getPicFormat();//图片格式
        String searchText = pictureQueryRequestDTO.getSearchText();//搜索词（同时搜名称、简介等）
        Long userId = pictureQueryRequestDTO.getUserId();//用户id

        Integer reviewStatus = pictureQueryRequestDTO.getReviewStatus();//审核状态
        String reviewMessage = pictureQueryRequestDTO.getReviewMessage();//审核信息
        Long reviewerId = pictureQueryRequestDTO.getReviewerId();//审核员id

        Long spaceId = pictureQueryRequestDTO.getSpaceId();
        Date startEditTime = pictureQueryRequestDTO.getStartEditTime();
        Date endEditTime = pictureQueryRequestDTO.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequestDTO.isNullSpaceId();

        String sortField = pictureQueryRequestDTO.getSortField();//排序字段
        String sortOrder = pictureQueryRequestDTO.getSortOrder();//排序顺序，默认升序

        //根据用户输入的搜索关键词 (searchText) 在数据库表中模糊匹配 name 或 introduction 字段
        if (StrUtil.isNotBlank(searchText)) { //isNotBlank()判断字符串是否为空（非null 非"" 非" "） ,为什么不用isEmpty(),因为isEmpty不会忽略空格
            //使用and()和or()拼接条件： and(name LIKE %searchText% OR introduction LIKE %searchText%)
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or().like("introduction", searchText));
        }

        //处理单个字段的精确匹配或模糊匹配条件：
        //规则：只有当参数不为空时，才会添加该条件（避免空值导致的查询错误）
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);// id精确匹配（=）
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);// 上传者ID精确匹配
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);// 名称模糊匹配（LIKE %name%）
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);// 简介模糊匹配
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);// 图片格式模糊匹配
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);// 分类精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);// 宽度精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight); // 高度精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);// 体积精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);// 宽高比精确匹配
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId"); // isNull先判断nullSpaceId是否为true，若是，则添加spaceId IS NULL条件，否则不添加
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);//ge是大于等于，startEditTime不为空，就在字段editTime找出大于等于startEditTime的数据
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);//lt是小于，endEditTime不为空，就在字段editTime找出小于endEditTime的数据
        // 处理标签列表（tags）查询：如果标签列表不为空，最终查询结果会同时包含”风景“和”自然“两个标签的图片
        //假设数据库中tags字段是以JSON数组形式存储的（如["风景","自然"]）
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                // 用like匹配JSON数组中的单个标签（加双引号避免部分匹配，如"自然"不会匹配"自然风光"）
                //生成的sql：tags LIKE '%"风景"%' AND tags LIKE '%"自然"%'
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // 排序，sortOrder.equals("ascend")：如果是"ascend"则升序，否则降序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        //打印一下拼接好的的sql条件，不含from和表名，仅where后的条件
        String sqlSegment = queryWrapper.getCustomSqlSegment();
        System.out.println("【构建好的sql条件：" + sqlSegment + "】");

        //返回构建好的查询条件包装器，用于后续数据库查询
        return queryWrapper;
    }

    /**
     * 获取单个图片封装即对单张图片进行脱敏
     * 接收Picture实体和请求对象，返回封装后的PictureVO
     *
     * @param picture 数据库查出来的图片实体
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //调用objToVo方法，实现对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //从图片实体中获取上传者的用户id
        Long userId = picture.getUserId();
        //验证userId是否有效，不为null且大于0，避免无效用户id
        if (userId != null && userId > 0) {
            //根据userId查询对应用户信息
            User user = userService.getById(userId);
            //获取脱敏后的用户信息
            UserVO userVO = userService.getUserVO(user);
            //将脱敏的用户信息写进封装类里面
            pictureVO.setUser(userVO);
        }
        //返回包含用户信息的VO对象（给前端使用）
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     * 将数据库查询到的分页图片数据（Page<Picture>）转换为包含用户信息的前端分页视图数据（Page<PictureVO>）
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {

        // 创建一个空的PictureVO分页对象，复制原Picture分页对象的核心分页参数，保证分页信息的一致性，参数解释：getCurrent当前页码，getSize每页条数，getTotal总条数
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        List<Picture> pictureList = picturePage.getRecords();//获取原分页对象中的具体数据列表（当前页的所有Picture实体）
        //如果当前这一页没数据，即列表里面是空的，直接返回空的分页VO对象，提取终止流程
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //将存储了Picture类型数据的列表通过stream流的方式，遍历列表中每一个元素，调用PictureVO类中的objToVo方法进行转换，得到一个存储PictureVO类型数据的列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        //通过Stream流提取所有图片的userId，利用Set的唯一性实现userId去重，避免重复查询用户
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        //批量查询所有去重后的userId对应的用户，得到的列表中的每个元素都是一个完整的User对象，然后将查询得到的列表按userId分组为Map，转换为key=userId value=用户列表的Map。实际每个userId对应一个用户，所以List中只有一个元素
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        //遍历列表里面的每一个元素 用pictureVO临时变量来表示
        pictureVOList.forEach(pictureVO -> {
            //取出每个元素对应的userId
            Long userId = pictureVO.getUserId();
            User user = null;
            //去userIdUserListMap里面检查有没有key等于这个userId的记录
            if (userIdUserListMap.containsKey(userId)) {
                //有的话，就通过get(userId)找到这个key，然后通过get(0)获取这个key的value中的第一个元素，即拿到一个user
                user = userIdUserListMap.get(userId).get(0);
            }
            //将user进行脱敏后，设置给pictureVO
            pictureVO.setUser(userService.getUserVO(user));
        });
        //将转换好的图片VO列表设置给图片分页VO对象中
        pictureVOPage.setRecords(pictureVOList);
        //返回完整数据的分页VO对象给前端
        return pictureVOPage;
    }



    /*-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/


    /**
     * 图片审核请求
     *
     * @param pictureReviewRequestDTO 审核请求
     * @param loginUser               登录用户，系统需要知道是哪一个管理员审核图片的
     */
    @Override
    public void doPictureReview(PictureReviewRequestDTO pictureReviewRequestDTO, User loginUser) {
        //校验参数
        Long id = pictureReviewRequestDTO.getId();//拿图片id
        Integer reviewStatus = pictureReviewRequestDTO.getReviewStatus();//从前端发过来的请求中拿到审核状态 0或者1或者2
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);//拿到枚举常量
        //如果id为空 或者 审核状态为空 或者审核状态不为空但审核状态是待审核，就抛异常
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验审核状态是否重复，假如已经通过审核的图片，不能重新修改为通过状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            //如果从数据库查出来的图片的审核状态等于前端发起请求中的状态，就代表重复审核，抛出异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        //数据库操作，更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequestDTO, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    /**
     * 批量抓取和创建图片
     * https://cn.bing.com/images/async?q=%s&mmasync=1(必应图库获取图片数据的接口)
     *
     * @param pictureUploadByBatchRequestDTO
     * @param loginUser
     * @return 成功创建的图片数量
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequestDTO pictureUploadByBatchRequestDTO, User loginUser) {
        //取出搜索词和上限
        String searchText = pictureUploadByBatchRequestDTO.getSearchText();
        Integer count = pictureUploadByBatchRequestDTO.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
        String namePrefix = pictureUploadByBatchRequestDTO.getNamePrefix();//前端展示的图片的名称前缀
        if (StrUtil.isBlank(namePrefix)) {
            //若图片名称前缀为空，将图片名称前缀设置为搜索词
            namePrefix = searchText;
        }

        Random random = new Random();
        int randomFirst = random.nextInt(50);//生成0-49的偏移量
        //用搜索词和随机偏移量拼接URL组装要抓取的地址
        String fetchUrl = String.format("https://www.bing.com/images/async?q=%s&mmasync=1&first=%d", searchText, randomFirst);
        Document document;
        //抓取页面
        try {
            //模拟浏览器发送 GET 请求，返回解析后的 HTML 文档
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败，", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        //从HTML文档中获取第一个class包含dgControl的节点（图片列表的容器）
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        //从容器中筛选所有<a class="iusc">的元素，必应每张图片都封装在这个a标签里面
        Elements imgElementList = div.select("a.iusc");

        int uploadCount = 0;//定义一个变量用于记录上传成功的数量
        //遍历每张图片节点，取出每张图片高清地址
        for (Element imgElement : imgElementList) {
            String dataM = imgElement.attr("m");//获取a标签中 m的属性值，得到的是一个JSON字符串
            String fileUrl;
            try {
                JSONObject jsonObject = JSONUtil.parseObj(dataM);//将JSON字符串反序列化为JSON对象
                fileUrl = jsonObject.getStr("murl");//从反序列化后的JSON对象中获取键为murl的值，得到图片的链接并赋值给fileUrl
            } catch (Exception e) {
                log.error("解析图片数据失败");
                continue;
            }
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            //截断图片链接中问号及后面的参数，防止转义或者和腾讯云对象存储冲突的问题，例如 codefather.cn?ozy=dog,应该只保留codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");//找到?的下标，若大于-1就更新src
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //定义上传图片方法所需参数
            PictureUploadRequestDTO pictureUploadRequestDTO = new PictureUploadRequestDTO();
            pictureUploadRequestDTO.setFileUrl(fileUrl);
            pictureUploadRequestDTO.setPicName(namePrefix + (uploadCount + 1));
            //尝试上传，成功则自增，失败则跳过，达到count就结束，并返回成功的数量
            try {
                //uploadPicture可以优化为批量上传，但其实上传到cos的速度是不变的
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequestDTO, loginUser);
                log.info("抓取的图片上传成功，id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                //上传失败记录日志，跳过当前图片
                log.error("抓取的图片上传失败");
                continue;
            }
            if (uploadCount >= count) {//达到指定数量则终止循环
                break;
            }
        }
        return uploadCount;
    }


    /**
     * 清理对象存储中的图片
     *
     * @param oldPicture
     */
    @Async //异步执行，避免阻塞主线程
    @Override
    public void clearPictureFile(Picture oldPicture) {
        String host = "https://oy-1372001294.cos.ap-guangzhou.myqcloud.com/";//域名

        //判断改图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();//这里拿到的是包含域名的url，这个url有可能是原图的url，也有可能是webp的url
        String key = pictureUrl.replace(host, "");//使用replace将域名部分替换为空字符串
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        //大于1就代表不只一条记录用到这张图片，那就不清理
        if (count > 1) {
            return;
        }
        //清理图片（有可能清理的是转换格式后的那张，也可能清理原图）
        cosManager.deleteObject(key);

        //清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();//缩略图Url，包含域名
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            String thumbnailUrlKey = thumbnailUrl.replace(host, "");
            cosManager.deleteObject(thumbnailUrlKey);
        }
    }


    /**
     * 删除图片，管理员或图片作者可以删
     *
     * @param pictureId
     * @param loginUser
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限 （已经改为使用注解鉴权）
        //checkPictureAuth(loginUser, oldPicture);
        //开启事务，在事务内执行核心操作，确保删除图片记录和更新空间额度要么同时成功，要么同时失败
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            //若图片属于私人空间，释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)//eq是等于条件 ，引用Space实体类的id字段（对应数据库表的id列）整体含义： where id =?(?替换为spaceId的值)
                        .setSql("totalSize=totalSize-" + oldPicture.getPicSize())//减去图片大小，例如图片1M，就总容量减1M
                        .setSql("totalCount=totalCount-1")
                        .update();
                /*类似：UPDATE space SET totalSize = totalSize - 1024, totalCount = totalCount - 1 WHERE id = 123*/
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });

        // 数据库删完，对象存储也要删
        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑（更新）图片 （用户使用）
     *
     * @param pictureEditRequestDTO
     * @param loginUser
     */
    @Override
    public void editPicture(PictureEditRequestDTO pictureEditRequestDTO, User loginUser) {
        //操作数据库需要使用Picture实体类，new一个，然后属性复制
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequestDTO, picture);
        //不要忘记将DTO的tags从List类型转换成数据库要求的json数组类型
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequestDTO.getTags()));
        //编辑时间要同步更新，这里是更新数据库的editTime字段，不是updateTime字段，updateTime字段是数据库记录发生改变时候由数据库来更新
        picture.setEditTime(new Date());
        //校验数据，这个方法内部会对picture的id字段进行必要性校验，没有id，数据库不知道要更新哪一张图片，同时也会对简介和url的长度进行校验
        this.validPicture(picture);
        //获取要修改的图片的id
        long id = pictureEditRequestDTO.getId();
        //根据id查询数据库有没有这张图片，要是数据库没这个id的图片，那还更新个鸡毛，直接报错
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验图片权限 （已经改为使用注解鉴权）
        //this.checkPictureAuth(loginUser, oldPicture);
        //补充审核参数，将图片的状态设置为待审核
        this.fillReviewParams(picture, loginUser);
        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }



    /*-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/

    /**
     * 填充审核参数：待审核  审核通过  拒绝
     * 该方法用于根据登录用户是否为管理员来设置图片的审核状态和相关参数
     *
     * @param picture   需要设置审核参数的图片对象
     * @param loginUser 当前登录用户对象
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        // 检查登录用户是否为管理员
        if (userService.isAdmin(loginUser)) {
            //管理员就自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());//审核人id就用管理员id
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            //不是管理员，则不管图片是新增还是修改完重新上传都需要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }


    /**
     * 编写图片数据校验方法，用于更新和修改图片时候进行判断
     *
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        //修改数据时，id不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");

        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }

        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }

    }


    /**
     * 删除图片和编辑图片都用到这校验方法
     * 校验空间图片的权限 传进来的参数就是图片和当前登录的这个人
     *
     * @param loginUser
     * @param picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long loginUserId = loginUser.getId();//把当前登录用户的id取出来
        Long spaceId = picture.getSpaceId();//把图片所属的空间Id取出来

        if (spaceId == null) {//如果图片所属的空间id为空，就说明图片是在公共图库里面，否则在私人空间
            //公共图库，仅图片作者或管理员可操作，如果当前登录的用户不是图片的作者并且当前登录的用户不是管理员，那么他操作这张图片就会报错
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            //私人空间，仅空间所属的用户可操作
            if (!picture.getUserId().equals(loginUser.getId())) {//如果图片的作者id不等于当前登录用户的id，那么这个人操作这张图片就会报错
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId   图片所属空间ID
     * @param picColor  颜色字符串
     * @param loginUser 当前登录的人
     * @return
     */
    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //2.校验空间权限
        Space spapce = spaceService.getById(spaceId);
        ThrowUtils.throwIf(spapce == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(spapce.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        //3.查询该空间下的所有图片（必须要有主色调）
        List<Picture> pictureList = this.lambdaQuery()//使用当前类的lambda查询构造器
                .eq(Picture::getSpaceId, spaceId)//添加查询条件：图片的空间 id =  传入的spaceId
                .isNotNull(Picture::getPicColor)//添加查询条件：图片的主色调不能为空
                .list();// 执行查询，返回符合条件的图片列表
        //如果没有图片，返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        //将传入的颜色字符串（十六进制）转换为Color对象（使用Color的decode方法将颜色字符串转换成Color对象），每张图片的主色调会与targetColor进行比较
        Color targetColor = Color.decode(picColor);

        //4.计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                //sorted：对流中的元素排序，核心是 Comparator.comparingDouble()
                .sorted(
                        // 对集合中的元素进行排序，需要一个比较器Comparator，使用comparingDouble（Double类型，因为相似度是一个小数），接收一个函数（排序的规则），即picture ->{...}
                        Comparator.comparingDouble(picture -> {
                                    String hexColor = picture.getPicColor();//取出当前图片的十六进制颜色代码
                                    if (StrUtil.isBlank(hexColor)) {
                                        //为空就返回一个极大值，表示这张图片颜色无效排到最后
                                        return Double.MAX_VALUE;
                                    }
                                    //将十六进制颜色代码转回Color对象，方便后续计算相似度
                                    Color pictureColor = Color.decode(hexColor);
                                    //调用工具类计算相似度，值越大表示颜色越接近
                                    //加负号是因为Java的sorted默认升序排序（从小到大，值越大排在越后面），而这里需要的是相似度从高到低排序，即降序，所以加 一个负号 例如 A图片相似度是0.9 B图片是0.8 没加负号前，是B排在A前面，而加了负号变成了A排在B前面
                                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                                }
                        )
                )
                .limit(12) // 取排序后的前12张图片
                .collect(Collectors.toList());
        //返回结果
        return sortedPictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequestDTO
     * @param loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequestDTO pictureEditByBatchRequestDTO, User loginUser) {
        //获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequestDTO.getPictureIdList();
        Long spaceId = pictureEditByBatchRequestDTO.getSpaceId();
        String category = pictureEditByBatchRequestDTO.getCategory();
        List<String> tags = pictureEditByBatchRequestDTO.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {//只能是这个空间的创建人才能使用
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        //查询指定图片，仅根据需要的字段把图片查出来
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)//只查询id和spaceId字段
                .eq(Picture::getSpaceId, spaceId)//Picture 表的 spaceId 字段 等于 变量 spaceId 的值
                .in(Picture::getId, pictureIdList)//Picture 表的 id 字段 包含在 pictureIdList 这个集合中
                .list();
        //等价于 select id,spaceId from picture where spaceId=#{spaceId} and id in (#{id1},#{id2},#{id3})
        if (pictureIdList.isEmpty()) {
            return;
        }
        //遍历每一张图片进行更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });

        //批量重命名
        String nameRule = pictureEditByBatchRequestDTO.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);//调用下面的方法
        //操作数据库进行批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败");
    }

    /**
     * nameRule 格式：图片{序号}
     * 批量重命名的私有方法
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                //将名称规则中的 "{序号}" 占位符替换为当前递增的序号（count++ 先取值再自增）
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }

    }

    /**
     * 创建拓图任务
     *
     * @param createPictureOutPaintingTaskRequestDTO
     * @param loginUser
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequestDTO createPictureOutPaintingTaskRequestDTO, User loginUser) {
        //获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequestDTO.getPictureId();

        /*Optional是Java8引入的容器类，ofNullable(x)会根据x是否为null，包装成Optional或者空的Optional
         * 如果Optional有值即查到了图片，就直接返回这个值，否则执行orElseThrow抛出一个异常*/
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));

        //校验权限，已经改为使用注解鉴权
        //checkPictureAuth(loginUser, picture);
        //构造发起拓图任务所需的请求参数
        CreateOutPaintingTaskRequestDTO createOutPaintingTaskRequestDTO = new CreateOutPaintingTaskRequestDTO();
        CreateOutPaintingTaskRequestDTO.Input input = new CreateOutPaintingTaskRequestDTO.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequestDTO.setInput(input);
        createOutPaintingTaskRequestDTO.setParameters(createPictureOutPaintingTaskRequestDTO.getParameters());

        //调用api创建任务
        CreateOutPaintingTaskResponse outPaintingTask = aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequestDTO);
        return outPaintingTask;
    }
}




