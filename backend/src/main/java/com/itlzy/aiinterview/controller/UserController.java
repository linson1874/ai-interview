package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.LoginDTO;
import com.itlzy.aiinterview.dto.RegisterDTO;
import com.itlzy.aiinterview.service.AuthService;
import com.itlzy.aiinterview.service.UserService;
import com.itlzy.aiinterview.vo.LoginVO;
import com.itlzy.aiinterview.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService){
        this.userService = userService;
        this.authService = authService;
    }

    //用户注册：成功就 ok；业务失败由 Service 抛异常，全局 handler 翻译装盘（Controller 不再判 boolean）
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto){
        userService.register(dto);
        return Result.ok("ok");
    }

    //用户登录
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto){
        return Result.okData(authService.login(dto));
    }

    //当前用户信息
    @GetMapping("/me")
    public Result<UserVO> me(@RequestAttribute("userId") Long userId){
        return Result.okData(authService.me(userId));
    }

    //注销
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String auth) {
        authService.logout(auth.substring(7));   // 剥掉 "Bearer "
        return Result.ok("ok");
    }
}
