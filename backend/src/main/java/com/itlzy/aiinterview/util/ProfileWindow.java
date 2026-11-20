package com.itlzy.aiinterview.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 合同09 曲线算术（golden rule 的正面形态：聚合必须是本地算术，模型不算数）。
 * 纯函数无状态——W8 手算对拍直接测这里，不用起 Spring、不碰库、零 token。
 */
public final class ProfileWindow {

    private ProfileWindow() {
    }

    /** append 新分数并截断到最近 window 个（左旧右新）。csv 为 null/空 = 窗口起点 */
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

    /** 均值×20，两位小数（DECIMAL(5,2) 口径）；空窗返回 0——0 分均值也是 0，不抛异常装死 */
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