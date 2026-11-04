package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.RegisterDTO;
import com.itlzy.aiinterview.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final UserService userService;

    public HelloController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "AI面试系统后端启动成功!";
    }

    @GetMapping("/test-result")
    public Result<Void> testResult(){
        return Result.ok("ok");
    }

}
