package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 405 回归测试：请求方法不匹配必须返回 HTTP 405，不得落入 500 兜底。 */
@SpringBootTest
@AutoConfigureMockMvc
class MethodNotAllowed405Test {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postHitsGetEndpoint_should405Not500() throws Exception {
        // POST 打进 GET 端点：方法不匹配应在路由阶段报错，够不到鉴权拦截器，故免 token
        mockMvc.perform(post("/interview/finish"))
                .andExpect(status().isMethodNotAllowed())      // HTTP 405
                .andExpect(jsonPath("$.code").value(405));      // 业务码镜像
    }
}