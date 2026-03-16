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

    //根据请求的路径来判断应该给这个上下文类传递什么参数，所以要获取到当前请求的上下文路径，这样才能从URL中获取到需要的业务参数
    @Value("${server.servlet.context-path}")
    private String contextPath;//   这里是/api，具体在yml中进行了规定

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
     * 返回一个账号所拥有的权限列表（权限的总入口）
     * loginType就是不同体系的标识，例如：管理员体系的loginType就是admin,空间体系的loginType就是space
     *
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //1. 判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            //loginType不是space，即不是空间体系，就直接返回空的权限列表
            return new ArrayList<>();
        }

        //2. 调用空间用户权限管理器的方法拿到管理员角色的权限列表
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        //3. 获取权限上下文类对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，表示查询公共图库，可以通过，返回管理员权限列表
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }

        //4. 获取当前登录用户的userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);// 根据登录id获取这个用户在SPACE体系下的会话对象（SaSession），然后从会话里找到键为USER_LOGIN_STATE对应的值（值是一个User对象），由于get()返回的是Object，所以强转为User类型
        if (loginUser == null) {//若为空，说明请求里没有带有效的登录凭证或者带的toke找不到对应的会话，直接抛出错误
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        Long userId = loginUser.getId();

        //5. 从权限上下文authContext中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            // 若SpaceUser对象存在，就根据该成员的角色调用空间用户权限管理器的方法，获取权限列表
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        //6. 从权限上下文authContext中取出spaceUserId字段，如果有spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            //spaceUserId不为空，先根据 spaceUserId 查库，确认这条关联记录存在
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
     * 这个方法不做任何权限判断，用于统一封装当前请求所涉及的空间、图片及成员信息，为后续权限判断逻辑提供必要的业务数据支持。该类本身不参与权限决策，仅承担数据整合与传递职责
     * @return
     */
    public SpaceUserAuthContext getAuthContextByRequest() {
        //第一步：从Spring的上下文工具类RequestContextHolder中，获取当前线程绑定的通用请求属性容器（RequestAttributes接口）
        RequestAttributes RA = RequestContextHolder.getRequestAttributes();

        //将通用属性容器转换为 Web 环境下的具体实现类，为获取HttpServletRequest做准备
        ServletRequestAttributes Sra = (ServletRequestAttributes) RA;

        //从Web专用的属性容器中提取最终的HttpServletRequest对象（包含请求头、参数、路径等所有请求信息）
        HttpServletRequest request = Sra.getRequest();

        //第二步：从当前 HTTP 请求的请求头中，获取 Content-Type 字段的值，这个值用于标识前端发送给后端的 请求体 数据格式（比如 JSON、表单、文件等）
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());

        SpaceUserAuthContext authRequest;//声明一个上下文对象变量，用于后面封装请求数据。

        // 判断请求体是否为JSON格式
        if (ContentType.JSON.getValue().equals(contentType)) {
            //用Hutool提供的ServletUtil工具类，把请求体里的JSON字符串读取出来
            String body = ServletUtil.getBody(request);

            //将 JSON 字符串映射成 上下文类中的属性。
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);

        } else {
            //如果不是JSON格式（例如表单格式），则利用getParamMap(request)获取请求中的所有参数（包括 URL 查询参数和表单参数），并转换为 Map。
            Map<String, String> paramMap = ServletUtil.getParamMap(request);

            //toBean方法会把上面的Map键值对映射到SpaceUserAuthContext对象的字段中
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }//到这里为止，无论前端传JSON还是其他格式的数据，都被统一转换成 上下文类

        //第三步：根据请求路径区分id字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {

            //获取当前请求的路径（不包含域名和参数）例： /api/space/123/edit
            String requestURI = request.getRequestURI();

            //去掉contextPath前缀，contextPath是/api 所以contextPath + "/" 就是/api/  用replace去掉后，partURI为space/123/edit
            String partURI = requestURI.replace(contextPath + "/", "");

            //subBefore方法，从partURI中截取第一个 分隔符 之前的部分，false表示不保留分隔符，moduleName为space
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
        //如果传入的对象本身就是null，直接返回true（认为"所有字段为空"）
        if (object == null) {
            return true;
        }

        return Arrays.stream(ReflectUtil.getFields(object.getClass()))   //获取对象所属类的所有字段（仅当前类，不含父类），并转为流

                .map(field -> ReflectUtil.getFieldValue(object, field))  //遍历这个对象的每一个字段，获取每一个字段的值

                .allMatch(ObjectUtil::isEmpty); //检查所有字段的值是否都为空，若是就返回true 否则false
    }

}
