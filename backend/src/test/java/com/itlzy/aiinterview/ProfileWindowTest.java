package com.itlzy.aiinterview;

import com.itlzy.aiinterview.util.ProfileWindow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** W8 手算对拍（合同09 §3）：窗口算术的每一格都必须能被小学除法复算 */
class ProfileWindowTest {

    @Test
    void 截断滚出最旧() {
        String csv = null;
        for (int s : new int[]{2, 0, 3, 4, 4, 1, 5}) {
            csv = ProfileWindow.appendAndTrim(csv, s, 5);
        }
        // 7 个分数进窗口 5：2 和 0 滚出，剩 3,4,4,1,5
        assertEquals("3,4,4,1,5", csv);
    }

    @Test
    void 均值乘20手算对拍() {
        // 手算：(3+4+4+1+5)/5 = 3.4 → ×20 = 68.00
        BigDecimal m = ProfileWindow.mastery("3,4,4,1,5");
        assertEquals(0, new BigDecimal("68.00").compareTo(m));
    }

    @Test
    void 单样本与空窗() {
        assertEquals(0, new BigDecimal("40.00").compareTo(ProfileWindow.mastery("2")));
        assertEquals(0, BigDecimal.ZERO.compareTo(ProfileWindow.mastery(null)));
    }
}