package com.itlzy.aiinterview.common;

/** 追问策略枚举：label 是唯一对外字面，与落库值保持一致；STOP 表示终止追问，无字面。
 *  @author 林子阳 */
public enum FollowupStrategy {

    DRILL("下钻"),
    ANGLE("换角度"),
    SCAFFOLD("降档"),
    STOP(null);

    private final String label;

    FollowupStrategy(String label) {
        this.label = label;
    }

    /** 获取策略字面；STOP 无字面，调用视为程序错误 */
    public String label() {
        if (label == null) {
            throw new IllegalStateException("STOP 不是生成策略，无注入字面");
        }
        return label;
    }

    /** 是否为终止策略 */
    public boolean isStop() {
        return this == STOP;
    }
}