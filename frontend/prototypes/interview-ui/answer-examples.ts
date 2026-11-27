export type Topic = {
  title: string
  question: string
  followup: string
  transcript: string
  keyword?: string
}

export type Exchange = { speaker: 'interviewer' | 'candidate'; text: string; keyword?: string }
export type TopicHistory = { title: string; exchanges: Exchange[] }
export type AnswerSession = {
  role: string
  round: string
  format: '文字' | '语音'
  difficulty?: string
  usesResume?: boolean
  initialQuestion?: string
  seeded?: boolean
}

export const frontendTopics: Topic[] = [
  {
    title: '浏览器渲染机制',
    question: '浏览器收到 HTML 后，页面是怎样一步步显示出来的？',
    followup: '哪些改动只会触发重绘？',
    transcript: '浏览器解析 DOM 和样式，然后计算布局、绘制并合成。',
  },
  {
    title: '前端性能优化',
    question: '如果页面首屏加载慢，你会先从哪里排查？',
    followup: '怎样判断问题出在网络还是主线程？',
    transcript: '我会先看请求瀑布和主线程任务，判断是资源加载还是执行时间过长。',
  },
  {
    title: '首屏体验',
    question: '一个商品页打开很慢。你会怎么定位首屏性能问题？',
    followup: '你提到了最大内容绘制。假如首屏大图已经提前加载，为什么 LCP 仍可能偏高？',
    transcript: '图片下载完还需要解码和绘制。如果主线程正被长任务阻塞，实际显示时间也会推迟。',
    keyword: '最大内容绘制',
  },
  {
    title: '组件设计',
    question: '一个列表组件被多个页面复用时，你会如何设计它的接口？',
    followup: '页面需要不同的空状态时，组件该怎么支持？',
    transcript: '我会把数据和操作暴露为明确的输入与事件，用插槽承载业务展示差异。',
  },
  {
    title: '请求与状态',
    question: '搜索请求较多时，怎么避免旧响应覆盖新结果？',
    followup: '搜索结果有缓存时，你会如何处理加载状态？',
    transcript: '新请求发出时可以取消上一次请求，落状态前也会核对响应是否对应当前查询。',
  },
  {
    title: '可访问性',
    question: '怎样验证一个对话框能完整地用键盘操作？',
    followup: '关闭对话框后，焦点应该回到哪里？',
    transcript: '我会检查初始焦点、Tab 顺序、Escape 关闭和关闭后的焦点恢复。',
  },
]

export const exampleHistory: TopicHistory[] = [
  {
    title: '浏览器渲染机制',
    exchanges: [
      { speaker: 'interviewer', text: frontendTopics[0]!.question },
      { speaker: 'candidate', text: frontendTopics[0]!.transcript },
      { speaker: 'interviewer', text: frontendTopics[0]!.followup },
      { speaker: 'candidate', text: '颜色变化通常只需重绘，尺寸变化往往需要重新布局。' },
    ],
  },
  {
    title: '前端性能优化',
    exchanges: [
      { speaker: 'interviewer', text: frontendTopics[1]!.question },
      { speaker: 'candidate', text: frontendTopics[1]!.transcript },
      { speaker: 'interviewer', text: frontendTopics[1]!.followup },
      { speaker: 'candidate', text: '请求瀑布可以定位资源等待，性能面板可以分析长任务。' },
    ],
  },
]

const roleScenarios: Partial<Record<string, Topic>> = {
  'Agent 开发实习生': {
    title: '工具调用与评估',
    question: '一个客服 Agent 会查询订单，也会发起退款。你会怎样设计工具调用流程，避免在用户只是咨询时误发起退款？',
    followup: '你会用哪些测试场景判断改动减少了误调用，同时没有拦下正常退款？',
    transcript: '查询和退款使用不同的工具，退款前要求用户确认。测试时要分别覆盖查询、真正的退款意图和表述模糊的请求。',
  },
  'Java 后端开发实习生': {
    title: '订单事务',
    question: '一个 Spring Boot 接口需要创建订单并扣减库存。你会怎样划分事务边界，避免只完成其中一步？',
    followup: '如果调用方在超时后重复发送创建订单请求，你会如何避免重复扣减库存？',
    transcript: '我会把订单写入和库存扣减放进同一个数据库事务，再通过唯一的业务请求号处理重复请求。',
  },
  'Python 后端开发实习生': {
    title: '异步接口设计',
    question: '一个 FastAPI 接口要调用两个彼此独立的外部服务。你会怎样安排请求，并处理其中一个服务超时的情况？',
    followup: '如果其中一个客户端库只提供同步方法，它对异步接口会有什么影响？',
    transcript: '我会并发发起两个调用，为它们设置独立的超时，并根据接口契约决定是否允许返回部分结果。',
  },
}

export function generalTopics(role: string): Topic[] {
  return [
    { title: '岗位理解', question: `你认为${role}最重要的职责是什么？`, followup: '遇到优先级冲突时，你会怎么安排？', transcript: '我会先确认目标，再根据交付时间和影响安排优先级。' },
    { title: '经历复盘', question: '请介绍一段和目标岗位有关的经历。你具体负责什么？', followup: '如果重来一次，你会调整什么？', transcript: '我负责梳理需求并推进交付，最后用反馈来验证结果。' },
    (Object.hasOwn(roleScenarios, role) ? roleScenarios[role] : undefined) ?? {
      title: '情境问答',
      question: '接到一项需求不明确的任务，你会怎么开始？',
      followup: '如果相关人员的期望不一致，你会怎样推进？',
      transcript: '我会与提出需求的人确认边界，并把关键决定同步给相关同事。',
    },
  ]
}
