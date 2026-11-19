package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.util.List;

/** ask 链响应：答案 + 引用了题池哪几条（evidenceKbIds=E3 考古通道，用户可逐条对答案对原文） */
@Data
public class AskVO {

    private String answer;
    /** 模型实际引用的 kb_question id（已校验⊆本场检索真返回集；诚实拒答时为空） */
    private List<Long> evidenceKbIds;
}