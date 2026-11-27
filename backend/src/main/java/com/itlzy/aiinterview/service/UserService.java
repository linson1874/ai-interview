package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.RegisterDTO;

/** 用户业务接口。
 *  @author 林子阳 */
public interface UserService {

    /** 注册：用户名已存在时抛业务异常 */
    void register(RegisterDTO dto);
}
