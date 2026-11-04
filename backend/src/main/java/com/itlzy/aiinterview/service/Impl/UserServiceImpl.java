package com.itlzy.aiinterview.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itlzy.aiinterview.dto.RegisterDTO;
import com.itlzy.aiinterview.entity.User;
import com.itlzy.aiinterview.mapper.UserMapper;
import com.itlzy.aiinterview.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean register(RegisterDTO dto) {
        // 规则1：查重（@TableLogic 会自动加 deleted=0 条件，不用手写）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            return false;
        }

        // 组装实体：只 set 前端允许传的 + 默认值（规则3）
        User user = new User();
        user.setUsername(dto.getUsername());
        // 规则2：BCrypt 加密，绝不存明文
        user.setPassword(new BCryptPasswordEncoder().encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(1);

        try {
            userMapper.insert(user);
            return true;
        } catch (DuplicateKeyException e) {
            // 规则4：并发兜底——查重是礼貌拦，uk_username 唯一索引是法律拦
            return false;
        }
    }
}