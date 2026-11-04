package com.itlzy.aiinterview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itlzy.aiinterview.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
