package com.oy.oypicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.oy.oypicturebackend.model.dto.space.analyze.*;
import com.oy.oypicturebackend.model.entity.Space;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.model.vo.space.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 分析空间资源使用情况（获取空间使用情况分析）
     *
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequestDTO spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 分析空间分类图片（获取空间图片分类分析）
     *
     * @param spaceCategoryAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequestDTO spaceCategoryAnalyzeRequestDTO, User loginUser);

    /**
     * 分析空间标签图片（获取空间标签分析）
     *
     * @param spaceTagAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequestDTO spaceTagAnalyzeRequestDTO, User loginUser);

    /**
     * 分析空间图片大小
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequestDTO spaceSizeAnalyzeRequestDTO, User loginUser);

    /**
     * 分析空间用户在某个日期区间上传行为分析
     *
     * @param spaceUserAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequestDTO spaceUserAnalyzeRequestDTO, User loginUser);

    /**
     * 空间使用排行分析（管理员）
     * @param spaceRankAnalyzeRequestDTO
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequestDTO spaceRankAnalyzeRequestDTO, User loginUser);
}
