package com.itlzy.aiinterview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 合同09 B/G4 裁决：Leitner 参数整组进 yml——换代 SM-2 时整组替换，不动代码。
 * 字段默认值是 yml 缺省时的兜底（与 sql/07_review.sql 注释口径一致）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "review")
public class ReviewProperties {

    /** 五盒间隔（天），盒号即下标+1 */
    private List<Integer> intervalsDays = List.of(1, 3, 7, 16, 35);

    /** 前进档下限（B：>=4 才配驱动盒号前进） */
    private int promoteMinScore = 4;

    /** 回盒一上限（B：<=2；同时是入队钩子的错题边界——判分与入队同一把尺） */
    private int demoteMaxScore = 2;

    /** 曲线窗口：最近 N 次判分均值×20（G4） */
    private int profileWindow = 5;

    /** processing 租约（分钟）：超时视为持有者已死可被 CAS 重捞——kill -9 死锁洞的堵法，待导师追认 */
    private int processingLeaseMinutes = 30;
}
