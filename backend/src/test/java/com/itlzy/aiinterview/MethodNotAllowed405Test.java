package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 裁决#29 回归网：框架/协议层错误必须镜像 HTTP，不得混进 500 兜底（11-05 家法的 405 分支） */
@SpringBootTest
@AutoConfigureMockMvc
class MethodNotAllowed405Test {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postHitsGetEndpoint_should405Not500() throws Exception {
        // 案发现场复刻：POST /interview/finish——"finish"被路由当成 {id} 撞进 GET /interview/{id}，
        // 方法不匹配该在 HandlerMapping 阶段抛 HttpRequestMethodNotSupportedException（够不到鉴权拦截器，免 token）
        mockMvc.perform(post("/interview/finish"))
                .andExpect(status().isMethodNotAllowed())      // HTTP 405
                .andExpect(jsonPath("$.code").value(405));      // 业务码镜像
    }
}