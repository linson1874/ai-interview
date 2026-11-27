package com.itlzy.aiinterview.util;

/** 向量工具：float[] 转为 pgvector 字面量 "[a,b,c]"。
 *  @author 林子阳 */
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
