package com.itlzy.aiinterview;

import com.itlzy.aiinterview.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** JWT 工具测试：签发/解析闭环与篡改、垃圾 token 的拒绝。 */
@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void createAndParse() {
        String token = jwtUtil.createToken(99L);
        Long userId = jwtUtil.parseUserId(token);
        Assertions.assertEquals(99L, (long) userId);   // 签发与解析闭环
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwtUtil.createToken(99L);
        // 篡改签名段中部字符（末字符含填充位，改动可能不改变解码字节）
        int pos = token.length() - 10;
        char c = token.charAt(pos);
        String bad = token.substring(0, pos) + (c == 'A' ? 'B' : 'A') + token.substring(pos + 1);
        Assertions.assertThrows(RuntimeException.class, () -> jwtUtil.parseUserId(bad));
    }

    @Test
    void garbageTokenRejected() {
        Assertions.assertThrows(RuntimeException.class, () -> jwtUtil.parseUserId("not.even.a.jwt"));
    }
}
