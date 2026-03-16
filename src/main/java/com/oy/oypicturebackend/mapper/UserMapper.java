package com.oy.oypicturebackend.mapper;

import com.oy.oypicturebackend.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @author ouziyang
 * @description 针对表【user(用户)】的数据库操作Mapper
 * @createDate 2025-07-25 16:34:58
 * @Entity com.oy.oypicturebackend.model.entity.User
 */
public interface UserMapper extends BaseMapper<User> {


    /**
     * UserMapper继承了BaseMapper
     * 而我自己的服务实现类继承了ServiceImpl，ServiceImpl继承了CrudRepository，在CrudRepository中，完成了针对泛型M（如UserMapper）的注入
     * 所以在我自己的服务实现类可以不用再次注入，就能使用来自BaseMapper的CRUD方法
     *
     */

}




