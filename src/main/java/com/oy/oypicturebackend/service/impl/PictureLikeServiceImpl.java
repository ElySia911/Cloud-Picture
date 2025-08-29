package com.oy.oypicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oy.oypicturebackend.exception.ErrorCode;
import com.oy.oypicturebackend.exception.ThrowUtils;
import com.oy.oypicturebackend.model.dto.picture.PictureLikeRequestDTO;
import com.oy.oypicturebackend.model.entity.Picture;
import com.oy.oypicturebackend.model.entity.PictureLike;
import com.oy.oypicturebackend.model.entity.User;
import com.oy.oypicturebackend.service.PictureLikeService;
import com.oy.oypicturebackend.mapper.PictureLikeMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ouziyang
 * @description 针对表【picture_like(图片点赞记录表)】的数据库操作Service实现
 * @createDate 2025-08-24 11:27:58
 */
@Service
public class PictureLikeServiceImpl extends ServiceImpl<PictureLikeMapper, PictureLike> implements PictureLikeService {

    /**
     * 用户对图片进行点赞
     *
     * @param pictureLikeRequestDTO
     * @param loginUser
     */
    @Override
    public void likePicture(PictureLikeRequestDTO pictureLikeRequestDTO, User loginUser) {
        //校验前端传过来的参数
        Long pictureId = pictureLikeRequestDTO.getPictureId();
        String pictureName = pictureLikeRequestDTO.getPictureName();
        ThrowUtils.throwIf(pictureId == null || pictureName == null, ErrorCode.PARAMS_ERROR);

        //校验是否已经点过赞
        PictureLike alreadyLiked = this.lambdaQuery()
                .eq(PictureLike::getPictureId, pictureId)
                .eq(PictureLike::getUserId, loginUser.getId())
                .one();
        //上面的校验等价于sql语句
        //SELECT * FROM picture_like WHERE picture_id = #{pictureId} AND user_id = #{userId}

        ThrowUtils.throwIf(alreadyLiked != null, ErrorCode.OPERATION_ERROR, "你已经点过赞了");

        //保存点赞记录
        PictureLike pictureLike = new PictureLike();
        pictureLike.setUserId(loginUser.getId());
        pictureLike.setPictureId(pictureId);
        pictureLike.setPictureName(pictureName);
        this.save(pictureLike);


    }

    /**
     * 获取用户点赞过的图片id列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<Long> listMyLikedPictureIds(Long userId) {
        //构造查询条件
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);

        //查询该用户点赞记录
        List<PictureLike> likeList = this.list(queryWrapper);
        //SELECT * FROM picture_like WHERE user_id = #{userId};
        List<Long> likedPictureIdList = new ArrayList<>();
        for (PictureLike pictureLike : likeList) {
            Long pictureId = pictureLike.getPictureId();
            if (pictureId != null && pictureId > 0) {
                likedPictureIdList.add(pictureId);
            }
        }
        return likedPictureIdList;
    }
}




