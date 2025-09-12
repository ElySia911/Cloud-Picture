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
import com.oy.oypicturebackend.manager.sharding.DynamicShardingManger;
import com.oy.oypicturebackend.model.dto.space.SpaceAddRequestDTO;
import com.oy.oypicturebackend.model.dto.space.SpaceQueryRequestDTO;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.SpaceLevelEnum;
import com.oy.oypicturebackend.model.enums.SpaceRoleEnum;
import com.oy.oypicturebackend.model.enums.SpaceTypeEnum;
import com.oy.oypicturebackend.model.vo.PictureVO;
import com.oy.oypicturebackend.model.vo.SpaceVO;
import com.oy.oypicturebackend.model.vo.UserVO;
import com.oy.oypicturebackend.service.SpaceService;
import com.oy.oypicturebackend.mapper.SpaceMapper;
import com.oy.oypicturebackend.service.SpaceUserService;
import com.oy.oypicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author ouziyang
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-08-14 16:41:20
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceUserService spaceUserService;

    //关闭分库分表
    //@Resource
    //@Lazy
    //private DynamicShardingManger dynamicShardingManger;

    //定义一个静态线程安全的哈希表，键是Long类型的userId，值是Objec类型的锁对象，确保同一个用户的并发操作会被同一把锁控制
    private static final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();

    /**
     * 创建空间
     *
     * @param spaceAddRequestDTO
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequestDTO spaceAddRequestDTO, User loginUser) {
        //1.填充参数默认值，前端传来的参数拷贝到space实体对象中
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequestDTO, space);

        if (StrUtil.isBlank(space.getSpaceName())) {
            //如果前端传入的空间名称为null、空字符串、纯空格的话，就给个默认名字
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            //空间级别为空就默认设置为普通版
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }

        //根据空间等级填充空间的最大容量和最多存储数量
        this.fillSpaceBySpaceLevel(space);

        //2.校验参数
        this.validSpace(space, true);
        //3.校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);//将用户id和空间关联起来

        //如果用户申请的空间级别不是普通级别，且当前登录用户不是管理员，则抛出无权限异常，保证了只有管理员才能创建非普通级别的空间
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        //4.控制同一用户只能创建一个私有空间、以及一个团队空间
        //String l = String.valueOf(userId).intern();//valueOf方法将userId转为字符串类型后，使用intern方法将字符串放进字符串常量池，并返回字符串在常量池中的引用，如果常量池里已经有内容相同的字符串，就直接返回那个已存在的对象引用
        //computeIfAbsent是lockMap的核心方法，当userId对应的锁不存在时，会执行后面的lambda表达式，创建一个新的Object对象作为锁，并放入map中，如果已存在，直接返回已有的锁对象
        Object l = lockMap.computeIfAbsent(userId, k -> new Object());
        Long newSpaceId;
        synchronized (l) {//对象锁，锁住l指向的对象，只要两个线程的userId一样，它们拿到的都是同一个锁对象引用，synchronized(l)会让它们按顺序执行（一个线程执行完释放锁，另一个才能进）
            try {
                //使用编程式事务的方式，确保了事务的提交是在锁内执行，如果改成使用@Transactional注解的方式就会出现锁先释放，事务后提交的问题
                newSpaceId = transactionTemplate.execute(status -> {

                    //判断是否已有空间，只有userId字段等于当前用户id，且当前用户创建过的空间类型等于当前用户申请创建的空间类型，才返回true
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, space.getSpaceType())
                            .exists();
                    //如果有空间，就不能再创建
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户的每类空间只能创建一个");
                    //否则创建空间
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");

                    //创建完空间，如果这个空间是团队空间，则还需要关联一条空间成员记录
                    if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequestDTO.getSpaceType()) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());//这个发起团队空间创建的人就是管理员角色
                        result = spaceUserService.save(spaceUser);//将这个对象保存到数据库
                        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                    }

/*                    //创建分表（仅对团队空间生效）
                    dynamicShardingManger.createSpacePictureTable(space);*/
                    return space.getId();//返回新创建的空间id

                });
            } finally {
                lockMap.remove(userId);//防止内存泄露
            }
            //如果事务失败或者newSpaceId是null，就返回-1L表示失败；否则返回正常的空间ID
            return Optional.ofNullable(newSpaceId).orElse(-1L);

        }
    }


    /**
     * 校验空间，add是布尔值，区分新增空间和修改空间两种场景，比如新增时必须填名称，修改时可以不填
     * 作用：检查数据是否符合业务规则
     *
     * @param space
     * @param add   CRUD中的“Create/Update”前置校验，新增（Create）或修改（Update）空间时，必须先校验数据合法性（比如名称不为空、容量符合级别限制），否则直接报错。没有这个校验，CRUD 操作可能写入非法数据
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        //从space对象中提取需要校验的字段：空间名称和空间级别
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();

        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);//根据传入的空间的级别获取枚举实例
        Integer spaceType = space.getSpaceType();//空间类型 ：私人或团队
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);//根据空间类型获取到枚举值


        //创建时校验，即当add为true时就进入
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                //判断空间名称是否为空：null 空字符串 纯空格 若是则抛出异常
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        //对空间名称长度进行校验（创建和修改操作都适用），使用isNotBlank确保只对有值的情况进行长度校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        //对空间级别进行校验（创建和修改操作都适用），当空间级别不为null，但无法匹配对应的枚举值时，说明传入的级别数值不在系统的预设范围内，就会抛出异常
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        //修改空间类型时，对空间类型进行校验，但空间类型不为空 时，无法匹配对应的枚举值时，说明传入的级别数值不在系统的预设范围内，就会抛出异常
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }

    }

    /**
     * 获取空间包装类，即获取视图对象 SpaceVO是给前端返回的数据格式，脱敏
     *
     * @param space
     * @param request
     * @return CRUD 中“Read”的结果转换，查询（Read）数据库得到 Space 实体后，不能直接返回给前端（可能包含敏感字段，或格式不符合前端需求），这个方法负责把实体转成前端需要的 SpaceVO（视图对象），是 “查询” 流程的必要步骤
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        //调用objToVo方法，实现对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        //从空间实体中获取创建空间的用户id
        Long userId = space.getUserId();
        //验证userId是否有效
        if (userId != null && userId > 0) {
            //根据space里面的userId，调用getById方法，从数据库查询完整用户实体user
            User user = userService.getById(userId);
            //对查询到的user实体进行脱敏处理并转成UserVO
            UserVO userVO = userService.getUserVO(user);
            //将脱敏的用户信息写进封装类里面
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间包装类（分页），将分页查询到的Space实体列表，批量转换为SpaceVO分页对象
     *
     * @param spacePage
     * @param request
     * @return CRUD 中“Read”的结果转换，查询（Read）数据库得到 Space 实体后，不能直接返回给前端（可能包含敏感字段，或格式不符合前端需求），这个方法负责把实体转成前端需要的 SpaceVO（视图对象），是 “查询” 流程的必要步骤
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        //getRecords方法是从分页对象spacePage中获取当前这一页的Space实体列表数据，包含了当前这一页的数据、页码、总条数
        List<Space> spaceList = spacePage.getRecords();

        //创建一个新的SpaceVO类型分页对象spaceVOPage ，复用原分页对象spacePage的当前页码，每一页条数，总记录数
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());

        //判断列表是否为空，若为空，直接返回空的分页对象
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        //用stream流遍历列表spaceList中每个Space实体，使用SpaceVO类的objToVo方法，将每个Space实体转换为SpaceVO，即脱敏，用一个新的列表存起来
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .collect(Collectors.toList());

        //用stream流遍历列表spaceList，使用Space实体的getUserId方法，提取出每个Space实体所属的用户id，用一个新的集合存起来（set集合去重）
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());

        /*根据userIdSet批量查询用户信息得到一个列表，将列表转换成stream流，groupingBy是分组，
        分组条件是User::getId,就是将用户列表按用户的id分组得到一个Map ：key是用户id  value是该id对应的用户列表
        由于是根据id查询的用户，正常情况下每个id只会对应一个用户，所以这个Map里面每个列表其实只有一个用户*/
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));

        //遍历列表中每个元素，用spaceVO临时变量来表示，获取对应的userId
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            //从Map查一下有没有key等于这个userId的记录，有的话，就取这条记录中第一个元素赋给user
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            //将user进行脱敏后设置给spaceVO的user字段
            spaceVO.setUser(userService.getUserVO(user));
        });
        //将转换好的spaceVOList设置给spaceVOPage分页对象
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 获取查询对象，根据查询条件DTO构建QueryWrapper
     * QueryWrapper是MP的查询条件构造器，用于拼接SQL条件
     * 作用：将前端查询参数转换为数据库查询条件
     *
     * @param spaceQueryRequestDTO
     * @return CRUD 中“Read”的条件构建，前端查询（Read）空间时，会传入各种筛选条件（如按用户 ID、空间名称查询），这个方法把这些条件转换成数据库能理解的查询语句（通过 QueryWrapper），是 “查询” 的前置条件。
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequestDTO spaceQueryRequestDTO) {

        //初始化一个空的查询条件包装器，Space是要查询的实体类
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        //如果前端查询请求DTO为null（即前端没传任何查询条件），直接返回空查询条件
        if (spaceQueryRequestDTO == null) {
            return queryWrapper;
        }

        //从查询请求 DTO 中分别获取 id、用户 id、空间名称、空间等级、排序字段和排序顺序这些参数
        Long id = spaceQueryRequestDTO.getId();
        Long userId = spaceQueryRequestDTO.getUserId();
        String spaceName = spaceQueryRequestDTO.getSpaceName();
        Integer spaceLevel = spaceQueryRequestDTO.getSpaceLevel();
        Integer spaceType = spaceQueryRequestDTO.getSpaceType();
        String sortField = spaceQueryRequestDTO.getSortField();
        String sortOrder = spaceQueryRequestDTO.getSortOrder();

        //拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);//当id不为空时，添加id=?的精确匹配条件
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);// 当userId不为空时，添加userId=?的精确匹配条件，用于查询空间的创建者
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);//当spaceName不为空且不是空白字符串时，添加spaceName LIKE %?%的模糊匹配条件
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);//当spaceLevel不为空时，添加spaceLevel = ?的精确匹配条件，用于查询特定等级的空间
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);//当spaceType不为空时，添加spaceType = ?的精确匹配条件，用于查询特定类型的空间

        //第一个参数是条件判断，判断排序字段sortField是否不为空且不是空白字符串，只有成立时，才会添加排序条件。第二个参数是判断排序顺序是否为ascend升序，若为true，则升序，否则降序。第三个参数是要进行排序的字段
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        //打印一下拼接好的的sql条件，不含from和表名，仅where后的条件
        String sqlSegment = queryWrapper.getCustomSqlSegment();
        System.out.println("【构建好的sql条件：" + sqlSegment + "】");
        //返回构建好的查询条件包装器，用于后续数据库查询
        return queryWrapper;

    }

    /**
     * 根据用户选择的空间级别，为这个空间自动填充最大容量和最大的图片数量
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();//提取出用户选择的空间级别
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);//根据级别拿到对应的枚举实例，里面预定义了该级别的总容量和最大图片数量
        if (spaceLevelEnum != null) {
            //若space的最大容量和最大图片书数量为空，就默认使用枚举中提前设置好的最大容量和最大图片数量
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        //仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




