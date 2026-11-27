export type ReviewOutcome = 'mastered' | 'needs-review'

export type ReviewTask = {
  id: string
  title: string
  domain: string
  role: string
  source: string
  score: string
  due: string
  priority: '优先复习' | '今日到期'
  prompt: string
  answer: string
  feedback: string
  explanation: string
  suggestedOutcome: ReviewOutcome
}

export const reviewTasks: ReviewTask[] = [
  {
    id: 'mysql-index',
    title: '联合索引的最左匹配原则',
    domain: 'MySQL',
    role: 'Java 后端',
    source: '来自面试报告 · 2 / 5 分',
    score: '2 / 5',
    due: '已逾期 1 天',
    priority: '优先复习',
    prompt: '请用自己的话解释联合索引的最左匹配原则，并举一个无法有效使用索引的查询例子。',
    answer: '你能说出索引字段顺序会影响查询，但还没有说明中间列缺失时为什么无法继续利用后续列。',
    feedback: '方向正确，机制解释还不完整。回答提到了字段顺序，但缺少索引树有序性与范围条件对后续列的影响。',
    explanation: '联合索引会按定义顺序形成有序结构。查询通常需要从最左列开始连续匹配；跳过中间列后，后续列无法独立定位。遇到范围查询时，范围列之后的列通常也难以继续用于精确定位。',
    suggestedOutcome: 'needs-review',
  },
  {
    id: 'mvcc',
    title: 'MVCC 与可见性判断',
    domain: '事务与并发',
    role: 'Java 后端',
    source: '来自面试报告 · 3 / 5 分',
    score: '3 / 5',
    due: '今日到期',
    priority: '今日到期',
    prompt: 'MVCC 如何帮助 InnoDB 在保证隔离性的同时减少读写互相阻塞？',
    answer: '你提到了快照读和版本，但 Read View 的作用还没有说清。',
    feedback: '核心概念已建立。补上 Read View 如何判断记录版本是否对当前事务可见，答案会更完整。',
    explanation: 'MVCC 为记录保留多个版本。快照读根据 Read View 中的事务边界判断版本可见性，从而读取历史版本，不必等待正在修改记录的事务释放锁。锁定读仍会按锁规则执行。',
    suggestedOutcome: 'needs-review',
  },
  {
    id: 'project-star',
    title: '项目经历中的个人贡献',
    domain: '项目表达',
    role: 'Java 后端',
    source: '来自面试报告 · 3 / 5 分',
    score: '3 / 5',
    due: '今日到期',
    priority: '今日到期',
    prompt: '用一个具体项目说明你解决了什么问题、承担了什么工作，结果如何衡量。',
    answer: '你说明了项目背景和使用的技术，但个人负责边界及结果证据偏少。',
    feedback: '背景和方案有交代。建议把“团队做了什么”收束为“我负责什么”，并补充可验证的结果。',
    explanation: '回答可以按情境、任务、行动、结果组织。结果不一定是宏观指标，也可以是耗时变化、故障减少、流程简化或明确的交付范围。',
    suggestedOutcome: 'needs-review',
  },
  {
    id: 'thread-pool',
    title: '线程池队列与拒绝策略',
    domain: 'Java 并发',
    role: 'Java 后端',
    source: '来自面试报告 · 4 / 5 分',
    score: '4 / 5',
    due: '今日到期',
    priority: '今日到期',
    prompt: '线程池工作队列持续堆积时，你会先观察哪些指标，又会如何判断是否触发拒绝策略？',
    answer: '你提到队列长度、活跃线程数和拒绝策略，排查顺序比较清楚。',
    feedback: '回答结构清楚。再补充队列容量、任务耗时和下游处理能力之间的关系，可以形成完整判断。',
    explanation: '将活跃线程、队列长度、任务等待时间、拒绝计数和下游延迟放在一起观察。队列持续增长通常意味着到达速率长期高于处理速率，单纯扩大队列可能延迟失败并增加内存压力。',
    suggestedOutcome: 'mastered',
  },
  {
    id: 'rag-recall',
    title: 'RAG 检索召回与重排',
    domain: 'RAG',
    role: 'Agent 工程',
    source: '来自面试报告 · 2 / 5 分',
    score: '2 / 5',
    due: '今日到期',
    priority: '优先复习',
    prompt: '检索阶段召回不足时，你会从哪些环节定位问题？重排模型能解决什么，不能解决什么？',
    answer: '还需要区分切块、召回和排序问题，并说明重排不能找回未召回的文档。',
    feedback: '已经意识到检索链路有多个环节。回答需要把召回与排序拆开，并明确重排的输入边界。',
    explanation: '先看文档解析与切块，再看 embedding、查询改写、向量或混合检索的召回率，最后评估重排。重排只能重新排列已召回候选，不能补回候选集中不存在的相关文档。',
    suggestedOutcome: 'needs-review',
  },
  {
    id: 'agent-tools',
    title: '工具调用的状态管理与失败恢复',
    domain: 'Agent 设计',
    role: 'Agent 工程',
    source: '来自模拟面试 · 3 / 5 分',
    score: '3 / 5',
    due: '今日到期',
    priority: '今日到期',
    prompt: 'Agent 调用外部工具超时后，你会怎样设计状态记录、重试和幂等保护？',
    answer: '你提到了重试，但还没有明确区分工具执行状态、重复请求和最终结果。',
    feedback: '能想到重试是好的起点。需要补充调用状态机、幂等键和超时后结果不确定时的恢复策略。',
    explanation: '记录每次调用的唯一标识与状态，例如 pending、succeeded、failed。重试前使用幂等键避免重复副作用；超时后先查询工具侧执行结果，无法确认时进入可恢复状态，而不是盲目重复执行。',
    suggestedOutcome: 'needs-review',
  },
  {
    id: 'browser-render',
    title: '浏览器渲染与长任务',
    domain: '浏览器机制',
    role: '前端工程',
    source: '来自面试报告 · 3 / 5 分',
    score: '3 / 5',
    due: '今日到期',
    priority: '今日到期',
    prompt: '页面出现长任务时，主线程会受到什么影响？你会怎样定位并拆解它？',
    answer: '你提到了主线程阻塞和性能面板，但任务拆解方案还不够具体。',
    feedback: '现象判断正确。补充长任务如何阻塞输入响应，以及拆分任务、让出主线程的方式。',
    explanation: '长任务连续占用主线程时，浏览器无法及时处理输入和绘制。可先用 Performance 录制定位调用栈，再通过分片执行、任务调度或 Web Worker 将耗时工作拆分或移出主线程。',
    suggestedOutcome: 'needs-review',
  },
  {
    id: 'frontend-performance',
    title: '交互卡顿的性能定位顺序',
    domain: '性能优化',
    role: '前端工程',
    source: '来自模拟面试 · 3 / 5 分',
    score: '3 / 5',
    due: '今日到期',
    priority: '今日到期',
    prompt: '一个页面在输入时明显卡顿，你会怎样从现象定位到具体代码，并确定优化有效？',
    answer: '你提到了减少渲染和使用性能面板，但缺少可复现、测量与回归的完整路径。',
    feedback: '优化方向合理。先固定复现步骤和测量指标，再定位长任务或重复渲染，最后用同一场景对比结果。',
    explanation: '用 Performance 记录输入期间的主线程活动，检查长任务、布局与脚本开销。再用组件性能工具定位重复渲染。每次只改一个主要因素，并使用相同输入和设备条件比较交互延迟与渲染次数。',
    suggestedOutcome: 'needs-review',
  },
]

