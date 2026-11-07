package com.itlzy.aiinterview.service.impl;

import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.vo.ChatVO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;

    public AiServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public ChatVO chat(String question) {
        ChatResponse response = chatClient.prompt()
                .user(question)
                .call()               // call=同步等全文返回（stream 阶段三再见）
                .chatResponse();      // 要完整响应(含metadata)，不是 .content()——因为合同还要 model 字段

        String answer = response.getResult().getOutput().getText();
        String model  = response.getMetadata().getModel();   // 供应商回显的"实际应答模型"，不是yml配置的回声
        return new ChatVO(answer, model);
    }
}