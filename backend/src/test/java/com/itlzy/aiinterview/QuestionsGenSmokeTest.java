package com.itlzy.aiinterview;

import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.dto.LoginDTO;
import com.itlzy.aiinterview.dto.RegisterDTO;
import com.itlzy.aiinterview.dto.ResumeSaveDTO;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.service.AuthService;
import com.itlzy.aiinterview.service.ResumeService;
import com.itlzy.aiinterview.service.UserService;
import com.itlzy.aiinterview.util.JwtUtil;
import com.itlzy.aiinterview.vo.LoginVO;
import com.itlzy.aiinterview.vo.QuestionsVO;
import com.itlzy.aiinterview.vo.ResumeListItemVO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuestionsGenSmokeTest {

    @Autowired private UserService userService;
    @Autowired private AuthService authService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private ResumeService resumeService;
    @Autowired private AiService aiService;

    @Test
    @Disabled("G1付费冒烟：真打DeepSeek烧余额，充值后摘帽运行；跑完把打印的usage回填合同05 §2（承诺项）")
    void g1GenerateQuestionsSmoke() {
        // ── 数据准备：自给自足（直调Service绕过@Valid，所以值要手动符合规则）──
        String username = "day8g1_" + System.currentTimeMillis();
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername(username);
        reg.setPassword("day8test123");           // ≥6位，符合注册规则
        reg.setNickname("出题冒烟");
        userService.register(reg);

        LoginDTO loginDto = new LoginDTO();
        loginDto.setUsername(username);
        loginDto.setPassword("day8test123");
        LoginVO login = authService.login(loginDto);   // 真实管道：BCrypt比对+Redis写存根
        Long userId = jwtUtil.parseUserId(login.getToken());  // 拦截器第一关同款工具复用

        ResumeSaveDTO resume = new ResumeSaveDTO();
        resume.setRealName("测试考生");
        resume.setTargetJob("Java后端开发");
        resume.setEducation("XX大学计算机科学与技术专业");
        resume.setSkills(List.of("Java", "Spring Boot", "MySQL", "Redis"));
        resume.setSelfIntro("一名有两段实习经历、熟悉Spring Boot和MySQL的应届生候选人。");
        resumeService.create(userId, resume);

        Long resumeId = resumeService.page(userId, 1, 10).getList().get(0).getId();

        // ── 正题：count 不填 → 走 R4 缺省 5 ──
        GenerateDTO dto = new GenerateDTO();
        dto.setResumeId(resumeId);
        QuestionsVO result = aiService.generateQuestions(userId, dto);

        // ── 断言 = 合同§6 G1 期望值的逐条翻译 ──
        assertNotNull(result.getQuestions());
        assertEquals(5, result.getQuestions().size(), "count缺省应出5题(R4)");
        for (var q : result.getQuestions()) {
            assertTrue(List.of("八股", "项目", "场景", "HR").contains(q.getCategory()),
                    "category必须∈§4枚举，实际=" + q.getCategory());
            assertTrue(List.of(1, 2, 3).contains(q.getDifficulty()), "difficulty必须1-3");
            assertFalse(q.getQuestion() == null || q.getQuestion().isBlank(), "题干非空");
        }
        assertNotNull(result.getModel(), "model来自响应元数据回显");
        assertTrue(result.getUsage().getPromptTokens() > 0, "usage非零(G1期望)");

        // ── §2 回填数字在这行打印里：跑完把两个数抄进合同 ──
        System.out.printf("【§2回填】prompt=%d completion=%d model=%s%n",
                result.getUsage().getPromptTokens(),
                result.getUsage().getCompletionTokens(),
                result.getModel());
    }
}