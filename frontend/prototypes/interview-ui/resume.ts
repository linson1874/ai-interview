export type Dimension = {
  id: string
  dimension: string
  score: number
}

export type ResumeRecord = {
  id: number
  name: string
  role: string
  analyzed: boolean
}

export const exampleResumes: readonly ResumeRecord[] = [
  { id: 1, name: '陈嘉宁_Java后端.pdf', role: 'Java 后端开发实习生', analyzed: true },
  { id: 2, name: '陈嘉宁_前端开发.pdf', role: '前端开发实习生', analyzed: false },
]

export const overview = {
  verdict: '岗位方向明确，项目证据偏弱',
  summary: '技术栈和业务项目符合 Java 后端实习的基础要求。主要问题出在项目经历：应聘者的职责和性能结果都缺少足够的信息，面试官难以判断实践深度。',
}

export const dimensions: readonly Dimension[] = [
  { id: 'fit', dimension: '岗位匹配', score: 85 },
  { id: 'ownership', dimension: '个人贡献', score: 72 },
  { id: 'evidence', dimension: '成果可信度', score: 66 },
  { id: 'depth', dimension: '技术深度', score: 76 },
  { id: 'writing', dimension: '表达结构', score: 81 },
]

export const improvements = [
  {
    id: 'critical',
    title: '优先修正',
    items: [
      { title: '延迟降低 40% 的结果缺少验证依据', reason: '简历未说明是平均延迟还是 P95，也缺少前后数值和压测条件，无法判断这个结果是否来自两次可比的测试。' },
      { title: '缓存优化的个人职责没有交代', reason: '项目只写了团队接入 Redis。面试官无法分辨你负责方案、编码还是压测，也就很难判断哪些成果能归到你身上。' },
    ],
  },
  {
    id: 'major',
    title: '重要改进',
    items: [
      { title: '项目经历缺少技术决策', reason: '商品检索和订单模块写了做成的功能，但没有交代遇到的约束或方案取舍，读者难以区分常规实现和你解决过的具体问题。' },
    ],
  },
  {
    id: 'polish',
    title: '表达优化',
    items: [
      { title: '技能栏偏重工具名称', reason: 'Spring Boot、MySQL 和 Redis 都与岗位相关，但独立列出名称无法体现熟练程度，也不容易对应到做过的项目工作。' },
    ],
  },
]
