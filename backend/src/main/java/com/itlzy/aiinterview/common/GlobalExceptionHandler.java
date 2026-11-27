package com.itlzy.aiinterview.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 全局异常处理：把各层抛出的异常统一翻译成标准响应。
 *  @author 林子阳 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常自带的码返回；码值≥400 时同步设置对应 HTTP 状态码，业务码仍走 HTTP 200 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletResponse response) {
        if (e.getResultCode().getCode() >= 400) {
            response.setStatus(e.getResultCode().getCode());
        }
        return Result.of(e.getResultCode(), e.getMessage());
    }
    /** 参数校验失败 → code=2，取第一条校验提示语返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().get(0);
        return Result.of(ResultCode.PARAM_INVALID, first.getDefaultMessage());
    }

    /** 请求路径不存在 → HTTP 404 + code=404 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        return Result.of(ResultCode.NOT_FOUND, "接口不存在，请检查请求地址");
    }

    /** 请求体不是合法 JSON → HTTP 400 + code=400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e){
        return Result.of(ResultCode.BAD_REQUEST, "请求体不是合法JSON");
    }

    /** 请求方法与路径不匹配 → HTTP 405 + code=405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.of(ResultCode.METHOD_NOT_ALLOWED, "请求方法与接口不匹配，请检查文档");
    }

    /** 上游大模型服务异常 → code=500 + 固定文案，不暴露内部细节 */
    @ExceptionHandler(NonTransientAiException.class)
    public Result<Void> handleAiUpstream(NonTransientAiException e) {
        log.error("AI上游异常", e);
        return Result.of(ResultCode.SERVER_ERROR, "AI 服务暂时不可用，请稍后重试");
    }

    /** 并发注册导致用户名唯一索引冲突的兜底 → code=1 */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicate(DuplicateKeyException e) {
        return Result.of(ResultCode.BUSINESS_FAIL, "用户名已存在");
    }

    /** 兜底：未预料到的异常 → code=500，详细原因只记日志，对外统一返回系统繁忙 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.of(ResultCode.SERVER_ERROR, "系统繁忙");
    }
}