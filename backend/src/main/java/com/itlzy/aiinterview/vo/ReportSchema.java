package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReportSchema {
    private String summary;                 // LLM 只写这两个字段
    private List<WeakPointVO> weakPoints;   // 总分/分类均分/明细全由 Java 本地拼（S3 口径：复数，与 prompt/schema 三方一致）
}
