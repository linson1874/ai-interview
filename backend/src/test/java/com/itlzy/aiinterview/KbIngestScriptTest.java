package com.itlzy.aiinterview;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 合同08 K4 入库管线（手动触发·付费几分钱）：12 道种子题 → 百炼变向量 → 一律 draft 入库。
 *  纪律三条：
 *  ① 真幂等（工单③'）：逐条对账——已入库题干跳过、缺失题补灌，重跑 N 次终点相同；
 *     embedding 成本同步幂等（只为新题掏钱）——07 抢单 CAS/D7 幂等删除的同款家法；
 *  ② 审核流：脚本只写 draft，approved 由人工 Navicat 过目后 UPDATE 放行（K4"人工改稿"落地，
 *     工单②的审核闸保证没过目的题检索永远看不见）；
 *  ③ V6 记账：tokens 总数打印在案，回填 K3 成本表（数字进文件不进印象，R2 家法）。 */
@SpringBootTest
class KbIngestScriptTest {

    private static final String SOURCE_TAG = "seed-v1";

    /** {题干, category题型(E1:八股/项目/场景/HR), difficulty(1-3), tags方向轴(逗号分隔), 答案知识段(工单⑤ #36裁决)} */
    private static final String[][] SEEDS = {
            {"Java 中 HashMap 的底层实现原理是什么？扩容是如何发生的？", "八股", "2", "Java,Spring",
             "结论：数组+链表+红黑树（JDK8起）。定位：hash扰动（高16位异或低16位）后与(容量-1)按位与取桶。冲突：同桶挂链表，链表长度≥8且容量≥64才树化，查询从O(n)变O(logn)。扩容：元素数超过阈值(容量×0.75)翻倍；JDK8不再逐元素rehash，链表按原元素高位是0还是1拆成两条链，要么留在原位要么平移到原位+旧容量，红黑树拆回链表。0.75是空间与冲突率的对换，源自泊松分布的经验值。"},
            {"== 和 equals() 的区别是什么？重写 equals 为什么必须重写 hashCode？", "八股", "1", "Java",
             "==：基本类型比值，引用类型比地址。equals：Object 默认实现就是 ==，String/Integer 等重写为比内容。重写 equals 必须重写 hashCode 源于哈希容器存取对称：HashMap 存时用 hashCode 定桶、取时同样先定桶再 equals 比对——若只改 equals，内容相等的对象 hashCode 不同，会被扔进不同的桶，get 永远找不到，Set 里还能同时存两份。契约四句：equals 相等则 hashCode 必须相等；反之不要求（哈希冲突合法）；对象参与集合期间 hashCode 不能变。"},
            {"简述 JVM 的垃圾回收机制，CMS 和 G1 收集器各有什么特点？", "八股", "3", "Java",
             "判定：可达性分析，GC Roots（栈引用、静态属性、常量）连不通的判死刑。分代：Eden+两个Survivor，minor GC 复制存活对象、年龄到阈值晋升老年代；老年代标记-清除或标记-整理。CMS：并发标记清除、停顿短，但有浮动垃圾和内存碎片。G1：堆划成几十个 Region，每次优先回收垃圾最多的区域，可设最大停顿目标，把 GC 从看运气变成看 SLA，JDK9 起默认。核心话术：分代是假设大部分对象朝生夕死换来的效率，G1 是大堆时代对停顿可预期性的工程化。"},
            {"Java 线程池的核心参数有哪些？拒绝策略如何选型？", "八股", "2", "Java,并发",
             "七大参数：核心线程数、最大线程数、空闲存活时间、单位、工作队列、线程工厂、拒绝策略。提交流程易错：先核心、再排队、队满才扩到最大、还满才拒绝——不是先扩满再排队。拒绝策略四种：Abort 抛异常（默认）、CallerRuns 调用者自己跑（天然反压降速）、Discard 丢、DiscardOldest 丢最老。选型：CPU 密集设核数+1，IO 密集 2 倍核数起步再压测调；队列必须有界（Executors 快捷方法埋的无界队列是 OOM 隐患，阿里手册明令禁止）；线程工厂给线程起业务名，排查堆栈时救命。"},
            {"MySQL 为什么选择 B+ 树作为索引结构而不是哈希表或红黑树？", "八股", "2", "MySQL",
             "对比哈希：哈希只答等值，范围查询只能全表扫，大量冲突后链变长退化；哈希是内存结构，磁盘上没法组织。对比红黑树：二叉树太高，千万行 20 多层，每层一次磁盘 IO 就是 20 次；B+ 树一个节点=一个 16K 页装几百个键，千万行三到四层，等值查询 3~4 次 IO 封顶。B+ 独有三件套：非叶节点只存键更矮胖；数据全在叶子且叶子双向链表串好，范围查询定位一条后顺着链走；查询路径长度稳定。一句话：磁盘时代 IO 次数是主要矛盾，B+ 树是把 IO 压到极致的结构。"},
            {"什么是聚簇索引和二级索引？回表是什么，如何避免？", "八股", "2", "MySQL",
             "聚簇索引：叶子节点直接放整行数据，InnoDB 按主键组织，表本身就是这个索引，所以每表只有一个。二级索引：叶子放的是索引列+主键值。回表：走二级索引查到主键后，还得拿主键去聚簇索引再查一次才拿到完整行——两次树高换一行数据。避免：覆盖索引，select 的列全在二级索引里就不用回表，explain 的 Extra 显示 Using index；优化套路是把 select * 改成按需列、或把常用查询列加进联合索引。顺带：主键要短要递增——短是二级索引叶子都揣着主键，越大越费空间；递增是避免随机插入导致页分裂。"},
            {"MySQL 事务的隔离级别有哪些？RR 级别如何解决幻读？", "八股", "3", "MySQL",
             "四级：读未提交（脏读）、读已提交（不可重复读）、可重复读（MySQL 默认，剩幻读：范围查询多出一行）、串行化（全防，靠排队牺牲并发）。InnoDB 的 RR 防幻读两条腿：快照读（普通 select）靠 MVCC，进事务拍了版本快照，别人提交的新行对我不可见；当前读（for update/更新语句）靠 Next-Key Lock=记录锁+间隙锁，把范围中间的缝也锁住，别的事务想插新行被阻塞。加分句：RR 下并非绝对无幻读——快照读和当前读混用时可能看见新行，隔离级别防的是本事务自己的读一致性。"},
            {"Redis 的 RDB 和 AOF 持久化各有什么优劣？生产环境如何选？", "八股", "2", "Redis",
             "RDB：定时把内存快照 fork 子进程写二进制。优点：文件紧凑恢复就是回灌，重启秒起，主从同步也传它；缺点：两次快照之间的数据宕机即丢，fork 瞬间有开销。AOF：每条写命令追加日志，恢复靠重放。安全性可选 always（每条刷盘，慢）/everysec（默认，最多丢一秒）/no；缺点：文件比 RDB 大、重放比回灌慢，靠重写压缩。生产标配两者同开：AOF 保数据、RDB 做定期兜底和复制种子；Redis 7 的混合 AOF 把 RDB 作为 AOF 底座，重写时增量续命令，兼得恢复快与安全。收尾话术：持久化没有银弹，是丢多少和起多慢的双约束优化。"},
            {"如何用 Redis 实现分布式锁？有哪些坑？", "场景", "3", "Redis,Java",
             "正确姿势一条命令：SET key uuid NX PX 30000——NX 抢锁、PX 过期防死锁、value 放本次请求唯一 uuid 证明锁是我的。解锁必须 Lua：先比对 uuid 再 DEL，两步原子——否则你的锁超时后被 B 抢到，你裸 DEL 删掉的是 B 的锁。坑一层层数：业务没跑完锁先过期→看门狗自动续期（Redisson 每 1/3 周期回血）；主从架构主节点带锁宕机，从节点没同步到锁；RedLock 多节点方案争议大，多数场景承认极端窗口、靠业务幂等兜底；可重入需要 value 记线程+计数（Redisson 的 Hash 实现）。选型实话：锁只解决少犯错，不解决绝对不重叠，钱相关的场景还得数据库唯一约束兜底。"},
            {"Spring Boot 的自动配置原理是什么？@ConditionalOnMissingBean 起什么作用？", "八股", "2", "Spring,Java",
             "链路：@SpringBootApplication 里的 @EnableAutoConfiguration 通过 AutoConfigurationImportSelector 读各 starter 的 META-INF/spring/AutoConfiguration.imports 清单，批量注册候选配置类，每个类头上挂条件注解过滤：@ConditionalOnClass（jar 在不在）、@ConditionalOnProperty（开关开没开）、@ConditionalOnMissingBean（用户自己建过没有）。@ConditionalOnMissingBean 是约定优于配置的机关：自动配置让位用户配置——我们双数据源那天 DataSourceAutoConfiguration 就是这么退位的；反向的坑是 @Primary 漏写时 @ConditionalOnSingleCandidate 静默罢工。话术收口：starter=依赖+自动配置+默认值的三件套，换 starter 即换基建。"},
            {"你实习/项目里遇到的最有挑战的技术问题是什么？你是如何定位和解决的？", "项目", "2", "行为面",
             "STAR 模板（学员需替换为自己真实经历）：情境——异步报告偶发被提前抢单重做；任务——定位竞态真因；行动——对比同列两个时钟写入的时间差 8 小时，顺藤摸到容器 UTC 与 Java 本地时间双源，排查发现清扫条件形同虚设；结果——容器加 TZ 一处根治，顺带教会我判断异常看 Caused by、时间问题查时钟源。格式照 S→T→A→R 四拍写，每拍一句，行动拍占一半篇幅——面试官听的是你排查的动作序列，不是结论。"},
            {"如果线上接口偶发超时，你的排查思路是什么？", "场景", "3", "Java,中间件",
             "偶发决定打法：单点复现大概率徒劳，要靠持续观测抓分布。第一层定位面：监控/链路追踪看超时集中在哪个接口、哪个时段、哪台机器、P99 还是均值劣化——先圈范围再动手。第二层分层排除：DB（慢查询日志、连接池指标）、Redis（大 key、热 key、慢日志）、GC（full GC 停顿日志，偶发尖刺头号嫌犯）、下游依赖（第三方接口毛刺）、线程池（队列积压、拒绝计数）、网络（重传率、DNS）。常见根因排序：GC 停顿>锁竞争>连接池耗尽>慢查询雪崩>下游拖累。工具三板斧：链路追踪看形状、arthas 现场 trace 方法耗时、日志按 traceId 串。收尾话术：先止血再定位，偶发问题的证据比结论重要。"},
    };

    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    void ingestSeedQuestions() {
        // 真幂等第二跳（工单⑤）：逐条对账分三路——题干缺→INSERT(要 embed)；题干在但答案空→UPDATE 补答案(零 embed)；全齐→跳过
        List<Map<String, Object>> rows = pgJdbcTemplate.queryForList(
                "SELECT content, answer_text FROM kb_question WHERE source = ?", SOURCE_TAG);
        Map<String, String> answerByContent = new HashMap<>();
        for (Map<String, Object> row : rows) {
            answerByContent.put((String) row.get("content"), (String) row.get("answer_text"));
        }
        List<String[]> toIngest = new ArrayList<>();
        int answerFilled = 0;
        for (String[] seed : SEEDS) {
            if (!answerByContent.containsKey(seed[0])) {
                toIngest.add(seed);
            } else if (answerByContent.get(seed[0]) == null) {
                pgJdbcTemplate.update("UPDATE kb_question SET answer_text = ? WHERE source = ? AND content = ?",
                        seed[4], SOURCE_TAG, seed[0]);
                answerFilled++;
            }
        }
        if (answerFilled > 0) {
            System.out.println("[答案补写] UPDATE " + answerFilled + " 条已有题的 answer_text（0 次 embed）");
        }
        if (toIngest.isEmpty()) {
            System.out.println("[幂等完成] seed-v1 全部 " + SEEDS.length + " 题题干+答案齐备，无事可做（0 次 embed，0 花费）");
            return;
        }

        List<String> texts = toIngest.stream().map(s -> s[0]).toList();
        // 分批护栏：百炼 embeddings 单请求 batch<=10（实弹 2026-11-18 HTTP 400 "should not be larger than 10"）——
        // OpenAI 兼容协议只保门面不保厂商私规，批量上限这类私规靠自己文档化防御
        int batchSize = 10;
        List<float[]> vectors = new ArrayList<>();
        int tokens = 0;
        for (int from = 0; from < texts.size(); from += batchSize) {
            List<String> batch = texts.subList(from, Math.min(from + batchSize, texts.size()));
            EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(batch, null));
            assertEquals(batch.size(), response.getResults().size(), "批量返回条数与输入不符=上游行为变了");
            for (Embedding e : response.getResults()) vectors.add(e.getOutput());
            tokens += response.getMetadata().getUsage().getPromptTokens();
        }

        for (int i = 0; i < toIngest.size(); i++) {
            String[] seed = toIngest.get(i);
            pgJdbcTemplate.update(
                    "INSERT INTO kb_question (content, category, difficulty, tags, status, source, embedding, embedding_model, answer_text) " +
                            "VALUES (?, ?, ?, ?::text[], 'draft', ?, ?::vector, ?, ?)",
                    seed[0], seed[1], Integer.parseInt(seed[2]),
                    "{" + seed[3] + "}",
                    SOURCE_TAG, toVectorLiteral(vectors.get(i)), "text-embedding-v4", seed[4]);
        }
        System.out.println("[V6 记账] skipped=" + (SEEDS.length - toIngest.size())
                + " new=" + toIngest.size() + " tokens合计=" + tokens
                + "（均值=" + tokens / toIngest.size() + "/题）");
        System.out.println("下一步（人工审核）：只有新题是 draft，Navicat 抽查后执行——");
        System.out.println("  UPDATE kb_question SET status='approved' WHERE source='" + SOURCE_TAG + "' AND status='draft';");
    }

    /** float[] → pgvector 字面量 "[a,b,c...]"——和检索面 ?::vector 同一方言 */
    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}