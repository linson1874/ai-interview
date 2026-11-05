package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.dto.LoginDTO;
import com.itlzy.aiinterview.entity.User;
import com.itlzy.aiinterview.mapper.UserMapper;
import com.itlzy.aiinterview.service.AuthService;
import com.itlzy.aiinterview.util.JwtUtil;
import com.itlzy.aiinterview.vo.LoginVO;
import com.itlzy.aiinterview.vo.UserVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthServiceImpl implements AuthService {

    public static final String TOKEN_KEY_PREFIX = "login:token:";  // 合同1约定的key格式

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;   // data-redis  starter 自带，application-dev.yml 里有连接配置

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 规则1：按用户名查
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));

        // 规则2：matches(明文, 密文)
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "用户名或密码错误");
        }

        // 规则3：签票 + 留存根（双保险：JWT 2h 自带过期，Redis TTL 2h 管"可注销"）
        String token = jwtUtil.createToken(user.getId());
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token,
                String.valueOf(user.getId()), Duration.ofHours(2));

        // 规则4：回执只给 token+nickname，password 从头到尾不在候选名单上
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        return vo;
    }

    @Override
    public UserVO me(Long userId) {
        // 拦截器已验过票，这里只管按 id 查人（selectById 也自动带 deleted=0）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或凭证失效");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        return vo;
    }

    @Override
    public void logout(String token) {
        // 合同4：撕存根。key 不存在 delete 也返回 false 不报错——天然幂等，无需判存在
        redisTemplate.delete(TOKEN_KEY_PREFIX + token);
    }
}