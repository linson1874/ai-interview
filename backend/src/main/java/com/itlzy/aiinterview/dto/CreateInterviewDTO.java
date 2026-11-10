package com.itlzy.aiinterview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateInterviewDTO {

    @NotNull(message = "请指定简历ID")
    private Long resumeId;

    @Min(value = 1, message = "题目数量需在1-10之间")
    @Max(value = 10, message = "题目数量需在1-10之间")
    private Integer count;
}
