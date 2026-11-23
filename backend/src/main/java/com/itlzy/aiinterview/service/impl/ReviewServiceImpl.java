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

/**
 * 合同09 入队钩子（决策点 A+B+E 的最小落地）。三条纪律：
 * 1. 不连坐：全量 try-catch，钩子只写日志不动主链（R7"单行失败不炸整批"先例的跨域回声）；
 * 2. 账本先、投影后（E）：insert t_review_item 成功才 ZADD——ZADD 失败无所谓，
 *    重启全量重建兜底，/review/today 读账本不读投影；
 * 3. Java 单时钟（#15 移植）：next_review_at 用 LocalDateTime.now() 算好再写库，
 *    禁 DB now()——ZSET score 与 MySQL 列必须来自同一个钟。
 */
@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewItemMapper reviewItemMapper;
    private final KnowledgeSearchDao searchDao;          // 08 唯一出口复用：钩子不自己拼 PG SQL（门框纪律）
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
            // B 边界：只有 <=2 入队（与判分回盒用同一把尺=demoteMaxScore，一个参数两处执法）
            if (score == null || score > props.getDemoteMaxScore()) {
                return;
            }

            // 合同10 边3 三段裁决的循环化身：主题行(parent null)回溯 0 步=现行语义(a段)；
            // 追问行沿链上溯，入队对象=根原题(b段)——"永远落到根"同时消灭了追问行自身入池(c段)。
            // 复习的对象是知识缺陷本体，探针只是探针
            InterviewQuestion target = question;
            while (target.getParentQuestionId() != null) {
                target = questionMapper.selectById(target.getParentQuestionId());
                if (target == null) {
                    log.error("复习入队回溯断链，放弃 questionId={}", question.getId());
                    return;
                }
            }

            // A 尽力而为映射：复用 ask 同一条检索链（approved 闸在 DAO 常量里，钩子递不进条子）。
            // 映射失败≠复习失败：kb_question_id 留空，材料走题干降级——映射吃的是【根题】题干
            Long kbId = null;
            try {
                float[] vec = embeddingModel.embed(target.getQuestionContent());
                List<KnowledgeSearchDao.KbItem> hits = searchDao.search(VectorUtils.toLiteral(vec), null);
                if (!hits.isEmpty()) {
                    kbId = hits.get(0).getId();   // DAO 已按 distance-max 过滤：有 top1 即 d<=0.3
                }
            } catch (Exception me) {
                log.warn("入队 kb 映射失败（材料降级为题干，不影响复习）questionId={}", target.getId(), me);
            }

            ReviewItem item = new ReviewItem();
            item.setUserId(userId);
            item.setInterviewQuestionId(target.getId());     // 身份锚=根题行
            item.setKbQuestionId(kbId);
            item.setBoxNo(1);
            item.setStatus(ReviewKeys.STATUS_DUE);
            LocalDateTime next = LocalDateTime.now().plusDays(props.getIntervalsDays().get(0));
            item.setNextReviewAt(next);
            item.setLastScore(score);                        // 分数留本次判分（追问的砸分也是信号）
            reviewItemMapper.insert(item);   // IdType.AUTO：insert 回填 id，下一发 ZADD 的 member 就是它

            // 账本先、投影后（E）：score=epoch 秒，与 next_review_at 同一个 Java 时钟算出
            redisTemplate.opsForZSet().add(ReviewKeys.QUEUE_KEY, String.valueOf(item.getId()),
                    next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000.0);
            log.info("错题已入复习队列 itemId={} rootQuestionId={} fromQuestionId={} score={} kbId={}",
                    item.getId(), target.getId(), question.getId(), score, kbId);
        } catch (DuplicateKeyException dk) {
            // uk_user_question 撞键=根题已在队列（含"主题先答砸、追问又砸"的双触发）：幂等跳过
            log.info("题目已在复习队列，幂等跳过 questionId={}", question.getId());
        } catch (Exception e) {
            // 不连坐铁律：钩子可以坏，面试主链不能赔（评分三值已落库，此处只留痕不抛出）
            log.error("复习入队钩子失败（不影响评分结果）questionId={}", question.getId(), e);
        }
    }

    @Override
    public List<ReviewItemVO> today(Long userId) {
        LocalDateTime now = LocalDateTime.now();   // #15：到期判断用 Java 的钟，和写入同一个
        List<ReviewItem> items = reviewItemMapper.selectList(new LambdaQueryWrapper<ReviewItem>()
                .eq(ReviewItem::getUserId, userId)
                .le(ReviewItem::getNextReviewAt, now)
                .orderByAsc(ReviewItem::getNextReviewAt)
                .last("LIMIT 20"));
        if (items.isEmpty()) {
            return List.of();
        }
        // 批量取场次题（题干+refAnswer 材料二级），n<=20 一把 IN 查完，不逐条打库
        List<Long> qids = items.stream().map(ReviewItem::getInterviewQuestionId).toList();
        Map<Long, InterviewQuestion> qMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(InterviewQuestion::getId, q -> q));
        return items.stream()
                .map(item -> toVO(item, qMap.get(item.getInterviewQuestionId())))
                .toList();
    }

    /** A 三级阶梯的装配处：题池答案段 → refAnswer → 裸题干，降级永不报错只降材料 */
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
        // ① 归属门：id+userId 双条件，查无=404（03/05/06 同款语义第五次复用）
        ReviewItem item = reviewItemMapper.selectOne(new LambdaQueryWrapper<ReviewItem>()
                .eq(ReviewItem::getId, dto.getItemId())
                .eq(ReviewItem::getUserId, userId));
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在或已删除");
        }

        // ② F2① 提交 CAS（06 抢单第四次转世，防浏览器双击）：due 才接；
        //    processing 只在租约过期后接（kill -9 死锁洞的堵法，租约界限 Java 算好传参——#15）
        LocalDateTime leaseDeadline = LocalDateTime.now().minusMinutes(props.getProcessingLeaseMinutes());
        int won = reviewItemMapper.update(null, new LambdaUpdateWrapper<ReviewItem>()
                .eq(ReviewItem::getId, item.getId())
                .and(w -> w.eq(ReviewItem::getStatus, ReviewKeys.STATUS_DUE)
                        .or(o -> o.eq(ReviewItem::getStatus, ReviewKeys.STATUS_PROCESSING)
                                .lt(ReviewItem::getUpdateTime, leaseDeadline)))
                .set(ReviewItem::getStatus, ReviewKeys.STATUS_PROCESSING)
                .set(ReviewItem::getUpdateTime, LocalDateTime.now()));  // 租约心跳：update(null,wrapper) 不触发 MP fill，claim 时刻必须显式写——F2① 双击实弹炸出的修复
        if (won == 0) {
            throw new BusinessException(ResultCode.BUSINESS_FAIL, "该题正在复习中，请勿重复提交");
        }

        // ③~⑥ 全在 try 里：CAS 已占 processing，此后任何一步炸了都由 finally 归位 due——
        //    否则用户要为一次网络抖动等 30 分钟租约，那是惩罚用户不是保护账本
        boolean settled = false;
        try {
            InterviewQuestion q = questionMapper.selectById(item.getInterviewQuestionId());
            if (q == null) {
                throw new BusinessException(ResultCode.BUSINESS_FAIL, "原题不存在，无法复习");
            }

            // 方向轴数据源：kb 映射存在才可能进 tag 轴（A 的"宁缺毋滥"红线：tags 空=不进，不是报错）。
            // PG 读在事务外取好再喂 writeBack——不把跨引擎调用裹进 MySQL 回写事务里
            List<String> kbTags = List.of();
            if (item.getKbQuestionId() != null) {
                KnowledgeSearchDao.KbItem kb = searchDao.selectById(item.getKbQuestionId());
                if (kb != null) {
                    kbTags = kb.getTags();
                }
            }

            // 评分：07 链内部复用（C 裁决"内部复用不算动 07 契约"），HTTP 面零变化。
            // ~10s 的调用在事务外——长事务家法；炸了走 S13 明示边
            ScoringSchema scored;
            try {
                scored = aiService.scoreQuestion(q.getQuestionContent(), q.getDifficulty(), dto.getAnswer());
            } catch (Exception e) {
                log.warn("复习判分失败 itemId={}", item.getId(), e);
                throw new BusinessException(ResultCode.BUSINESS_FAIL, "复习评分未成功，可重新提交");
            }

            // 账本先（事务内）：快照=item ①门读到的 last_reviewAt——CAS 前旧值也安全：
            // 若 CAS 间隙别人已回写，本发快照必然不匹配，回写 0 行被 F2② 挡下
            ReviewTxService.Outcome out = txService.writeBack(item, scored.getScore(), q.getCategory(), kbTags);
            if (out == null) {
                throw new BusinessException(ResultCode.BUSINESS_FAIL,
                        "本题复习结果已记录，本次判分作废（防盒号双跳）");
            }
            settled = true;

            // 投影后：失败只留 warn——对账 5 分钟内搬运账本，不连坐已成功的回写
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
            // 补偿边：未走到成功回写的任何失败，把 processing 归位 due。
            // WHERE status=processing 是安全阀：成功路径下 status 已是 due，这发 0 行无副作用
            if (!settled) {
                reviewItemMapper.update(null, new LambdaUpdateWrapper<ReviewItem>()
                        .eq(ReviewItem::getId, item.getId())
                        .eq(ReviewItem::getStatus, ReviewKeys.STATUS_PROCESSING)
                        .set(ReviewItem::getStatus, ReviewKeys.STATUS_DUE)
                        .set(ReviewItem::getUpdateTime, LocalDateTime.now()));  // 同上：行变了时间就得变，账本卫生+租约判据不被旧值误导
            }
        }
    }

    @Override
    public void purgeItemsForInterview(Long interviewId) {
        try {
            // 先取旧题 id 清单——本方法必须在删题前跑，跑完这些 id 就没了（题表物理删除的时序债）
            List<Long> oldQuestionIds = questionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getInterviewId, interviewId))
                    .stream().map(InterviewQuestion::getId).toList();
            if (oldQuestionIds.isEmpty()) {
                return;
            }
            // MP 逻辑删除：@TableLogic 让 delete 变 UPDATE deleted=1——账本不物理抹尸（09 三表制哲学）
            int purged = reviewItemMapper.delete(new LambdaQueryWrapper<ReviewItem>()
                    .in(ReviewItem::getInterviewQuestionId, oldQuestionIds));
            if (purged > 0) {
                // 投影不用手动 ZREM：对账 SQL 的 selectList 被 MP 自动加 deleted=0 →
                // 下一轮对账（≤5min）重建时幽灵成员自然消失——账本先、投影后的又一次自动兑现
                log.info("工单G级联清理：interviewId={} 逻辑删除复习单元 {} 条（锚点旧题 {} 行）",
                        interviewId, purged, oldQuestionIds.size());
            }
        } catch (Exception e) {
            // 清理失败不拦重生成：旧 items 变悬空锚是已知边（4a 的"原题行已不存在"兜底接住），
            // 拦下整场重生成才是事故——主链优先，留痕待查
            log.error("级联清理复习单元失败（不拦重生成，悬空锚走兜底边）interviewId={}", interviewId, e);
        }
    }
}
