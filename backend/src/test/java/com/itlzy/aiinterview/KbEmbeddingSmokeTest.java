package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** embedding 链路冒烟测试（付费，手动跑，不进 CI）：验证连通性与输出维度。 */
@SpringBootTest
class KbEmbeddingSmokeTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void embedOneSentenceReturns1024Dims() {
        EmbeddingResponse response = embeddingModel.call(
                new EmbeddingRequest(List.of("MySQL 索引为什么能让查询变快？"), null));
        float[] vector = response.getResults().get(0).getOutput();
        assertEquals(1024, vector.length, "维度≠1024 = 维度配置未生效或模型变更");
        Integer tokens = response.getMetadata().getUsage().getPromptTokens();
        System.out.println("embedding 单发 tokens=" + tokens);
        assertNotNull(tokens, "拿不到用量数据=计费不可观测");
    }
}