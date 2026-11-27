package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.util.List;

/** 报告追问树节点。 */
@Data
public class ReportFollowupVO {

    private Long id;

    private String question;

    /** 追问策略中文字面，与落库值一致 */
    private String strategy;

    /** 沿父链派生的深度：主题行的孩子=1，追问的孩子=2 */
    private Integer depth;

    private Integer score;

    private String scoreComment;

    private String userAnswer;

    /** 嵌套孩子（一父至多一子），叶子为空数组 */
    private List<ReportFollowupVO> followups;
}
