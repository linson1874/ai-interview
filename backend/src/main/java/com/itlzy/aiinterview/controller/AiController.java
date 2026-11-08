package com.itlzy.aiinterview.controller;

import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.dto.ChatDTO;
import com.itlzy.aiinterview.dto.GenerateDTO;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.vo.ChatVO;
import com.itlzy.aiinterview.vo.QuestionsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** AI问答 **/
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
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
}
