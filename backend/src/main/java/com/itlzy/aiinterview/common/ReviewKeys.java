package com.itlzy.aiinterview.common;

/**
 * 合同09 复习域工程常量（InterviewStream 同层同级）：Redis 键名与状态枚举值是工程结构件不是旋钮——
 * 挪它们=挪代码，不叫调参（yml 里只有 Leitner 间隔/阈值/窗口这类业务参数）。
 */
public final class ReviewKeys {

    private ReviewKeys() {
    }

    /** 到期队列 ZSET（全局单队列，member=itemId，score=next_review_at epoch 秒）——纯投影，重启重建 */
    public static final String QUEUE_KEY = "review:queue";

    /** t_review_item.status 两值（F2① CAS 状态机；processing 带租约防死锁） */
    public static final String STATUS_DUE = "due";
    public static final String STATUS_PROCESSING = "processing";

    /** t_skill_profile.axis 两值（E1 两轴不串门的表结构形态） */
    public static final String AXIS_TAG = "tag";
    public static final String AXIS_CATEGORY = "category";

    /** t_review_log.result 三档（B 裁决：>=4 前进 / <=2 回盒一 / =3 冻结） */
    public static final String RESULT_PROMOTE = "promote";
    public static final String RESULT_DEMOTE = "demote";
    public static final String RESULT_FREEZE = "freeze";
}
