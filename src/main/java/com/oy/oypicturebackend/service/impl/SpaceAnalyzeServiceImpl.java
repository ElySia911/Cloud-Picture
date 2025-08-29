package com.oy.oypicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.mapper.SpaceMapper;
import com.oy.oypicturebackend.model.dto.space.analyze.*;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.space.analyze.*;
import com.oy.oypicturebackend.service.PictureService;
import com.oy.oypicturebackend.service.SpaceAnalyzeService;
import com.oy.oypicturebackend.service.SpaceService;
import com.oy.oypicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.LinkedHashMap;
import java.util.List;

import java.util.Map;
import java.util.stream.Collectors;


@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;

    /**
     * 分析空间资源使用情况（获取空间使用情况分析）
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequestDTO spaceUsageAnalyzeRequest, User loginUser) {
        //校验参数
        //全空间或公共图库，只要有一个为真，就进入if分支，需要从Picture表查询
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            //权限校验，把查询的范围请求和当前登录用户传进去，让它决定是否放行。若查询的范围请求是全空间/公共图库,这是管理员仅有的
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //统计图库的使用空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");//只查询图片体积这一列
            //补充查询范围
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);//根据请求对象封装查询条件
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);//只查一列时，用selectObjs合适，省去了映射为实体的开销，返回的元素是那一列的值，这里是picSize
            //统计
            long usedSize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();//通过流将对象转为long类型，再求和
            long usedCount = pictureObjList.size();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);//全空间容量的已使用大小
            spaceUsageAnalyzeResponse.setUserCount(usedCount);//全空间的图片数量
            //全空间无数量和容量限制，也没有空间使用比例
            spaceUsageAnalyzeResponse.setMaxSize(null);//全空间容量总大小
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);//全空间使用比例
            spaceUsageAnalyzeResponse.setMaxCount(null);//全空间最大图片数量
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);//全空间图片数量使用比例
            return spaceUsageAnalyzeResponse;
        } else {
            //特定空间直接从Space表查询
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            //获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //权限校验：仅空间所有者或管理员可访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());//空间容量的已使用大小
            spaceUsageAnalyzeResponse.setUserCount(space.getTotalCount());//空间有多少张图片
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());//空间容量总大小
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());//空间最大图片数量
            //计算空间容量的使用比例和图片数量的使用比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();//保留两位小数
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();//保留两位小数
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);//空间容量使用比例
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);//空间图片数量使用比例
            return spaceUsageAnalyzeResponse;


        }
    }

    /**
     * 分析空间分类图片（获取空间图片分类分析）
     *
     * @param spaceCategoryAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequestDTO spaceCategoryAnalyzeRequestDTO, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequestDTO, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequestDTO, queryWrapper);

        //使用MP分组查询 select category as category,count(*) as count,sum(picSize) as totalSize from picture group by category
        //从picture表里，取出category这一列，别名为category，统计同一分类的记录条数，统计同一分类的picSize图片体积总和，按category进行分组
        queryWrapper.select("category as category", "count(*) as count", "sum(picSize) as totalSize").groupBy("category");

        //查询，selectMaps返回的是一个Map类型的List，通过流的方式，对List中每一条记录根据 键 取 值
        List<SpaceCategoryAnalyzeResponse> list = pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {//result是一个Map
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                }).collect(Collectors.toList());
        return list;
    }

    /**
     * 分析空间标签图片（获取空间标签分析）
     *
     * @param spaceTagAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequestDTO spaceTagAnalyzeRequestDTO, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequestDTO, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequestDTO, queryWrapper);

        //查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)//selectObjs返回的是一个Object类型的List
                .stream()
                .filter(ObjUtil::isNotNull)//过滤掉null值
                .map(Object::toString)//将Object类型元素转为String类型
                .collect(Collectors.toList());//收集成一个List

        // 合并所有标签并统计各个标签的使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                //借助JSONUtil.toList将JSON数组tagsJson解析为List<String>,例如 tagsJson是["艺术"]时解析得到一个List，List里面存储了"艺术"
                //flatMap是扁平化处理，拆分所有列表中每一个元素，把拆出来的标签放成一堆
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())

                //Collectors.groupingBy用于分组统计，第一个参数tag->tag是分类参数，按标签本身分组
                //第二个参数Collectors.counting()是收集器，用于计算每个元素的数量 最终得到一个键为标签，值为出现次数的Map
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        //转换为响应对象，按使用次数降序排序
        return tagCountMap.entrySet().stream()//将Map里面的键值对取出来变成一个Set<Map.Entry>集合，将集合转换成流，此时流里的元素是一个个Map.Entry对象，
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))//按值降序排序，e1和e2是流里的两个元素，通过getValue比较次数
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))//把流里的每一个元素转换为响应对象，用标签名和次数作为参数
                .collect(Collectors.toList());
        /*将Map转成Set是因为Map本身并不提供键值对的遍历接口，只能通过entrySet()拿到键值对整体,
        而entrySet之所以返回一个Set是因为Map的键是唯一的，而Set集合的特性就是元素唯一，和Map键值对的唯一性匹配*/
    }

    /**
     * 分析空间图片大小
     *
     * @param spaceSizeAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequestDTO spaceSizeAnalyzeRequestDTO, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequestDTO, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequestDTO, queryWrapper);
        //查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizesList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())//将Object类型元素转为Long类型
                .collect(Collectors.toList());

        //定义分段范围，注意使用有序的Map
        Map<String, Long> sizeRangeCountMap = new LinkedHashMap<>();
        sizeRangeCountMap.put("<100KB", picSizesList.stream().filter(size -> size < 100 * 1024).count());//过滤<100KB的图片数量，只保留<100KB的图片
        sizeRangeCountMap.put("100KB-500KB", picSizesList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());//过滤100KB-500KB的图片数量，只保留100KB-500KB的图片
        sizeRangeCountMap.put("500KB-1MB", picSizesList.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());//过滤500KB-1MB的图片数量，只保留500KB-1MB的图片
        sizeRangeCountMap.put(">1MB", picSizesList.stream().filter(size -> size >= 1 * 1024 * 1024).count());//过滤>1MB的图片数量，只保留>1MB的图片

        //转换为响应对象
        return sizeRangeCountMap.entrySet().stream()//将Map里面的键值对取出来变成一个Set<Map.Entry>集合，将集合转换成流，此时流里的元素是一个个Map.Entry对象，
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))//把流里的每一个元素转换为响应对象，用范围和图片数量作为参数
                .collect(Collectors.toList());
    }

    /**
     * 分析空间用户在某个日期区间上传行为分析
     *
     * @param spaceUserAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequestDTO spaceUserAnalyzeRequestDTO, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        //检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequestDTO, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequestDTO.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequestDTO, queryWrapper);

        //补充分析日期维度：日 周 月
        String timeDimension = spaceUserAnalyzeRequestDTO.getTimeDimension();
        switch (timeDimension) {
            case "day":
                //DATE_FORMAT是MySql的日期格式化函数，createTime是表中的创建时间字段，%Y-%m-%d是格式化模板，将日期转换为"年-月-日"的形式，as period是给这个格式化后的字段起一个别名
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;

            case "week":
                //YEARWEEK是MySql的日期函数，用于获取日期对应的年份+周数 例如2023年第一周返回202301，第十周返回202310，createTime是表中的创建时间字段，as period是给这个格式化后的字段起一个别名
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;

            case "month":
                //DATE_FORMAT是MySql的日期格式化函数，createTime是表中的创建时间字段，%Y-%m是格式化模板，将日期转换为"年-月"的形式，as period是给这个格式化后的字段起一个别名
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;

            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        //分组排序 按时间周期period分组统计，然后按时间升序排列
        queryWrapper.groupBy("period").orderByAsc("period");

        //查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                }).collect(Collectors.toList());
    }


    /**
     * 空间使用排行分析
     *
     * @param spaceRankAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequestDTO spaceRankAnalyzeRequestDTO, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        //检查权限，仅管理员可用查看
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        //构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequestDTO.getTopN());//取前N名

        return spaceService.list(queryWrapper);

    }


    //-----------------------------------------------------------------

    /**
     * 校验是否具有，分析空间的权限的方法
     *
     * @param spaceAnalyzeRequestDTO
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequestDTO spaceAnalyzeRequestDTO, User loginUser) {

        boolean queryPublic = spaceAnalyzeRequestDTO.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequestDTO.isQueryAll();
        //全空间分析或者公共图库权限校验，仅管理员可用
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            //分析特定空间，仅本人或者管理员可用访问
            Long spaceId = spaceAnalyzeRequestDTO.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
        }

    }

    /**
     * 根据请求对象封装查询条件
     *
     * @param spaceAnalyzeRequestDTO 通用的，分析空间资源使用情况的请求DTO
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequestDTO spaceAnalyzeRequestDTO, QueryWrapper<Picture> queryWrapper) {


        boolean queryAll = spaceAnalyzeRequestDTO.isQueryAll();
        //全部空间分析：公共图库+全部空间
        if (queryAll) {
            //直接return，没有往queryWrapper里加任何条件，在MP里，QueryWrapper没有条件=查询整张表的所有数据
            return;
        }
        //公共图库
        boolean queryPublic = spaceAnalyzeRequestDTO.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");//空间id必须为null，因为是查公共图库，条件就是没有空间id的图片
            return;
        }
        //分析特定空间
        Long spaceId = spaceAnalyzeRequestDTO.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");

    }


}




