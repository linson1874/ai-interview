package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.vo.ChatVO;
import com.itlzy.aiinterview.vo.QuestionsVO;
import com.itlzy.aiinterview.vo.ReportSchema;
import com.itlzy.aiinterview.vo.ScoringSchema;

import java.util.List;

/** AI 能力接口：对话/出题/评分/报告定性段。
 *  @author 林子阳 */
public interface AiService {

    ChatVO chat(String question);


    QuestionsVO generateQuestions(Long userId, GenerateDTO dto);

    ScoringSchema scoreQuestion(String questionContent,Integer difficulty,String userAnswer);

    /** 生成报告的摘要与薄弱点；总分不在此计算 */
    ReportSchema generateReport(List<InterviewQuestion> questions);
}
