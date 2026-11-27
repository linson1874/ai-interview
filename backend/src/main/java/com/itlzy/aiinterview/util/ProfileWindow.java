package com.itlzy.aiinterview.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** 掌握度曲线算术（纯函数）：维护最近判分窗口并计算掌握度。
 *  @author 林子阳 */
public final class ProfileWindow {

    private ProfileWindow() {
    }

    /** 追加新分数并截断到最近 window 个（左旧右新） */
    public static String appendAndTrim(String csv, int score, int window) {
        List<String> parts = new ArrayList<>();
        if (csv != null && !csv.isBlank()) {
            for (String s : csv.split(",")) {
                parts.add(s.trim());
            }
        }
        parts.add(String.valueOf(score));
        while (parts.size() > window) {
            parts.remove(0);   // 最旧的滚出窗口
        }
        return String.join(",", parts);
    }

    /** 计算掌握度=窗口均值×20，保留两位小数；空窗口返回 0 */
    public static BigDecimal mastery(String csv) {
        if (csv == null || csv.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String[] parts = csv.split(",");
        int sum = 0;
        for (String p : parts) {
            sum += Integer.parseInt(p.trim());
        }
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(parts.length), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(20))
                .setScale(2, RoundingMode.HALF_UP);
    }
}