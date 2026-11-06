package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.LoginDTO;
import com.itlzy.aiinterview.vo.LoginVO;
import com.itlzy.aiinterview.vo.UserVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);

    UserVO me(Long userId);
    void logout(Long userId);
}
