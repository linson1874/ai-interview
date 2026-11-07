package com.itlzy.aiinterview.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 列表项 */
@Data
public class ResumeListItemVO {

    private Long id;

    private String realName;

    private String targetJob;

    private List<String> skills;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}