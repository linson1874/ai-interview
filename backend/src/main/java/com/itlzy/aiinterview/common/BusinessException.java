package com.itlzy.aiinterview.common;

import lombok.Getter;

/**
 * 业务异常：后厨"主动鸣哨"用的哨子（如用户名已存在）。
 * Service 抛出后由 GlobalExceptionHandler 统一接住、按异常自带的 ResultCode 装盘，
 * Controller 不必再关心业务失败的文案（文案归 Service，见文档 2.2 分层约定）。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);            // msg 交给父类的 getMessage()，最终成为响应的 msg 字段
        this.resultCode = resultCode;
    }
}
