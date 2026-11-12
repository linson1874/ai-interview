package com.itlzy.aiinterview.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnswerDTO {

    @NotBlank(message = "答案不能为空")
    @Size(max = 1000,message = "答案不能超过1000字")
    private String answer;
}
