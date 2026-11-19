package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.vo.AskVO;

/** 合同08 K6-d：知识库问答独立最小链（出题/评分旧链不动）。
 *  墙二检索 + J5 五层纵深平移（E3 开工前置件） */
public interface KnowledgeAskService {

    AskVO ask(String question);
}