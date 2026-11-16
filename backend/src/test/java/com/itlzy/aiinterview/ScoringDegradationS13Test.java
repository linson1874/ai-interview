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

/** S13（裁决#30）：评分降级必须明示 code=1，不许静默三 null 欺骗客户端。
 *  直调 Service 不走 HTTP：①S13 的病根在降级分支不在协议层（405 测试已证 MockMvc 通路）；
 *  ②走 HTTP 需真 token，单端互踢会把学员 Apifox 的登录态顶掉——测试不该有社交副作用。
 *  @MockBean 替换 AiService：评分必炸=可控降级，零 AI 费用 */
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
        // 祭品=20号场74题（末题全空）。⚠ 场主是 user 5 不是 1（D2 演练建场用的 token）——首跑用 1L 被归属门 404 弹死，
        // 验收证据先验场次与归属，心理预期不算数（{{id}}变量误记案的测试版同课）
        BusinessException e = assertThrows(BusinessException.class,
                () -> interviewService.answer(5L, 20L, 74L, dto));
        assertEquals(1, e.getResultCode().getCode());
        assertEquals("本题评分未成功，可重新提交重评", e.getMessage());
    }
}
