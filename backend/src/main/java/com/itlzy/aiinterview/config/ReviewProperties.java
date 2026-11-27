package com.itlzy.aiinterview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** 复习调度（Leitner）可调参数，整组进 yml 便于整体替换算法；字段默认值为 yml 缺省时的兜底。
 *  @author 林子阳 */
@Data
@Component
@ConfigurationProperties(prefix = "review")
public class ReviewProperties {

    /** 五盒间隔（天），盒号=下标+1 */
    private List<Integer> intervalsDays = List.of(1, 3, 7, 16, 35);

    /** 前进档的分数下限 */
    private int promoteMinScore = 4;

    /** 退回档的分数上限（同时是错题入复习队列的边界） */
    private int demoteMaxScore = 2;

    /** 能力曲线窗口：取最近 N 次判分 */
    private int profileWindow = 5;

    /** 处理中状态的租约时长（分钟），超时可被重新捞取 */
    private int processingLeaseMinutes = 30;
}
