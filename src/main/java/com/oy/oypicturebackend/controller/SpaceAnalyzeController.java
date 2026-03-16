package com.oy.oypicturebackend.controller;

import com.oy.oypicturebackend.common.BaseResponse;
import com.oy.oypicturebackend.common.ResultUtils;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.model.dto.space.analyze.*;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.space.analyze.*;
import com.oy.oypicturebackend.service.SpaceAnalyzeService;
import com.oy.oypicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {
    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    private UserService userService;

    /**
     * 获取空间资源的使用情况
     *
     * @param spaceUsageAnalyzeRequestDTO
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(@RequestBody SpaceUsageAnalyzeRequestDTO spaceUsageAnalyzeRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeResponse response = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequestDTO, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 获取空间的 图片分类 分析
     *
     * @param spaceCategoryAnalyzeRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeRequestDTO spaceCategoryAnalyzeRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeResponse> response = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequestDTO, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 获取空间的 图片标签 分析
     *
     * @param spaceTagAnalyzeRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeRequestDTO spaceTagAnalyzeRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeResponse> response = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequestDTO, loginUser);
        return ResultUtils.success(response);
    }


    /**
     * 获取空间的 图片大小 分析
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeRequestDTO spaceSizeAnalyzeRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeResponse> response = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequestDTO, loginUser);
        return ResultUtils.success(response);
    }


    /**
     * 获取用户在某个日期区间上传行为分析
     * @param spaceUserAnalyzeRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeRequestDTO spaceUserAnalyzeRequestDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeResponse> response = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequestDTO, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 获取空间排行榜
     * @param spaceRankAnalyzeRequestDTO
     * @param request
     * @return
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>>getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeRequestDTO spaceRankAnalyzeRequestDTO, HttpServletRequest request){
        ThrowUtils.throwIf(spaceRankAnalyzeRequestDTO == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<Space> response = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequestDTO, loginUser);
        return ResultUtils.success(response);
    }
}
