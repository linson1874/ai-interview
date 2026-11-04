package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.RegisterDTO;
import com.itlzy.aiinterview.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    //用户注册
    @PostMapping("/user/register")
    public Result<Void> register(@Valid@RequestBody RegisterDTO dto){
        boolean ok = userService.register(dto);
        return ok ? Result.ok("ok") : Result.fail("用户名已存在");
    }
}
