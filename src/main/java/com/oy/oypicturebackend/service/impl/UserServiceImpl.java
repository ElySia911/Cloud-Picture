package com.oy.oypicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oy.oypicturebackend.constant.UserConstant;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.manager.auth.StpKit;
import com.oy.oypicturebackend.model.dto.user.UserQueryRequestDTO;
import com.oy.oypicturebackend.model.dto.user.UserUpdateMySelfRequestDTO;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.UserRoleEnum;
import com.oy.oypicturebackend.model.vo.LoginUserVO;
import com.oy.oypicturebackend.model.vo.UserVO;
import com.oy.oypicturebackend.service.UserService;
import com.oy.oypicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author ouziyang
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-25 16:34:58
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户id
     */
    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验参数，hasBlank判断多个字符串中是否存在一个字符串为 null或""或" "，只要有一个字符串满足其中一个，就返回true，抛出错误
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        //2.检查用户账号是否和数据库中已有的重复，查询到大于1就代表重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        //baseMapper是UserMapper类型，而UserMapper继承了BaseMapper，即子类继承父类，子类同时拥有了父类的方法
        Long count = this.baseMapper.selectCount(queryWrapper);
        //生成的sql类似：select  count(*) from user where userAccount = 'abc123'
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        //3.密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //4.插入数据进数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("默认昵称");//新注册用户的默认昵称
        user.setUserRole(UserRoleEnum.USER.getValue());//用户的默认权限
        boolean saveResult = this.save(user);//mp提供的save方法
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();//id是数据库进行主键回填得到的，把用户的id返回给前端
    }

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验，hasBlank判断多个字符串中是否存在一个字符串为 null或""或" "，只要有一个字符串满足其中一个，就返回true，抛出错误
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误");
        }
        //2.用户的密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.查询数据库中这个用户是否存在，不存在就抛异常
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed,userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或者密码错误");
        }
        //4.保存用户的登录态，第一次登录的时候，会创建一个Session对象，将用户信息用键值对的形式保存在服务器的Session对象中，以便后续识别“当前登录的用户是谁”，并将唯一的session id返回给浏览器
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        //记录用户登录态到 Sa-token，便于空间鉴权时使用
        StpKit.SPACE.login(user.getId());//让当前用户在space体系下登录
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);//把user对象存到space会话里面
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录的用户，用于service间传输
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //服务器通过请求中的sessionId找到对应的session，再从session中获取存储的用户信息
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        //将session中获取的对象转换为User类型（因为session中存储的是Object类型）
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {// 根据是否查到session 或 id是否为空来判断，因为前端要根据id是否为空来判断是否登录
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //用户登录后，可能会修改个人的昵称，若是通过上面代码获取的还是之前保存的session，获取的还是没有修改前的用户的昵称，所以要重新去数据库查询一次新的用户信息
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");//注销是一个动作，所以这里用 操作失败，未登录
        }
        //移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 脱敏的用户信息，即从User转成LoginUserVO
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获得脱敏后的信息，即从User（未脱敏）转成UserVO（脱敏），这个service是提供给用户根据id获取其他单个用户信息的
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获得脱敏后的用户信息列表，即从User列表（未脱敏）转成UserVO列表（脱敏），这个service是提供给管理员根据id获取多个用户信息
     *
     * @param userList
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserListVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {//CollUtil是处理列表的工具类，而isEmpty是判断列表是否为null或者为空，即size为0
            return new ArrayList<>();
        }
        //将userList转换成一个流，对流中每个user对象调用getUserVO(user)方法，将User转成UserVO，然后将所有UserVO收集成新的List<UserVO>并返回
        return userList.stream().map(user -> getUserVO(user)).collect(Collectors.toList());
        /*由于getUserVO方法是当前类的方法，所以也可以使用lambda写法，
        改写成.map(this::getUserVO).collect(Collectors.toList())*/
    }


    /**
     * 获取查询条件，专门用于将查询请求转为 QueryWrapper 对象
     * 根据前端传入的查询条件，动态构造一个QueryWrapper<User>用于查询用户表 user
     * 返回值是 QueryWrapper<User>：MyBatis-Plus提供的查询构造器，封装了SQL的WHERE和ORDER BY等条件
     *
     * @param userQueryRequestDTO
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequestDTO userQueryRequestDTO) {
        if (userQueryRequestDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        //从DTO中提取查询字段
        Long id = userQueryRequestDTO.getId();
        String userName = userQueryRequestDTO.getUserName();
        String userAccount = userQueryRequestDTO.getUserAccount();
        String userProfile = userQueryRequestDTO.getUserProfile();
        String userRole = userQueryRequestDTO.getUserRole();
        String sortField = userQueryRequestDTO.getSortField();
        String sortOrder = userQueryRequestDTO.getSortOrder();
        //用来构建where条件、like、order by等sql语句片段
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //下面两句是等值查询：isNotNull方法判断id条件是否为空，isNotBlank方法判断userRole是否非空字符串，不为空就添加  where id= ? 方法 或 where userRole = ?
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        //下面三句是模糊查询：isNotBlank方法判断条件是否为空，不为空就添加  LIKE 条件（用于模糊搜索）
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        //下面这一句判断是否指定了排序字段（sortField 非空），然后根据 sortOrder（排序规则） 是否为 "ascend" 来决定是升序还是降序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);//ascend是升序 descen是降序
        return queryWrapper;//返回查询器
    }


    /**
     * 密码加密
     *
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        //加盐就是在原始密码基础上，人为加上一段固定或随机的字符串，再进行加密
        final String SALT = "ozy";
        //把盐和用户密码拼接起来，转成字节数组后，进行MD5加密，得到十六进制字符串
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }


    /**
     * 当前登录用户更新自己个人信息
     *
     * @param userUpdateMySelfRequestDTO
     * @param request
     * @return
     */
    @Override
    public boolean updateMySelf(UserUpdateMySelfRequestDTO userUpdateMySelfRequestDTO, HttpServletRequest request) {
        if (userUpdateMySelfRequestDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取当前用户
        User loginUser = getLoginUser(request);

        // 构造要更新的用户对象
        User userUpdate = new User();
        userUpdate.setId(loginUser.getId());
        userUpdate.setUserName(userUpdateMySelfRequestDTO.getUserName());
        userUpdate.setUserProfile(userUpdateMySelfRequestDTO.getUserProfile());
        userUpdate.setUserAvatar(userUpdateMySelfRequestDTO.getUserAvatar());

        //更新数据库
        boolean result = this.updateById(userUpdate);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败");
        }
        return true;
    }

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        if (UserRoleEnum.ADMIN.getValue().equals(user.getUserRole())) {
            return true;
        }
        return false;
    }


}




