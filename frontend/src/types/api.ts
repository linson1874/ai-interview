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
