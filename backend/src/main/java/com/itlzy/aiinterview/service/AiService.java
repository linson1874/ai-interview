package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.vo.ChatVO;
import com.itlzy.aiinterview.vo.QuestionsVO;
import com.itlzy.aiinterview.vo.ReportSchema;
import com.itlzy.aiinterview.vo.ScoringSchema;

import java.util.List;

public interface AiService {

    ChatVO chat(String question);


    QuestionsVO generateQuestions(Long userId, GenerateDTO dto);

    ScoringSchema scoreQuestion(String questionContent,Integer difficulty,String userAnswer);

    /** 报告定性段（合同07 J3）：吃整场题目材料，吐已过闸门的可信 schema；总分不归它管 */
    ReportSchema generateReport(List<InterviewQuestion> questions);
}
