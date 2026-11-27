package com.itlzy.aiinterview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itlzy.aiinterview.common.BusinessException;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.common.ReviewKeys;
import com.itlzy.aiinterview.config.ReviewProperties;
import com.itlzy.aiinterview.dao.KnowledgeSearchDao;
import com.itlzy.aiinterview.dto.ReviewAnswerDTO;
import com.itlzy.aiinterview.entity.InterviewQuestion;
import com.itlzy.aiinterview.entity.ReviewItem;
import com.itlzy.aiinterview.mapper.InterviewQuestionMapper;
import com.itlzy.aiinterview.mapper.ReviewItemMapper;
import com.itlzy.aiinterview.service.AiService;
import com.itlzy.aiinterview.service.ReviewService;
import com.itlzy.aiinterview.util.VectorUtils;
import com.itlzy.aiinterview.vo.ReviewAnswerVO;
import com.itlzy.aiinterview.vo.ReviewItemVO;
import com.itlzy.aiinterview.vo.ScoringSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 复习业务逻辑：错题入队/今日清单/作答判分/级联清理。
 *  纪律：钩子异常只记日志不影响主链；先写数据库账本再更新 Redis 投影；
 *  到期时间统一用 Java 时钟计算写入。
 *  @author 林子阳 */
@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewItemMapper reviewItemMapper;
    private final KnowledgeSearchDao searchDao;          // 知识库检索统一出口
    private final EmbeddingModel embeddingModel;
    private final StringRedisTemplate redisTemplate;
    private final ReviewProperties props;
    private final InterviewQuestionMapper questionMapper;
    private final AiService aiService;
    private final ReviewTxService txService;

    public ReviewServiceImpl(ReviewItemMapper reviewItemMapper,
                             KnowledgeSearchDao searchDao,
                             EmbeddingModel embeddingModel,
                             StringRedisTemplate redisTemplate,
                             ReviewProperties props,
                             InterviewQuestionMapper questionMapper,
                             AiService aiService,
                             ReviewTxService txService) {
        this.reviewItemMapper = reviewItemMapper;
        this.searchDao = searchDao;
        this.embeddingModel = embeddingModel;
        this.redisTemplate = redisTemplate;
        this.props = props;
        this.questionMapper = questionMapper;
        this.aiService = aiService;
        this.txService = txService;
    }

    @Override
    public void enqueueIfWrong(Long userId, InterviewQuestion question, Integer score) {
        try {
            // 只有低分错题入队
            if (score == null || score > props.getDemoteMaxScore()) {
                return;
            }

            // 追问行沿链回溯到根原题，入队对象始终是根题（复习的是知识缺陷本体）
            InterviewQuestion target = question;
            while (target.getParentQuestionId() != null) {
                target = questionMapper.selectById(target.getParentQuestionId());
                if (target == null) {
                    log.error("复习入队回溯断链，放弃 questionId={}", question.getId());
                    return;
                }
            }

            // 尽力而为映射：检索题池命中则记录 id；失败不影响入队，材料降级为题干
            Long kbId = null;
            try {
                float[] vec = embeddingModel.embed(target.getQuestionContent());
                List<KnowledgeSearchDao.KbItem> hits = searchDao.search(VectorUtils.toLiteral(vec), null);
                if (!hits.isEmpty()) {
                    kbId = hits.get(0).getId();   // DAO 已按相似度阈值过滤，有命中即合格
                }
            } catch (Exception me) {
                log.warn("入队 kb 映射失败（材料降级为题干，不影响复习）questionId={}", target.getId(), me);
            }

            ReviewItem item = new ReviewItem();
            item.setUserId(userId);
            item.setInterviewQuestionId(target.getId());     // 身份锚=根题
            item.setKbQuestionId(kbId);
            item.setBoxNo(1);
            item.setStatus(ReviewKeys.STATUS_DUE);
            LocalDateTime next = LocalDateTime.now().plusDays(props.getIntervalsDays().get(0));
            item.setNextReviewAt(next);
            item.setLastScore(score);
            reviewItemMapper.insert(item);   // insert 回填自增 id，供投影使用

            // 先写账本再更新投影；score=到期时间戳（秒），与 next_review_at 同一 Java 时钟算出
            redisTemplate.opsForZSet().add(ReviewKeys.QUEUE_KEY, String.valueOf(item.getId()),
                    next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000.0);
            log.info("错题已入复习队列 itemId={} rootQuestionId={} fromQuestionId={} score={} kbId={}",
                    item.getId(), target.getId(), question.getId(), score, kbId);
        } catch (DuplicateKeyException dk) {
            // 唯一索引撞键=根题已在队列：幂等跳过
            log.info("题目已在复习队列，幂等跳过 questionId={}", question.getId());
        } catch (Exception e) {
            // 钩子失败只留痕不抛出，不影响已落库的评分结果
            log.error("复习入队钩子失败（不影响评分结果）questionId={}", question.getId(), e);
        }
    }

    @Override
    public List<ReviewItemVO> today(Long userId) {
        LocalDateTime now = LocalDateTime.now();   // 到期判断与写入使用同一个 Java 时钟
        List<ReviewItem> items = reviewItemMapper.selectList(new LambdaQueryWrapper<ReviewItem>()
                .eq(ReviewItem::getUserId, userId)
                .le(ReviewItem::getNextReviewAt, now)
                .orderByAsc(ReviewItem::getNextReviewAt)
                .last("LIMIT 20"));
        if (items.isEmpty()) {
            return List.of();
        }
        // 批量查题目，避免逐条打库
        List<Long> qids = items.stream().map(ReviewItem::getInterviewQuestionId).toList();
        Map<Long, InterviewQuestion> qMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(InterviewQuestion::getId, q -> q));
        return items.stream()
                .map(item -> toVO(item, qMap.get(item.getInterviewQuestionId())))
                .toList();
    }

    /** 复习项 VO 装配：材料按 题池答案 → 参考答案 → 无 三级降级，降级不报错 */
    private ReviewItemVO toVO(ReviewItem item, InterviewQuestion q) {
        ReviewItemVO vo = new ReviewItemVO();
        vo.setItemId(item.getId());
        vo.setBoxNo(item.getBoxNo());
        vo.setLastScore(item.getLastScore());
        vo.setNextReviewAt(item.getNextReviewAt());
        vo.setStatus(item.getStatus());
        vo.setQuestion(q == null ? "（原题行已不存在）" : q.getQuestionContent());
        String material = null;
        String source = "none";
        if (item.getKbQuestionId() != null) {
            KnowledgeSearchDao.KbItem kb = searchDao.selectById(item.getKbQuestionId());
            if (kb != null && kb.getAnswerText() != null && !kb.getAnswerText().isBlank()) {
                material = kb.getAnswerText();
                source = "kb";
            }
        }
        if (material == null && q != null && q.getRefAnswer() != null && !q.getRefAnswer().isBlank()) {
            material = q.getRefAnswer();
            source = "llm";
        }
        vo.setMaterial(material);
        vo.setMaterialSource(source);
        return vo;
    }

    @Override
    public ReviewAnswerVO answer(Long userId, ReviewAnswerDTO dto) {
        // 归属校验：不存在/他人统一 404
        ReviewItem item = reviewItemMapper.selectOne(new LambdaQueryWrapper<ReviewItem>()
                .eq(ReviewItem::getId, dto.getItemId())
                .eq(ReviewItem::getUserId, userId));
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }

        // 提交 CAS 防双击：到期待接；处理中的仅在租约过期后可接管
        LocalDateTime leaseDeadline = LocalDateTime.now().minusMinutes(props.getProcessingLeaseMinutes());
        int won = reviewItemMapper.update(null, new LambdaUpdateWrapper<ReviewItem>()
                .eq(ReviewItem::getId, item.getId())
                .and(w -> w.eq(ReviewItem::getStatus, ReviewKeys.STATUS_DUE)
                        .or(o -> o.eq(ReviewItem::getStatus, ReviewKeys.STATUS_PROCESSING)
                                .lt(ReviewItem::getUpdateTime, leaseDeadline)))
                .set(ReviewItem::getStatus, ReviewKeys.STATUS_PROCESSING)
                .set(ReviewItem::getUpdateTime, LocalDateTime.now()));  // 显式写时间：租约判据需要准确的处理时刻
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "该题正在复习中，请勿重复提交");
        }

        // CAS 已占 processing，后续任何失败在 finally 中归位到期，不让用户干等租约过期
        boolean settled = false;
        try {
            InterviewQuestion q = questionMapper.selectById(item.getInterviewQuestionId());
            if (q == null) {
                throw new BusinessException(ResultCode.BUSINESS_FAIL, "原题不存在，无法复习");
            }

            // 题池标签在事务外取好再传入回写，不把跨引擎调用裹进 MySQL 事务
            List<String> kbTags = List.of();
            if (item.getKbQuestionId() != null) {
                KnowledgeSearchDao.KbItem kb = searchDao.selectById(item.getKbQuestionId());
                if (kb != null) {
                    kbTags = kb.getTags();
                }
            }

            // 评分约 10 秒，保持在事务外；失败显式报错可重试
            ScoringSchema scored;
            try {
                scored = aiService.scoreQuestion(q.getQuestionContent(), q.getDifficulty(), dto.getAnswer());
            } catch (Exception e) {
                log.warn("复习判分失败 itemId={}", item.getId(), e);
                throw new BusinessException(ResultCode.BUSINESS_FAIL, "复习评分未成功，可重新提交");
            }

            // 事务内回写账本：携带读到的旧值作快照，若期间已被他人回写则快照不匹配，0 行命中拦截
            ReviewTxService.Outcome out = txService.writeBack(item, scored.getScore(), q.getCategory(), kbTags);
            if (out == null) {
                throw new BusinessException(ResultCode.BUSINESS_FAIL,
                        "本题复习结果已记录，本次判分作废（防盒号双跳）");
            }
            settled = true;

            // 投影更新失败只留日志，对账任务会自愈
            try {
                redisTemplate.opsForZSet().add(ReviewKeys.QUEUE_KEY, String.valueOf(item.getId()),
                        out.nextReviewAt().atZone(ZoneId.systemDefault())
                                .toInstant().toEpochMilli() / 1000.0);
            } catch (Exception pe) {
                log.warn("复习投影更新失败（对账将自愈）itemId={}", item.getId(), pe);
            }

            ReviewAnswerVO vo = new ReviewAnswerVO();
            vo.setScore(scored.getScore());
            vo.setComment(scored.getComment());
            vo.setRefAnswer(scored.getRefAnswer());
            vo.setBoxNo(out.boxNo());
            vo.setResult(out.result());
            vo.setNextReviewAt(out.nextReviewAt());
            return vo;
        } finally {
            // 补偿：未成功回写的失败把 processing 归位到期；成功路径下状态已变，此处 0 行无副作用
            if (!settled) {
                reviewItemMapper.update(null, new LambdaUpdateWrapper<ReviewItem>()
                        .eq(ReviewItem::getId, item.getId())
                        .eq(ReviewItem::getStatus, ReviewKeys.STATUS_PROCESSING)
                        .set(ReviewItem::getStatus, ReviewKeys.STATUS_DUE)
                        .set(ReviewItem::getUpdateTime, LocalDateTime.now()));
            }
        }
    }

    @Override
    public void purgeItemsForInterview(Long interviewId) {
        try {
            // 先取旧题 id 清单——必须在删题前调用
            List<Long> oldQuestionIds = questionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getInterviewId, interviewId))
                    .stream().map(InterviewQuestion::getId).toList();
            if (oldQuestionIds.isEmpty()) {
                return;
            }
            // 逻辑删除复习项，账本不物理抹除
            int purged = reviewItemMapper.delete(new LambdaQueryWrapper<ReviewItem>()
                    .in(ReviewItem::getInterviewQuestionId, oldQuestionIds));
            if (purged > 0) {
                // 投影无需手动移除：下一轮对账重建时已删成员自然消失
                log.info("级联清理：interviewId={} 逻辑删除复习单元 {} 条（锚点旧题 {} 行）",
                        interviewId, purged, oldQuestionIds.size());
            }
        } catch (Exception e) {
            // 清理失败不拦重生成：悬空锚由后续读取的降级逻辑兜底
            log.error("级联清理复习单元失败（不拦重生成，悬空锚走兜底边）interviewId={}", interviewId, e);
        }
    }
}
