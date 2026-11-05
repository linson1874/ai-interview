package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.RegisterDTO;

public interface UserService {
    /**
     * 注册：成功则静默返回；用户名已存在抛 BusinessException(code=1)
     * 异常驱动：业务文案归 Service 层，Controller 不关心（文档 2.2 + 裁决 #2）
     */
    void register(RegisterDTO dto);
}
