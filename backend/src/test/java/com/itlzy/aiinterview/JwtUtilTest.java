package com.itlzy.aiinterview;

import com.itlzy.aiinterview.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void createAndParse() {
        String token = jwtUtil.createToken(99L);
        System.out.println("token = " + token);
        Long userId = jwtUtil.parseUserId(token);
        assert userId == 99L;   // 发票、验票闭环
    }
}
