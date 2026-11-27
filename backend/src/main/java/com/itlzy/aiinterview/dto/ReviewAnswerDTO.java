package com.itlzy.aiinterview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 复习作答提交。 */
@Data
public class ReviewAnswerDTO {

    @NotNull(message = "复习题ID不能为空")
    private Long itemId;

    @NotBlank(message = "答案不能为空")
    @Size(max = 1000, message = "答案不能超过1000字")
    private String answer;
}