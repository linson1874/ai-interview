package com.itlzy.aiinterview.common;

/** Redis Stream 的键名与消费组常量，跨重启稳定。
 *  @author 林子阳 */
public final class InterviewStream {

    private InterviewStream() {}   // 常量类，不实例化

    /** 出题流 */
    public static final String STREAM_KEY = "interview:gen";
    public static final String GROUP      = "gen-group";
    public static final String CONSUMER   = "gen-consumer-1";
    /** 流最大长度上限，超出后自动裁剪旧消息 */
    public static final long   MAX_LEN    = 10000;

    /** 报告生成流（与出题流分离，互不干扰） */
    public static final String REPORT_STREAM_KEY = "interview:report";
    public static final String REPORT_GROUP      = "report-group";
    public static final String REPORT_CONSUMER   = "report-consumer-1";
}