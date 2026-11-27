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

/** 用户账户端点：注册/登录/注销/当前用户。
 *  @author 林子阳 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService){
        this.userService = userService;
        this.authService = authService;
    }

    /** 用户注册 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto){
        userService.register(dto);
        return Result.ok("ok");
    }

    /** 用户登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto){
        return Result.okData(authService.login(dto));
    }

    /** 当前登录用户信息 */
    @GetMapping("/me")
    public Result<UserVO> me(@RequestAttribute("userId") Long userId){
        return Result.okData(authService.me(userId));
    }

    /** 注销登录 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute("userId") Long userId) {
        authService.logout(userId);
        return Result.ok("ok");
    }
}
