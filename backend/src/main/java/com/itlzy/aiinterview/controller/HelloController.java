package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    /** 健康检查 */
    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.okData("AI面试系统后端启动成功!");
    }
}
