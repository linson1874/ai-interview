package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.LoginDTO;
import com.itlzy.aiinterview.vo.LoginVO;
import com.itlzy.aiinterview.vo.UserVO;

/** 登录认证接口。
 *  @author 林子阳 */
public interface AuthService {
    LoginVO login(LoginDTO dto);

    UserVO me(Long userId);
    void logout(Long userId);
}
