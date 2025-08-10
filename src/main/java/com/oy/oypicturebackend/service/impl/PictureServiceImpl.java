package com.oy.oypicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.manager.FileManager;
import com.oy.oypicturebackend.manager.upload.FilePictureUpload;
import com.oy.oypicturebackend.manager.upload.PictureUploadTemplate;
import com.oy.oypicturebackend.manager.upload.UrlPictureUpload;
import com.oy.oypicturebackend.model.dto.file.UploadPictureResult;
import com.oy.oypicturebackend.model.dto.picture.PictureQueryRequestDTO;
import com.oy.oypicturebackend.model.dto.picture.PictureReviewRequestDTO;
import com.oy.oypicturebackend.model.dto.picture.PictureUploadByBatchRequestDTO;
import com.oy.oypicturebackend.model.dto.picture.PictureUploadRequestDTO;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.PictureReviewStatusEnum;
import com.oy.oypicturebackend.model.vo.PictureVO;
import com.oy.oypicturebackend.model.vo.UserVO;
import com.oy.oypicturebackend.service.PictureService;
import com.oy.oypicturebackend.mapper.PictureMapper;
import com.oy.oypicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private FilePictureUpload filePictureUpload;
    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 上传图片
     *
     * @param inputSource             文件输入源
     * @param pictureUploadRequestDTO id用于修改
     * @param loginUser               用户，判断用户是否有权限上传
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequestDTO pictureUploadRequestDTO, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //判断是新增还是删除，若DTO为null，则pictureId为null，是新增，若DTO不为null，则提取其中pictureId，表示可能是更新操作
        Long pictureId = null;
        if (pictureUploadRequestDTO != null) {
            pictureId = pictureUploadRequestDTO.getId();
        }
        //若id不为空，（尝试更新图片），根据这个pictureId去数据库中查询有没有这条记录，防止传入无效的id，防止更新不存在的图片
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);//从数据库把这张图片查出来
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            //仅当前这一张图片的作者或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        String uploadPathPrefix = String.format("public/%s", loginUser.getId());//路径前缀，例如 public/1001

        //根据输入源inputSource的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;//先让模板默认使用本地图片上传的方式
        if (inputSource instanceof String) {//如果输入源是Url ，就换成使用url上传图片的方式
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //将上传结果的图片信息转换成Picture实体类存入数据库
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        //支持将图片上传后得到的图片名改为自定义的图片名
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequestDTO != null && StrUtil.isNotBlank(pictureUploadRequestDTO.getPicName())){
            picName=pictureUploadRequestDTO.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());//体积
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());//宽高比
        picture.setPicFormat(uploadPictureResult.getPicFormat());//图片格式
        picture.setUserId(loginUser.getId());
        //不管是新增还是修改更新，只要不是管理员就需要审核，fillReviewParams方法内部会根据登录用户的身份进行图片状态的审核设置
        this.fillReviewParams(picture, loginUser);
        //操作数据库，如果pictureId不为空，表示更新，否则是新增
        if (pictureId != null) {
            //如果是更新，需要补充id和编辑时间，若是不补充id，saveOrUpdate方法会把它当作新记录插入导致重复数据
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        boolean result = this.saveOrUpdate(picture);//这个方法内部会根据picture的id是否为空来决定执行新增还是修改
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
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
        //如果前端查询请求DTO为null（即前端没传任何查询条件），直接返回空的查询条件
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
        Date reviewTime = pictureQueryRequestDTO.getReviewTime();//审核时间

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
     * 获取单个图片封装
     * 接收Picture实体和请求对象，返回封装后的PictureVO
     *
     * @param picture
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
        //从分页对象picturePage中获取当前这一页的记录，并用列表存起来
        List<Picture> pictureList = picturePage.getRecords();

        //创建一个新的分页VO对象，用来装PictureVO类型数据，这个分页对象复制原分页对象的 当前页码、每页显示多少条数据、总共多少条数据
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());

        //如果当前这一页没数据，即列表里面是空的，直接返回空的分页VO对象
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        //将存储了Picture类型数据的列表通过stream流的方式，遍历列表中每一个元素，调用objToVo方法进行转换，得到一个存储PictureVO类型数据的列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)//等价于：每个picture -> PictureVO.objToVo(picture)
                .collect(Collectors.toList());

        /*将存储了Picture类型数据的列表通过stream流的方式，遍历列表中每一个元素，
        调用getUserId方法获取每张图片的上传者的id，用一个Long类型的Set集合存起来，由于Set集合不允许有重复的元素，
        所以最终得到的是去重后的用户id集合*/
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());

        /*先根据userIdSet里的id，查询出这些id对应的用户信息，得到一个用户列表
         * 将用户列表通过stream流的方式处理
         * groupingBy是分组，分组条件是User::getId,就是将用户列表按用户的id分组，
         * 分组后会得到一个Map，key=userId  value是一个列表，列表里面装着id等于这个键的所有用户
         * 由于是根据id查询的用户，正常情况下每个id只会对应一个用户，所以这个Map里面每个列表其实只有一个用户
         * */
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

    /**
     * 编写图片数据校验方法，用于更新和修改图片时候进行判断
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
        String namePrefix = pictureUploadByBatchRequestDTO.getNamePrefix();//图片名称前缀
        if (StrUtil.isBlank(namePrefix)){
            //若图片名称前缀为空，将图片名称前缀设置为搜索词
            namePrefix=searchText;
        }

        //用搜索词拼URL组装要抓取的地址
        String fetchUrl = String.format("https://www.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;

        //抓取页面
        try {
            //拉回HTML，失败就抛业务异常
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败，", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        //定位结果区域，找到结果容器div，空则报错
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }

        //选出图片节点
        Elements imgElementList = div.select("img.mimg");

        int uploadCount = 0;//定义一个变量用于记录上传成功的数量

        //遍历元素，依次上传图片
        for (Element imgElement : imgElementList) {
            //从每个img取src，空则跳过
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }

            //处理每一张图片地址，防止转义或者和腾讯云对象存储冲突的问题，例如 codefather.cn?ozy=dog,应该只保留codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");//找到?的下标，若大于-1就更新src
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //定义上传图片方法所需参数
            PictureUploadRequestDTO pictureUploadRequestDTO = new PictureUploadRequestDTO();
            pictureUploadRequestDTO.setFileUrl(fileUrl);
            pictureUploadRequestDTO.setPicName(namePrefix+(uploadCount+1));

            //尝试上传，成功则自增，失败则跳过，达到count就结束，并返回成功的数量
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequestDTO, loginUser);
                log.info("抓取的图片上传成功，id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("抓取的图片上传失败");
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }


    /*-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/

    /**
     * 填充审核参数：待审核  审核通过  拒绝
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
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


}




