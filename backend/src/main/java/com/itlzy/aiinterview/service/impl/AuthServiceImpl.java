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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 登录/注销/当前用户：JWT 签发与 Redis 登录存根管理。
 *  @author 林子阳 */
@Service
public class AuthServiceImpl implements AuthService {

    public static final String USER_KEY_PREFIX = "login:user:";  // Redis 登录存根键前缀

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;


    // 登录存根 TTL 与 JWT 过期时间同源
    @Value("${jwt.expire-minutes}")
    private long expireMinutes;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 按用户名查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));

        // 校验密码
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "用户名或密码错误");
        }

        // 签发 token 并覆盖写登录存根（覆盖即踢掉旧设备）
        String token = jwtUtil.createToken(user.getId());
        redisTemplate.opsForValue().set(USER_KEY_PREFIX + user.getId(),
                token, Duration.ofMinutes(expireMinutes));

        // 回执不含密码
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        return vo;
    }

    @Override
    public UserVO me(Long userId) {
        // 按 id 查询用户
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
    public void logout(Long userId) {
        // 删除登录存根；key 不存在时天然幂等
        redisTemplate.delete(USER_KEY_PREFIX + userId);
    }
}