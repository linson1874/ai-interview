package com.itlzy.aiinterview.vo;

import lombok.Data;

import java.util.List;

@Data
public class WeakPointVO {
    private String category;  // 八股|项目|场景|HR——四值枚举，闸门执法
    private String reason;
    private List<Long> evidence; // 题目ID列表
}