export const weakDomains = [
  { id: 'mysql', role: 'Java 后端', name: 'MySQL', count: 3, mastery: 42, detail: '索引与事务仍需巩固', taskId: 'mysql-index', tone: 'warm' },
  { id: 'concurrency', role: 'Java 后端', name: '事务与并发', count: 2, mastery: 56, detail: 'MVCC 可见性判断', taskId: 'mvcc', tone: 'cool' },
  { id: 'project', role: 'Java 后端', name: '项目表达', count: 2, mastery: 61, detail: '个人贡献与结果证据', taskId: 'project-star', tone: 'green' },
  { id: 'java', role: 'Java 后端', name: 'Java 并发', count: 1, mastery: 78, detail: '线程池机制', taskId: 'thread-pool', tone: 'blue' },
  { id: 'rag', role: 'Agent 工程', name: '检索增强生成', count: 3, mastery: 39, detail: '召回率与重排边界', taskId: 'rag-recall', tone: 'warm' },
  { id: 'agent-design', role: 'Agent 工程', name: 'Agent 设计', count: 2, mastery: 54, detail: '工具调用与状态管理', taskId: 'agent-tools', tone: 'cool' },
  { id: 'browser', role: '前端工程', name: '浏览器机制', count: 2, mastery: 51, detail: '渲染与主线程任务', taskId: 'browser-render', tone: 'warm' },
  { id: 'frontend-performance', role: '前端工程', name: '性能优化', count: 2, mastery: 67, detail: '性能定位与拆解', taskId: 'frontend-performance', tone: 'blue' },
]

export const mistakeQuestions = [
  { id: 1, category: '八股', domain: 'MySQL', title: '联合索引在范围查询后，为什么后续列难以继续用于过滤？', score: 2, answer: '回答提到了索引字段顺序，但没有解释有序结构和范围条件。', taskId: 'mysql-index', date: '11月25日' },
  { id: 2, category: '项目', domain: '项目经历', title: '在订单系统中，你个人负责的关键改动是什么？', score: 2, answer: '描述集中在团队方案，个人边界和结果指标不够明确。', taskId: 'project-star', date: '11月24日' },
  { id: 3, category: '场景', domain: 'Java 并发', title: '线程池队列突然持续增长，你会如何定位？', score: 3, answer: '排查方向正确，但没有说明任务耗时和下游延迟。', taskId: 'thread-pool', date: '11月22日' },
  { id: 4, category: '八股', domain: 'RAG', title: '重排模型能否解决召回阶段漏掉的相关文档？', score: 2, answer: '没有区分候选召回和候选排序两个环节。', taskId: 'rag-recall', date: '11月21日' },
  { id: 6, category: '场景', domain: 'Agent 设计', title: '工具调用超时后如何安全重试并防止重复副作用？', score: 3, answer: '提到了重试，但幂等和结果不确定时的处理还不清楚。', taskId: 'agent-tools', date: '11月21日' },
  { id: 5, category: '场景', domain: '浏览器机制', title: '长任务为什么会让页面交互变得迟钝？', score: 3, answer: '提到了主线程阻塞，但任务拆分方法不够具体。', taskId: 'browser-render', date: '11月20日' },
  { id: 7, category: '场景', domain: '性能优化', title: '你会如何定位输入卡顿并验证优化有效？', score: 3, answer: '需要补全复现、测量、定位和回归的顺序。', taskId: 'frontend-performance', date: '11月19日' },
]
