package com.itlzy.aiinterview.service.impl;

import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.dto.ChatDTO; // 保留 chat
import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.service.ResumeService;
import com.itlzy.aiinterview.util.LenientQuestionsConverter;
import com.itlzy.aiinterview.vo.ChatVO;
import com.itlzy.aiinterview.vo.QuestionsSchema;
import com.itlzy.aiinterview.vo.QuestionsVO;
import com.itlzy.aiinterview.vo.ResumeDetailVO;
import com.itlzy.aiinterview.vo.UsageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final ResumeService resumeService;    // 清单4"复用别复制粘贴"：404门+skills解析全借detail()

    /** 出题专属输出预算：5题×500×1.2（§2 定稿），与 /ai/chat 的全局 512 分离 */
    private static final int GEN_MAX_TOKENS = 3000;
    private static final int DEFAULT_COUNT = 5;   // R4 缺省，唯一出处

    /** §5 system prompt 定稿——与合同逐字一致（含 R6 修正），一个字都不许顺手改。
     *  题数不在这里（运行时参数进 user，保住可缓存的恒定前缀） */
    private static final String SYSTEM_PROMPT = """
            你是应届生技术面试环节的面试官，任务是根据候选人简历生成面试题。
            规则：
            1. 只生成面试题，不作答、不闲聊、不解释你的行为。
            2. 每道题必须与简历内容相关；"项目"类题目必须引用简历中的具体经历，不许凭空编造简历里没有的项目。
            3. category 只能取以下四个值之一：八股、项目、场景、HR；difficulty 取 1（简单）、2（中等）、3（困难）。
            4. 题目难度需有梯度，禁止全部同档。
            5. 题干使用中文。
            6. 只输出一个 JSON 对象，禁止输出任何解释文字、前后缀，禁止使用 markdown 代码块符号。
            """;

    /** L3 重试的纠错强化句（§3 定稿文案，追加在 system 末尾——重试必须带增量信息） */
    private static final String RETRY_REMINDER =
            "\n上一次输出包含非法内容。本次只输出一个符合 schema 的 JSON 对象，禁止 markdown 代码块，禁止任何解释文字。";

    /** L1 的 schema 指令来源 + L2 清洗执行者，一个实例两用，无状态所以字段级复用 */
    private final LenientQuestionsConverter outputConverter = new LenientQuestionsConverter();

    public AiServiceImpl(ChatClient chatClient, ResumeService resumeService) {
        this.chatClient = chatClient;
        this.resumeService = resumeService;
    }

    @Override
    public ChatVO chat(String question) {           // Day7 原方法一字不动
        ChatResponse response = chatClient.prompt()
                .user(question)
                .call()
                .chatResponse();
        String answer = response.getResult().getOutput().getText();
        String model = response.getMetadata().getModel();
        return new ChatVO(answer, model);
    }

    @Override
    public QuestionsVO generateQuestions(Long userId, GenerateDTO dto) {
        // ── 门① 归属+取数一步：detail 内部就是 getOwnResumeOr404——G2 的 404 与 03 D5 同源，零复制 ──
        ResumeDetailVO resume = resumeService.detail(userId, dto.getResumeId());
        int count = dto.getCount() == null ? DEFAULT_COUNT : dto.getCount();   // R4 兜底

        // ── 门② 拼 prompt：skills 在这里完成 List→顿号（形态跟用途走的那一步）──
        String user = buildUserPrompt(resume, count);

        // ── 门③ 第一发调用 + L2 解析；炸了才进门④ L3（三级漏斗的流程化身，最多两次上游调用封顶）──
        ChatResponse response = callUpstream(SYSTEM_PROMPT, user);
        QuestionsSchema schema;
        try {
            schema = outputConverter.convert(response.getResult().getOutput().getText());
        } catch (Exception first) {
            log.warn("出题解析失败(L2未救回)，L3重试一次。首轮原文={}",
                    response.getResult().getOutput().getText(), first);        // 首轮原文留痕(warn)

            // ── 门④ 重试：system 末尾追加纠错句——原样重发=掷骰子 ──
            response = callUpstream(SYSTEM_PROMPT + RETRY_REMINDER, user);
            try {
                schema = outputConverter.convert(response.getResult().getOutput().getText());
            } catch (Exception second) {
                log.error("出题重试后仍失败，畸形原文全文={}",
                        response.getResult().getOutput().getText(), second);
                throw new BusinessException(ResultCode.SERVER_ERROR, "AI 出题未成功，请稍后重试");
                // ↑ 与"AI 服务暂时不可用"语义分立(§1.2)；上游传输异常不在这——由 Day7 的 C handler 收口
            }
        }

        // ── 转驳装配：答卷归 questions；model/usage 从响应元数据抄——答卷里没有的，元数据里找 ──
        QuestionsVO vo = new QuestionsVO();
        vo.setQuestions(schema.getQuestions());
        vo.setModel(response.getMetadata().getModel());
        Usage usage = response.getMetadata().getUsage();
        UsageVO usageVO = new UsageVO();
        usageVO.setPromptTokens(usage.getPromptTokens());
        usageVO.setCompletionTokens(usage.getCompletionTokens());
        vo.setUsage(usageVO);
        return vo;
    }

    /** L1 语法层 + 出题专属预算都锁在这一个方法里——两发调用同一道闸门，想漏都难 */
    private ChatResponse callUpstream(String system, String user) {
        return chatClient.prompt()
                .system(system)
                .user(user)
                .options(OpenAiChatOptions.builder()
                        .maxTokens(GEN_MAX_TOKENS)                                    // per-call，不污染 yml
                        .responseFormat(ResponseFormat.builder()
                                .type(ResponseFormat.Type.JSON_OBJECT).build())       // JSON mode 只给出题用
                        .build())
                .call()
                .chatResponse();
    }

    /** §5 user 模板定稿的逐字拼接。故意用字符串拼接而非模板引擎：
     *  getFormat() 的 JSON Schema 里满是花括号，拼接朴素但对 {} 绝对免疫 */
    private String buildUserPrompt(ResumeDetailVO r, int count) {
        return "候选人简历：\n"
                + "姓名：" + r.getRealName() + "\n"
                + "目标岗位：" + nullSafe(r.getTargetJob()) + "\n"
                + "教育背景：" + nullSafe(r.getEducation()) + "\n"
                + "专业技能：" + String.join("、", r.getSkills()) + "\n"
                + "项目与自我介绍：" + nullSafe(r.getSelfIntro()) + "\n"
                + "请生成 " + count + " 道题。\n"
                + outputConverter.getFormat();      // = 模板里的 {format} 位
    }

    /** 空字段拼"（未填写）"而不是删整行：整行消失会让模型在"没这栏"和"没填"之间猜谜 */
    private String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "（未填写）" : s;
    }
}