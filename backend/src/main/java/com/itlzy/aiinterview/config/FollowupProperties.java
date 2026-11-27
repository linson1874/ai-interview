package com.itlzy.aiinterview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 追问生成的可调参数（yml 可配）。
 *  @author 林子阳 */
@Data
@Component
@ConfigurationProperties(prefix = "followup")
public class FollowupProperties {

    /** 单场面试的追问总量上限 */
    private int maxPerInterview = 6;

    /** 单次追问生成的输出 token 预算 */
    private int maxTokens = 400;
}