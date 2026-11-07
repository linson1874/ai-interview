package com.itlzy.aiinterview.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 详情（含 education/selfIntro；无 user_id/deleted——内部字段不外泄） */
@Data
public class ResumeDetailVO {

    private Long id;

    private String realName;

    private String targetJob;

    private String education;

    private List<String> skills;

    private String selfIntro;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}