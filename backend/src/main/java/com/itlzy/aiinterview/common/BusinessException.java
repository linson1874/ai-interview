package com.itlzy.aiinterview.common;

import lombok.Getter;

/** 业务异常：由 Service 抛出，由全局异常处理器统一转换为标准响应。
 *  @author 林子阳 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);            // msg 交给父类的 getMessage()，最终成为响应的 msg 字段
        this.resultCode = resultCode;
    }
}
