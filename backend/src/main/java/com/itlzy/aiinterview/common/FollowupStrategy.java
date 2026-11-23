package com.itlzy.aiinterview.common;

/**
 * 合同10 B 分支策略枚举（#60：中文三值字面定死，label 是唯一对外形态——
 * prompt 注入值与 strategy 落库值同出一门，禁翻译层）。
 * STOP 无 label：它不生成、不落库，端点见 STOP 即走拒绝文案。
 */
public enum FollowupStrategy {

    DRILL("下钻"),
    ANGLE("换角度"),
    SCAFFOLD("降档"),
    STOP(null);

    private final String label;

    FollowupStrategy(String label) {
        this.label = label;
    }

    /** 注入 prompt 与落 strategy 列的唯一出口；STOP 调用视为编程错误 */
    public String label() {
        if (label == null) {
            throw new IllegalStateException("STOP 不是生成策略，无注入字面");
        }
        return label;
    }

    /** 端点分流用：STOP 与生成策略走两条完全不同的路 */
    public boolean isStop() {
        return this == STOP;
    }
}