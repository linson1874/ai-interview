package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.dto.RegisterDTO;
import com.itlzy.aiinterview.entity.User;
import com.itlzy.aiinterview.mapper.UserMapper;
import com.itlzy.aiinterview.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void register(RegisterDTO dto) {
        // 规则1：查重（@TableLogic 会自动加 deleted=0 条件，不用手写）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "用户名已存在");
        }

        // 组装实体：只 set 前端允许传的 + 默认值（规则3）
        User user = new User();
        user.setUsername(dto.getUsername());
        // 规则2：BCrypt 加密，绝不存明文
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(1);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 规则4（裁决#2升为必做）：并发兜底——查重是礼貌拦，uk_username 唯一索引是法律拦
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "用户名已存在");
        }
    }
}