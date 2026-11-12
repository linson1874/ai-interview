package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.vo.ChatVO;
import com.itlzy.aiinterview.vo.QuestionsVO;
import com.itlzy.aiinterview.vo.ScoringSchema;

public interface AiService {

    ChatVO chat(String question);

    QuestionsVO generateQuestions(Long userId, GenerateDTO dto);

    ScoringSchema scoreQuestion(String questionContent,Integer difficulty,String userAnswer);
}
