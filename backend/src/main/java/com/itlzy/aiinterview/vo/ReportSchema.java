package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReportSchema {
    private String summary;                 // 大模型只写这两个字段
    private List<WeakPointVO> weakPoints;   // 总分与明细由本地计算，不在此结构内
}
