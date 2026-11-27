package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 面试记录列表行；大字段 summary/report_json 不出列表，简历名称由前端按 resumeId 映射 */
@Data
public class InterviewListItemVO {

    /** 跳转详情/报告的钥匙 */
    private Long id;

    /** 状态徽章：0排队 1进行中 2已交卷 3失败 5已取消（4 是内部中间态，列表只见结果态） */
    private Integer status;

    private Integer questionCount;

    private BigDecimal overallScore;

    private LocalDateTime createTime;

    private Long resumeId;
}
