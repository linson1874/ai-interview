package com.itlzy.aiinterview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateDTO {

    @NotNull(message = "请指定简历ID")
    private Long resumeId;

    @Min(value = 1,message = "题目数量需在1-5之间")
    @Max(value = 5,message = "题目数量需在1-5之间")
    private Integer count;
}
