package com.itlzy.aiinterview.common;

import lombok.Getter;

/** 错误码枚举：业务码 0~3；框架层错误码镜像 HTTP 状态码，业务代码一律取值于本枚举。
 *  @author 林子阳 */
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
