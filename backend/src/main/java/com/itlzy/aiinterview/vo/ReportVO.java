package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 报告响应面 **/
@Data
public class ReportVO {

    /** 报告小状态机：0排队 1生成中 2就绪 3失败 */
    private Integer reportStatus;

    /** 总分（J4 公式，Java 本地算；DECIMAL(5,2) 所以 BigDecimal，已评<3 时 null——P10 样本闸） */
    private BigDecimal overallScore;

    /** 报告注明已评数：未评分剔除分母，系统故障不记用户头上 */
    private Integer scoredCount;
    private Integer totalCount;

    /** AI定性段 */
    private String summary;

    /** AI薄弱点段 */
    private List<WeakPointVO> weakPoints;

    /** 逐题明细 */
    private List<ReportDetailVO> details;
}
