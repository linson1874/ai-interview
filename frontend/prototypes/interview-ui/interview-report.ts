export type ReportTurn = {
  role: '面试官' | '我'
  text: string
  score?: number
  comment?: string
}

export type ReportQuestion = {
  id: number
  category: '八股' | '项目' | '场景' | 'HR'
  title: string
  topic: string
  score: number | null
  mastered: boolean
  answer: string
  comment: string
  dialogue: ReportTurn[]
}

export const interviewReportQuestions: ReportQuestion[] = [
  {
    id: 1, category: '项目', topic: '项目经历', score: 4, mastered: true,
    title: '介绍一下你在订单系统中的核心设计，以及你负责的部分。',
    answer: '我主要负责订单创建和库存扣减的接口，使用 Spring Boot 和 MySQL。订单与库存更新放在同一个事务里，并用请求号避免重复提交。',
    comment: '职责边界和关键设计交代清楚。结果数据略少，可以补充并发量或异常率等实际指标。',
    dialogue: [
      { role: '面试官', text: '介绍一下你在订单系统中的核心设计，以及你负责的部分。' },
      { role: '我', text: '我主要负责订单创建和库存扣减的接口，使用 Spring Boot 和 MySQL。订单与库存更新放在同一个事务里，并用请求号避免重复提交。' },
      { role: '面试官', text: '请求号是怎么保证幂等的？如果两个相同请求同时到达呢？', score: 3, comment: '抓住了重复提交问题，但没有解释唯一约束冲突后的处理方式。' },
      { role: '我', text: '请求号会建立唯一索引，冲突时查询并返回已创建的订单，不重复扣库存。', score: 4, comment: '补充了唯一约束与冲突分支，回答形成闭环。' },
    ],
  },
  {
    id: 2, category: '八股', topic: 'MySQL · 索引', score: 2, mastered: false,
    title: 'MySQL 的 B+ 树索引为什么适合范围查询？',
    answer: 'B+ 树的叶子节点存数据，树的高度比较低，所以查找比较快。范围查询可以从一个位置开始往后查。',
    comment: '知道树高与叶子顺序的基本结论，但没有说明双向链表如何支持范围扫描，也未区分聚簇与二级索引。',
    dialogue: [
      { role: '面试官', text: 'MySQL 的 B+ 树索引为什么适合范围查询？' },
      { role: '我', text: 'B+ 树的叶子节点存数据，树的高度比较低，所以查找比较快。范围查询可以从一个位置开始往后查。' },
      { role: '面试官', text: '“往后查”具体依赖什么结构？如果查询字段不在联合索引最左列，会发生什么？', score: 2, comment: '追问命中了范围查询和最左前缀两个核心边界。' },
      { role: '我', text: '叶子节点之间会有链表连接。最左列不匹配时，索引可能无法按预期使用。', score: 2, comment: '方向基本正确，但没有说明双向链表、回表和索引条件下推。' },
    ],
  },
  {
    id: 3, category: '场景', topic: '故障排查', score: 3, mastered: true,
    title: '接口偶发超时，但数据库监控没有明显异常，你会如何排查？',
    answer: '先根据 traceId 看链路耗时，再拆分应用、数据库和下游服务的耗时，重点观察线程池、连接池和慢请求。',
    comment: '排查顺序合理，能从链路而非单一组件切入。可以继续说明超时分位数和线程池队列的判断方法。',
    dialogue: [
      { role: '面试官', text: '接口偶发超时，但数据库监控没有明显异常，你会如何排查？' },
      { role: '我', text: '先根据 traceId 看链路耗时，再拆分应用、数据库和下游服务的耗时，重点观察线程池、连接池和慢请求。', score: 3, comment: '给出了可执行的观测路径。' },
      { role: '面试官', text: '如果 p99 明显升高，但平均耗时正常，你会优先看什么？', score: 3, comment: '追问进一步检查长尾请求，不被平均值掩盖。' },
      { role: '我', text: '会按接口和下游依赖拆分 p99，再看超时请求是否集中在特定实例或线程池队列堆积。', score: 4, comment: '补充了分组维度和实例差异，分析方向明确。' },
    ],
  },
  {
    id: 4, category: 'HR', topic: '沟通协作', score: 1, mastered: false,
    title: '讲一次你收到负面反馈后调整做法的经历。',
    answer: '之前有一次项目进度慢，后来我会更努力，也会多和同学沟通。',
    comment: '回答停留在态度，没有交代反馈内容、采取的行动和结果，难以判断改变是否有效。',
    dialogue: [
      { role: '面试官', text: '讲一次你收到负面反馈后调整做法的经历。' },
      { role: '我', text: '之前有一次项目进度慢，后来我会更努力，也会多和同学沟通。', score: 1, comment: '缺少具体事件和结果。' },
      { role: '面试官', text: '你收到的反馈是什么？后来具体改变了哪一个做法？', score: 1, comment: '尝试追问可验证的行为变化。' },
      { role: '我', text: '主要是沟通变多了，之后项目就推进得顺一些。', score: 1, comment: '仍没有说明沟通机制或推进结果，证据不足。' },
    ],
  },
  {
    id: 5, category: '八股', topic: 'Java · 并发', score: 4, mastered: true,
    title: 'Java 线程池的核心参数分别影响什么？',
    answer: '核心线程数和最大线程数控制并发，队列承接暂时的任务积压，拒绝策略决定饱和后的处理方式。还要结合任务类型和下游容量设置。',
    comment: '覆盖主要参数，也考虑到下游承载能力。补充参数变化顺序和压测依据会更完整。',
    dialogue: [
      { role: '面试官', text: 'Java 线程池的核心参数分别影响什么？' },
      { role: '我', text: '核心线程数和最大线程数控制并发，队列承接暂时的任务积压，拒绝策略决定饱和后的处理方式。还要结合任务类型和下游容量设置。', score: 4, comment: '说明了参数作用与容量约束。' },
      { role: '面试官', text: '队列设得很大，会带来什么风险？', score: 3, comment: '检查对排队延迟和内存占用的理解。' },
      { role: '我', text: '任务可能长时间排队，增加延迟，也可能造成大量对象占用内存。', score: 4, comment: '准确指出排队延迟和内存压力。' },
    ],
  },
  {
    id: 6, category: '项目', topic: '技术决策', score: null, mastered: false,
    title: '你做过但最终没有采用的技术方案是什么？',
    answer: '这一题结束前没有提交完整回答。',
    comment: '本题未完成作答，因此不计入平均分。',
    dialogue: [{ role: '面试官', text: '你做过但最终没有采用的技术方案是什么？' }, { role: '我', text: '这一题结束前没有提交完整回答。' }],
  },
]

export const reportStrengths = [
  {
    title: '项目回答有实际支撑',
    detail: '能说明自己负责的接口、事务边界和幂等处理，回答不只停留在技术名词。',
  },
  {
    title: '排查思路比较完整',
    detail: '面对故障场景时，会先看链路，再拆分应用、数据库和下游依赖，步骤清楚且可执行。',
  },
]

export const reportImprovements = [
  {
    title: '基础机制解释还不够深入',
    detail: '部分回答能说出结论，但原理、适用条件和边界讲得不够完整；遇到追问时，补充解释的稳定性也有波动。',
  },
  {
    title: '经历描述缺少结果证据',
    detail: '谈到做法时较具体，但对个人贡献、前后变化和结果依据交代偏少，面试官不容易判断实际影响。',
  },
  {
    title: '表达收尾可以更明确',
    detail: '个别回答停在处理过程，没有用结论或结果收束；建议先给判断，再解释依据，最后说明结果。',
  },
]
