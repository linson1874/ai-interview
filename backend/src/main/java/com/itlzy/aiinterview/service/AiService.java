package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.vo.ChatVO;
import com.itlzy.aiinterview.vo.QuestionsVO;

public interface AiService {

    ChatVO chat(String question);

    QuestionsVO generateQuestions(Long userId, GenerateDTO dto);
}
