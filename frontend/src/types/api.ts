/**
 * 共享类型集中地（合同11 §0 TS 纪律）：
 * 接口形状以后端 VO 类与合同文档为唯一事实源，不凭前端想象造字段
 */

/** 后端统一响应包装。事实源：backend common/Result.java —— code/msg/data 三件 */
export interface Result<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 02 合同 §2.2：登录成功 data 形状（后端保证不含 password） */
export interface LoginVO {
  token: string
  nickname: string
}

/** 01 合同 §2.1：注册请求体（nickname 可空，镜像后端 DTO） */
export interface RegisterParams {
  username: string
  password: string
  nickname?: string
}

/** 02 合同 §2.1：登录请求体（后端仅 @NotBlank，前端不额外加规则） */
export interface LoginParams {
  username: string
  password: string
}

/** 03 合同 §4 D4：分页列表项（五字段——D4 裁决删除了虚构的 status，不含 selfIntro） */
export interface ResumeListItemVO {
  id: number
  realName: string
  targetJob: string
  skills: string[]
  createTime: string
}

/** 03 合同 §3：详情 = 列表五字段 + 选填两件（education/selfIntro 可为 null） */
export interface ResumeDetailVO extends ResumeListItemVO {
  education: string | null
  selfIntro: string | null
}

/** 03 合同 §4 D2 响应形状：MP 分页包装 */
export interface PageVO<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 03 合同 §2：保存请求体（新建/修改共用，D6 全量替换语义——可空字段为 null 即清空） */
export interface ResumeSaveParams {
  realName: string
  targetJob: string
  education: string | null
  skills: string[]
  selfIntro: string | null
}

/** 06 §2：创建面试响应（秒回 interviewId，T1） */
export interface InterviewCreatedVO {
  interviewId: number
}

/** 06 §2 + S10：题目项（id 是 07 作答端点 /answer/{qid} 的寻址凭据） */
export interface QuestionItemVO {
  id: number
  category: '八股' | '项目' | '场景' | 'HR'
  difficulty: 1 | 2 | 3
  question: string
}

/**
 * 06 §2 + R1：场次轮询面。questions 仅 status=1 时非空（R1 题目窗口条款，
 * 生成中 0/4 与失败 3 时为 null——前端看 status 决定画进度还是画题）
 */
export interface InterviewDetailVO {
  id: number
  status: 0 | 1 | 2 | 3 | 4
  questionCount: number
  createTime: string
  questions: QuestionItemVO[] | null
}

/** 07 §2：作答请求体（后端 AnswerDTO 镜像：@NotBlank、≤1000 字） */
export interface AnswerParams {
  answer: string
}

/** 07 §2 J2/S1：作答同步评分返回，三字段同批产出（S13 降级时 code=1 data=null，走拦截器 reject） */
export interface AnswerScoreVO {
  score: number
  comment: string
  refAnswer: string
}

/** 10 §E：模型用量收据（S1 回显衣钵，只随响应走不落库） */
export interface UsageVO {
  promptTokens: number
  completionTokens: number
}

/**
 * 10 §A/E：追问回执。strategy 为中文三值字面（#60 同一路字符串）；depth 是
 * 派生值随响应走不落库（D 反幽灵字段判例）；source 供审计展示不进话术；usage 仅 llm 路径非 null
 */
export interface FollowupVO {
  questionId: number
  question: string
  strategy: '下钻' | '换角度' | '降档'
  source: 'kb' | 'llm'
  depth: 1 | 2
  usage: UsageVO | null
}

/** 07 §2 J3/S3：报告逐题明细行（题面+作答+判分三值；score=null = 未评分，P11 分母剔除） */
export interface ReportDetailVO {
  id: number
  category: string
  difficulty: number
  question: string
  userAnswer: string | null
  score: number | null
  scoreComment: string | null
}

/** 07 §2 J3/S3：薄弱点（category 四值枚举闸门执法；evidence 引题 id 供前端溯源） */
export interface WeakPointVO {
  category: '八股' | '项目' | '场景' | 'HR'
  reason: string
  evidence: number[]
}

/**
 * 07 §2 J3：报告响应（分级：未交卷 code=1 拦截器弹；0/1/3 只回壳 reportStatus；
 * 2 全量。overallScore=null = 已评<3 样本闸 P10——前端必须显式呈现"仅定性"）
 */
export interface ReportVO {
  reportStatus: 0 | 1 | 2 | 3
  overallScore: number | null
  scoredCount: number | null
  totalCount: number | null
  summary: string | null
  weakPoints: WeakPointVO[] | null
  details: ReportDetailVO[] | null
}

/**
 * 09 C(c)：今日复习清单项。materialSource 是诚实标签必须呈现（A 裁决三级阶梯：
 * kb=题池已人工审核 / llm=AI 参考答案未经人工审核 / none=裸题干）；
 * status=processing 的项照常列出但提交会被 CAS 弹回明示文案（W7 复活路径靠它可见）
 */
export interface ReviewItemVO {
  itemId: number
  question: string
  material: string | null
  materialSource: 'kb' | 'llm' | 'none'
  boxNo: number
  lastScore: number | null
  nextReviewAt: string
  status: 'due' | 'processing'
}

/** 09：复习提交请求体（itemId=复习域自己的 id，不是场次题 id；answer 口径沿 07 同一把尺） */
export interface ReviewAnswerParams {
  itemId: number
  answer: string
}

/** 09 B：复习判分回执——转移可见化是灵魂："回盒一"还是"前进到盒2"必须让用户看见 */
export interface ReviewAnswerVO {
  score: number
  comment: string
  refAnswer: string
  /** 转移后的盒号（不是转移前！用户关心下一步） */
  boxNo: number
  /** promote / demote / freeze（t_review_log.result 同词汇表） */
  result: 'promote' | 'demote' | 'freeze'
  /** 下次到期——"3 天后再见"是复习闭环给用户的唯一承诺 */
  nextReviewAt: string
}
