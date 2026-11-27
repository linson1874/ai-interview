package com.itlzy.aiinterview;

import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.dto.AnswerDTO;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** 评分降级测试：评分失败必须显式返回业务错误，不得静默；@MockBean 使评分必炸，零 AI 费用。
 *  直调 Service 不走 HTTP，避免测试登录态互踢。 */
@SpringBootTest
class ScoringDegradationS13Test {

    @Autowired
    private InterviewService interviewService;

    @MockBean
    private AiService aiService;

    @Test
    void scoringFailure_shouldReturnCode1Explicit() {
        when(aiService.scoreQuestion(any(), any(), any())).thenThrow(new RuntimeException("演练：评分必炸"));
        AnswerDTO dto = new AnswerDTO();
        dto.setAnswer("演练作答文本");
        // 固定场次（属 user 5）：先验归属再验降级文案
        BusinessException e = assertThrows(BusinessException.class,
                () -> interviewService.answer(5L, 20L, 74L, dto));
        assertEquals(1, e.getResultCode().getCode());
        assertEquals("本题评分未成功，可重新提交重评", e.getMessage());
    }
}
