package com.itlzy.aiinterview.service;

import com.itlzy.aiinterview.vo.AskVO;

/** 知识库问答接口。
 *  @author 林子阳 */
public interface KnowledgeAskService {

    AskVO ask(String question);
}