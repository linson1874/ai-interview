package com.itlzy.aiinterview.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j                      // Lombok：生成 log 日志对象（相当于自动 new 了个 logger）
@RestControllerAdvice       // 声明：全项目 Controller 的异常都归我翻译
public class GlobalExceptionHandler {

    /** 参数校验失败 → code=2，把第一条校验提示语带出去 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        // 密码同时违反两条规则时会有多个 FieldError，取第一个即可（前端只展示一条）
        FieldError first = e.getBindingResult().getFieldErrors().get(0);
        return Result.invalidParam(first.getDefaultMessage());  // msg 就是注解里写的"密码至少6位"
    }

    /** 并发重复注册漏网 → code=1（Service 里已拦一层，这里双保险） */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e) {
        return Result.fail("用户名已存在");
    }

    /** 兜底：没预料到的异常 → code=500，日志记详细原因，对外只说"系统繁忙" */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);   // ⚠️ 面试点：异常必须记日志，否则线上事故无从查起
        return Result.serverError("系统繁忙");
    }
}