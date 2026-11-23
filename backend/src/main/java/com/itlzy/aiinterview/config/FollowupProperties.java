package com.itlzy.aiinterview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 合同10 E 旋钮（#59 批准参数二分）：max-per-interview/max-tokens 进 yml——
 * 预期会被调的预算值；max-depth 不在此，它是 FollowupDecision.MAX_DEPTH 的规则资产
 * （改深度须重新论证信号衰减，不给一行配置的便宜通道）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "followup")
public class FollowupProperties {

    /** 单场追问总量上限（数法=题表 parent 非空行数，树节点口径） */
    private int maxPerInterview = 6;

    /** 生成输出预算：分布上沿 320×1.2=384 取整（算式见合同 10 §E，S14 家法预算与 schema 同文档） */
    private int maxTokens = 400;
}