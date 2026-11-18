package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 工单③A段：embedding 链路冒烟（付费·几分钱·手动跑，非常驻回归）。
 *  验四件事一次过：key 有效 / base-url 对 / 模型名活 / 维度=1024（V3 三锁实测）。
 *  打印 tokens = V6 成本账本的第一行原始数据。 */
@SpringBootTest
class KbEmbeddingSmokeTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void embedOneSentenceReturns1024Dims() {
        EmbeddingResponse response = embeddingModel.call(
                new EmbeddingRequest(List.of("MySQL 索引为什么能让查询变快？"), null));
        float[] vector = response.getResults().get(0).getOutput();
        assertEquals(1024, vector.length, "维度≠1024 = dimensions 配置没生效或模型偷偷换族（V3 锁的就是这个）");
        Integer tokens = response.getMetadata().getUsage().getPromptTokens();
        System.out.println("[V6 记账] embedding 单发 tokens=" + tokens
                + "，单价见百炼当日牌价（K3：数字进文件不进印象）");
        assertNotNull(tokens, "拿不到 usage=计费不可观测，V6 没法做");
    }
}