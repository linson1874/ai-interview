
package com.itlzy.aiinterview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ResumeSaveDTO {

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 20, message = "真实姓名长度需在2-20个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z ·]+$", message = "真实姓名仅支持中英文、空格与“·”")
    private String realName;

    @NotBlank(message = "目标岗位不能为空")
    @Size(min = 2, max = 50, message = "目标岗位名称过短或过长")
    private String targetJob;

    @Size(max = 64, message = "学历描述不能超过64个字符")
    private String education;

    @NotEmpty(message = "请至少填写一项技能")
    @Size(max = 10, message = "最多只能添加10项技能")
    private List<String> skills;

    @Size(min = 10, message = "自我介绍不能少于10个字")
    @Size(max = 1000, message = "自我介绍不能超过1000个字")
    private String selfIntro;
}