package com.itlzy.aiinterview.common;

/** 复习模块的 Redis 键名与状态枚举常量。
 *  @author 林子阳 */
public final class ReviewKeys {

    private ReviewKeys() {
    }

    /** 到期队列 ZSET 键（member=复习项 id，score=下次到期时间戳），重启可重建 */
    public static final String QUEUE_KEY = "review:queue";

    /** 复习项状态：到期待处理 / 处理中（带租约防死锁） */
    public static final String STATUS_DUE = "due";
    public static final String STATUS_PROCESSING = "processing";

    /** 技能画像的两个维度 */
    public static final String AXIS_TAG = "tag";
    public static final String AXIS_CATEGORY = "category";

    /** 复习结果三档：前进 / 退回 / 冻结 */
    public static final String RESULT_PROMOTE = "promote";
    public static final String RESULT_DEMOTE = "demote";
    public static final String RESULT_FREEZE = "freeze";
}
