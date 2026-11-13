package com.itlzy.aiinterview.common;

/** 面试出题 Stream 命名册（合同06 H2：名字必须跨重启稳定，禁止时间戳/UUID） */
public final class InterviewStream {

    private InterviewStream() {}   // 常量类，不实例化

    public static final String STREAM_KEY = "interview:gen";
    public static final String GROUP      = "gen-group";
    public static final String CONSUMER   = "gen-consumer-1";
    public static final long   MAX_LEN    = 10000;   // R4：XADD 必带 MAXLEN ~，否则"存全量历史"从论据变事故

    // ── 合同07 报告流（J3：不混 interview:gen——串行排队+死信语义不同场）──
    public static final String REPORT_STREAM_KEY = "interview:report";
    public static final String REPORT_GROUP      = "report-group";
    public static final String REPORT_CONSUMER   = "report-consumer-1";
}