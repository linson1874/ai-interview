package com.itlzy.aiinterview.common;

import lombok.Getter;

/**
 * 错误码总台账（对应接口文档 01 错误码表 v0.4：400/404/405 已镜像入表）
 * 编码规则：业务码占个位小数（0~3）；框架/协议层错误码镜像 HTTP 状态码（404、405、500）
 * 禁止在业务代码里裸写数字，一律通过本枚举取值（文档"禁止私加"在代码层的落地）
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),
    BUSINESS_FAIL(1, "业务失败"),
    PARAM_INVALID(2, "参数校验失败"),
    UNAUTHORIZED(3, "未登录/凭证失效（预留，登录课启用）"),
    BAD_REQUEST(400,"请求体无法解析（非法JSON）"),
    NOT_FOUND(404, "接口不存在"),
    METHOD_NOT_ALLOWED(405, "HTTP方法与路径不匹配"),
    SERVER_ERROR(500, "系统异常");

    private final Integer code;
    private final String desc;

    ResultCode(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
