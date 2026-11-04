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

    //成功：code=0
    public static <T> Result<T> ok(String msg){
        return new Result<>(0,msg,null);
    }

    //业务失败：code=1
    public static <T> Result<T> fail(String msg){
        return new Result<>(1,msg,null);
    }

    //参数检验失败:code=2
    public static <T> Result<T> invalidParam(String msg){
        return new Result<>(2,msg,null);
    }

    //系统异常:code=500
    public static <T> Result<T> serverError(String msg){
        return new Result<>(500,msg,null);
    }
}
