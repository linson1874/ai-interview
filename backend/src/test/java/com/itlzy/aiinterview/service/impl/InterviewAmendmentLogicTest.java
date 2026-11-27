package com.itlzy.aiinterview.service.impl;

import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.vo.ReportFollowupVO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 面试记录分页参数校验与报告追问树组装的纯逻辑对拍（零库零 token）。 */
class InterviewAmendmentLogicTest {

    // ---------- 分页参数校验 ----------

    @Test
    void L2_页大小越界_沿用03文案() {
        for (long size : new long[]{0, 51, 99999}) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> InterviewServiceImpl.validatePageParams(1, size, null), "size=" + size);
            assertEquals(ResultCode.PARAM_INVALID, e.getResultCode());
            assertEquals(2, e.getResultCode().getCode());
            assertEquals("每页条数需在1-50之间", e.getMessage());
        }
    }

    @Test
    void L3_页码小于1_沿用03文案() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> InterviewServiceImpl.validatePageParams(0, 10, null));
        assertEquals(ResultCode.PARAM_INVALID, e.getResultCode());
        assertEquals("页码不能小于1", e.getMessage());
    }

    @Test
    void statusFilter_非法值弹回_含内部中间态4() {
        for (Integer filter : new Integer[]{0, 4, 6, 99}) {
            BusinessException e = assertThrows(BusinessException.class,
                    () -> InterviewServiceImpl.validatePageParams(1, 10, filter), "filter=" + filter);
            assertEquals("筛选状态不支持", e.getMessage());
        }
    }

    @Test
    void statusFilter_合法域与缺省全通过() {
        assertDoesNotThrow(() -> InterviewServiceImpl.validatePageParams(1, 10, null));
        for (Integer filter : new Integer[]{1, 2, 3, 5}) {
            assertDoesNotThrow(() -> InterviewServiceImpl.validatePageParams(1, 10, filter), "filter=" + filter);
        }
    }

    // ---------- 追问树组装 ----------

    private InterviewQuestion row(long id, Long parentId, String strategy, Integer score,
                                  String comment, String userAnswer) {
        InterviewQuestion q = new InterviewQuestion();
        q.setId(id);
        q.setParentQuestionId(parentId);
        q.setQuestionContent("题面" + id);
        q.setStrategy(strategy);
        q.setScore(score);
        q.setScoreComment(comment);
        q.setUserAnswer(userAnswer);
        return q;
    }

    @Test
    void RF1_两层追问树_depth与字段逐格对拍() {
        // 主题行 id=10 → 追问 11(depth=1, 下钻) → 追问 12(depth=2, 换角度)；uk_parent 一父一子
        List<InterviewQuestion> rows = List.of(
                row(11L, 10L, "下钻", 4, "有深度", "我的回答11"),
                row(12L, 11L, "换角度", 3, "还行", "我的回答12"));
        Map<Long, List<InterviewQuestion>> byParent = rows.stream()
                .collect(Collectors.groupingBy(InterviewQuestion::getParentQuestionId));

        List<ReportFollowupVO> tree = InterviewServiceImpl.buildFollowupChildren(10L, byParent, 1);
        assertEquals(1, tree.size(), "uk_parent：一层至多一子");

        ReportFollowupVO depth1 = tree.get(0);
        assertEquals(11L, depth1.getId());
        assertEquals(1, depth1.getDepth());
        assertEquals("下钻", depth1.getStrategy(), "策略中文字面直出");
        assertEquals(4, depth1.getScore());
        assertEquals("有深度", depth1.getScoreComment());
        assertEquals("我的回答11", depth1.getUserAnswer(), "userAnswer 必留，与主题行对称");

        List<ReportFollowupVO> depth2List = depth1.getFollowups();
        assertEquals(1, depth2List.size());
        ReportFollowupVO depth2 = depth2List.get(0);
        assertEquals(12L, depth2.getId());
        assertEquals(2, depth2.getDepth(), "RF1：depth=1/2 沿父链派生");
        assertEquals("换角度", depth2.getStrategy());
        assertEquals(3, depth2.getScore());
        assertEquals("还行", depth2.getScoreComment());

        assertEquals(List.of(), depth2.getFollowups(), "叶子=空数组不是缺字段");
    }

    @Test
    void RF2_无追问_主题行挂空数组() {
        Map<Long, List<InterviewQuestion>> empty = Map.of();
        List<ReportFollowupVO> tree = InterviewServiceImpl.buildFollowupChildren(10L, empty, 1);
        assertTrue(tree.isEmpty(), "无追问=空数组，绝不缺字段/null");
    }

    }
