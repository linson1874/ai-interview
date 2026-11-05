package com.itlzy.aiinterview.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j                      // Lombok：生成 log 日志对象（相当于自动 new 了个 logger）
@RestControllerAdvice       // 声明：全项目 Controller 的异常都归我翻译
public class GlobalExceptionHandler {

    /** 业务失败（Service 主动鸣哨，如用户名已存在）→ 按异常自带的码装盘，预期内结果不记错误日志 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.of(e.getResultCode(), e.getMessage());
    }

    /** 参数校验失败 → code=2，把第一条校验提示语带出去 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        // 密码同时违反两条规则时会有多个 FieldError，取第一个即可（前端只展示一条）
        FieldError first = e.getBindingResult().getFieldErrors().get(0);
        return Result.of(ResultCode.PARAM_INVALID, first.getDefaultMessage());  // msg 就是注解里写的"密码至少6位"
    }

    /** 接口不存在（URL/方法写错）→ HTTP 404 + code=404；靠 Spring“就近匹配”从下面 500 兜底手里抢回来（裁决 #1） */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)   // 让 HTTP 状态码也返回 404，不再被误报成系统故障
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.of(ResultCode.NOT_FOUND, "接口不存在，请检查请求地址");
    }

    /**请求体根本不是合法JSON->HTTP 400 + code=400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e){
        return Result.of(ResultCode.BAD_REQUEST, "请求体不是合法JSON");
    }

    /** 并发重复注册漏网到这一层的 → code=1（Service 内已必做处理，这里给其他未来自查漏写的 Service 兑底） */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e) {
        return Result.of(ResultCode.BUSINESS_FAIL, "用户名已存在");
    }

    /** 兜底：没预料到的异常 → code=500，日志记详细原因，对外只说"系统繁忙" */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);   // ⚠️ 面试点：异常必须记日志，否则线上事故无从查起
        return Result.of(ResultCode.SERVER_ERROR, "系统繁忙");
    }
}