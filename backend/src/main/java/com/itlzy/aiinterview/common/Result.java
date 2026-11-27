package com.itlzy.aiinterview.common;

import lombok.Data;

/** 统一响应体：code + msg + data。
 *  @author 林子阳 */
@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    private Result(Integer code,String msg,T data){
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 构建响应体，码值统一取自 ResultCode 枚举 */
    public static <T> Result<T> of(ResultCode resultCode, String msg){
        return new Result<>(resultCode.getCode(),msg,null);
    }

    /** 成功响应：code=0 */
    public static <T> Result<T> ok(String msg){
        return of(ResultCode.SUCCESS,msg);
    }

    /** 成功且携带数据 */
    public static <T> Result<T> okData(T data){
        return new Result<>(ResultCode.SUCCESS.getCode(), "ok", data);
    }
}
