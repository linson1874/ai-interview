package com.itlzy.aiinterview;

import com.itlzy.aiinterview.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 导师代改（2026-11-14）：第六次点名未兑现的裸 assert + 长期缺席的负路径。
 * 教训在案：surefire 默认 -ea 掩盖了裸 assert 在 IDE 里的假绿——
 * "换个环境就失效的检查"比没有检查更危险。今后测试一律 Assertions。
 */
@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void createAndParse() {
        String token = jwtUtil.createToken(99L);
        Long userId = jwtUtil.parseUserId(token);
        Assertions.assertEquals(99L, (long) userId);   // 发票、验票闭环
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwtUtil.createToken(99L);
        // 篡改签名段末字符：票面被动过一笔，验票必须翻脸
        char last = token.charAt(token.length() - 1);
        String bad = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');
        Assertions.assertThrows(RuntimeException.class, () -> jwtUtil.parseUserId(bad));
    }

    @Test
    void garbageTokenRejected() {
        Assertions.assertThrows(RuntimeException.class, () -> jwtUtil.parseUserId("not.even.a.jwt"));
    }
}
