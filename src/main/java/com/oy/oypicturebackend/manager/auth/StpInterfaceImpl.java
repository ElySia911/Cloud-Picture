package com.oy.oypicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.oy.oypicturebackend.exception.BusinessException;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.SpaceUser;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.enums.SpaceRoleEnum;
import com.oy.oypicturebackend.model.enums.SpaceTypeEnum;
import com.oy.oypicturebackend.service.PictureService;
import com.oy.oypicturebackend.service.SpaceService;
import com.oy.oypicturebackend.service.SpaceUserService;
import com.oy.oypicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.oy.oypicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 * 实现了Sa-Token的权限加载接口
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    //根据请求的路径来判断应该给这个上下文传递什么参数，所以要获取到当前请求的上下文路径，这样才能从URL中获取到需要的业务参数
    @Value("${server.servlet.context-path}")
    private String contextPath;//这里是/api 具体在yml中进行了规定

    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 返回一个账号所拥有的权限列表
     * loginType就是不同体系的标识，例如：管理员体系的loginType就是admin,空间体系的loginType就是space
     * 这个方法是供注解内部来调用的
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //1. 判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            //loginType不是space，即不是空间体系，就直接返回空的权限列表
            return new ArrayList<>();
        }
//-----------
        //2. 调用空间用户权限管理器的方法拿到管理员角色的权限列表
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
//-----------
        //3. 获取权限上下文类对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，表示查询公共图库，可以通过，返回管理员权限列表
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }
//-----------
        //4. 获取当前登录用户的userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        //   根据登录id获取这个用户在SPACE体系下的会话对象（SaSession），然后从会话里找到键为USER_LOGIN_STATE对应的值（值是一个User对象），由于get()返回的是Object，所以强转为User类型
        if (loginUser == null) {//若为空，说明请求里没有带有效的登录凭证或者带的toke找不到对应的会话，直接抛出错误
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();
//-----------
        //5. 从权限上下文authContext中获取 SpaceUser 对象，即拿出当前用户在该空间下的成员信息
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            // 若SpaceUser对象存在，就根据该成员的角色调用空间用户权限管理器的方法，获取权限列表
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
//-----------
        //6. 从权限上下文authContext中取出spaceUserId字段，如果有spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            //spaceUserId不为空，就根据这个id查询数据库有没有这条记录
            spaceUser = spaceUserService.getById(spaceUserId);

            if (spaceUser == null) {//如果没这条记录，就抛异常
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 否则就是有这条记录，但有这条记录，并不代表“当前登录用户”就是这条记录对应的成员，还需要查出当前登录用户是不是这个空间的其中一员
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())//where spaceId=这条记录的spaceId
                    .eq(SpaceUser::getUserId, userId)//where userId=当前登录用户的id
                    .one();
            //如果查不到，说明当前登录用户不是该空间的成员，返回一个空的权限列表
            if (loginSpaceUser == null) {
                //但有可能当前用户是系统的管理员，系统管理员不会在space_user表里建成员记录，所以需要返回管理员权限
                if (userService.isAdmin(loginUser)) {
                    return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
                }
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        //上述这部分逻辑就是： 从上下文取spaceUserId————>查数据库验证这条记录存在————>确认当前登录用户是否属于该空间————>不是则返回空列表————>是则根据角色返回权限
//-----------
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            // 否则根据图片 id 查询图片信息 sql： select id,spaceId,userId from picture where id = ?
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();//从图片信息中获取图片所属的空间id
            // 公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
//-----------
        // spaceId不为空，则获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                //判断这个空间记录的创建人id是否等于当前登录用户的id 或者 判断当前登录用户是否为管理员
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询SpaceUser，通过spaceId和userId查询出这条记录，然后根据这条记录的spaceRole字段来决定返回对应的权限
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }


    }


    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {

        return new ArrayList<>();
    }

//--------------------------------------------------------------------------------------------------------------------//

    /**
     * 从前端发来的请求中获取上下文对象
     * 说白了就是不管前端用什么格式发数据，不管参数是通过URL还是请求体发送，这个方法都会把这些数据整理好得到一个SpaceUserAuthContext对象
     * SpaceUserAuthContext（空间用户授权上下文类）
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        //第一步：先获取当前请求对象
        RequestAttributes RA = RequestContextHolder.getRequestAttributes();//拿到当前请求在Spring中的属性容器，里面包含了请求的各种信息。

        //ServletRequestAttributes 是 RequestAttributes 接口的一个实现类，专门用于封装 Servlet 环境下的请求信息
        ServletRequestAttributes Sra = (ServletRequestAttributes) RA;//将通用属性容器转换为Servlet环境下的属性容器，为获取HttpServletRequest做准备

        HttpServletRequest request = Sra.getRequest();//从属性容器中获取到HttpServletRequest对象，这个对象包含了请求的各种信息，包括请求路径，请求参数等等

        //第二步：从请求头里取出Content-Type，用来判断前端发来的请求是什么格式
        // 例如："application/json" "application/x-www-form-urlencoded" "multipart/form-data"
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());

        SpaceUserAuthContext authRequest;//上下文类

        //获取请求参数
        if (ContentType.JSON.getValue().equals(contentType)) {
            //判断请求参数contentType是否为JSON格式，如果是，利用Hutool提供的ServletUtil工具类，把请求体里的JSON字符串读取出来
            String body = ServletUtil.getBody(request);

            //转换成需要的上下文类
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);

        } else {
            //如果不是JSON格式，则利用getParamMap(request)会把请求中的所有参数(包括URL上的查询参数和表单提交的参数)收集成一个Map<String,String>
            Map<String, String> paramMap = ServletUtil.getParamMap(request);

            //toBean方法会把上面的Map键值对映射到SpaceUserAuthContext对象的字段中
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }

        //第三步：根据请求路径区分id字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {

            //先拿到完整的路径，例如/api/space/123/edit
            String requestURI = request.getRequestURI();

            //去掉contextPath前缀，contextPath是/api 所以contextPath + "/" 就是/api/  用replace去掉后，剩space/123/edit
            String partURI = requestURI.replace(contextPath + "/", "");

            //在指定字符第一次出现的位置，截取它前面的字符串，false表示不保留分隔符 / 本身，截取后得到的moduleName是space
            String moduleName = StrUtil.subBefore(partURI, "/", false);

            //根据不同的业务模块，把id赋值给不同的字段
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);//如果moduleName是picture，代表是图片业务，则这个临时id属于图片id
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);//如果moduleName是spaceUser，代表是空间成员业务，则这个临时id属于空间成员id
                    break;
                case "space":
                    authRequest.setSpaceId(id);//如果moduleName是space，代表是空间业务，则这个临时id属于空间id
                    break;
                default:
            }
        }

        return authRequest;

    }


    /**
     * 判断对象的所有字段是否为空
     *
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // ReflectUtil.getFields()是Hutool工具类，返回这个类里所有字段，然后把字段数组转成Stream流，即拿到对象的所有属性
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值，用field临时变量代表流中当前正在处理的那个字段，使用Hutool的反射工具，取出object这个对象在当前字段上的值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

}
