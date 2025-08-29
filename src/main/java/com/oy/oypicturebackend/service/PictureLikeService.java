package com.oy.oypicturebackend.service;

import com.oy.oypicturebackend.model.dto.picture.PictureLikeRequestDTO;
import com.oy.oypicturebackend.model.entity.PictureLike;
import com.baomidou.mybatisplus.extension.service.IService;
import com.oy.oypicturebackend.model.entity.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author ouziyang
 * @description 针对表【picture_like(图片点赞记录表)】的数据库操作Service
 * @createDate 2025-08-24 11:27:58
 */
public interface PictureLikeService extends IService<PictureLike> {

    /**
     * 点赞图片
     */
    void likePicture(PictureLikeRequestDTO pictureLikeRequestDTO, User loginUser);

    /**
     * 获取用户点赞过的图片id列表
     *
     * @param loginUser
     * @return
     */
    List<Long> listMyLikedPictureIds(Long userId);
}
