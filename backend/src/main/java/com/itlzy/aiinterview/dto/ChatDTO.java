package com.itlzy.aiinterview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatDTO {

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题过长(≤500字)")
    private String question;
}
