package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.AskDTO;
import com.itlzy.aiinterview.dto.ChatDTO;
import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.service.KnowledgeAskService;
import com.itlzy.aiinterview.vo.AskVO;
import com.itlzy.aiinterview.vo.ChatVO;
import com.itlzy.aiinterview.vo.QuestionsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** AI 能力端点：对话/出题/知识库问答。
 *  @author 林子阳 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final KnowledgeAskService knowledgeAskService;

    public AiController(AiService aiService, KnowledgeAskService knowledgeAskService) {
        this.aiService = aiService;
        this.knowledgeAskService = knowledgeAskService;
    }

    @PostMapping("/chat")
    public Result<ChatVO> chat(@Valid @RequestBody ChatDTO dto) {
        return Result.okData(aiService.chat(dto.getQuestion()));
    }

    @PostMapping("/questions")
    public Result<QuestionsVO> generate(@RequestAttribute("userId") Long userId,
                                        @Valid @RequestBody GenerateDTO dto) {
        return Result.okData(aiService.generateQuestions(userId, dto));
    }

    /** 知识库问答 */
    @PostMapping("/kb/ask")
    public Result<AskVO> ask(@Valid @RequestBody AskDTO dto) {
        return Result.okData(knowledgeAskService.ask(dto.getQuestion()));
    }
}
