package com.itlzy.aiinterview;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.mapper.InterviewQuestionMapper;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.vo.ReportSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/** 报告链付费冒烟（QuestionsGenSmokeTest 同款先例）：真实 DB 材料 → 真实 AI → 打印报告 */
@SpringBootTest
class ReportGenSmokeTest {

    @Autowired AiService aiService;
    @Autowired InterviewQuestionMapper questionMapper;

    @Test
    void 真实材料出真实报告() {
        List<InterviewQuestion> qs = questionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>().eq(InterviewQuestion::getInterviewId, 18L));
        ReportSchema r = aiService.generateReport(qs);
        System.out.println("summary=" + r.getSummary());
        r.getWeakPoints().forEach(w ->
                System.out.println(w.getCategory() + " | " + w.getReason() + " | " + w.getEvidence()));
    }
}