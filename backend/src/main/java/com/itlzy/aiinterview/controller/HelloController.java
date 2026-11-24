package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public Result<String> hello() {
        // Result 包装统一响应（合同11 §0：所有接口 code/msg/data 三件对齐）
        return Result.okData("AI面试系统后端启动成功!");
    }
}
