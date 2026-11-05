package com.itlzy.aiinterview.common;

import lombok.Data;

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

    /** 通用装盘入口：码值统一来自 ResultCode 枚举（收口魔法数字） */
    public static <T> Result<T> of(ResultCode resultCode, String msg){
        return new Result<>(resultCode.getCode(),msg,null);
    }

    //成功：code=0
    public static <T> Result<T> ok(String msg){
        return of(ResultCode.SUCCESS,msg);
    }

    /** 成功且携带数据（msg 固定 "ok"，data 放 VO） */
    public static <T> Result<T> okData(T data){
        return new Result<>(ResultCode.SUCCESS.getCode(), "ok", data);
    }
}
