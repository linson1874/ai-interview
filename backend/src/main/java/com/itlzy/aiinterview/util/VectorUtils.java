package com.itlzy.aiinterview.util;

/**
 * float[] → pgvector 字面量 "[a,b,c]"（08 在案账兑现：toLiteral 曾有三处私有副本，
 * 合同09 入队映射是第四个调用方——五而无一，抽公共出口）。
 */
public final class VectorUtils {

    private VectorUtils() {
    }

    public static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
