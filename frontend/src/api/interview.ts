/**
 * interview 域 API（#77 映射：06 场次 / 07 作答报告 —— InterviewController：/interview/**，全部受保护）
 */
import http from './http'
import type {
  AnswerParams,
  AnswerScoreVO,
  FollowupVO,
  InterviewCreatedVO,
  InterviewDetailVO,
  ReportVO,
  Result,
} from '@/types/api'

/** 06 §2 + T1：创建面试——秒回 interviewId，不等生成（异步出题是本合同的灵魂） */
export async function createInterview(params: {
  resumeId: number
  count: number
}): Promise<number> {
  const res = await http.post<Result<InterviewCreatedVO>>('/interview', params)
  return res.data.data.interviewId
}

/** 06 §2：查场次（用户轮询面）。status=1 才附题目列表（R1 题目窗口条款） */
export async function getInterview(id: number): Promise<InterviewDetailVO> {
  const res = await http.get<Result<InterviewDetailVO>>(`/interview/${id}`)
  return res.data.data
}

/** 06 §2 + H5/T6：失败重投（3→0 原子边）。连点第二次 code=1"生成进行中，无需重试"由拦截器统一弹 */
export async function retryInterview(id: number): Promise<void> {
  await http.post<Result<null>>(`/interview/${id}/retry`)
}

/** 07 §2 J1：作答并同步评分（~10s）。S13 降级=code=1"本题评分未成功，可重新提交重评"，
 *  S15 stale="已有更新版作答提交…"——两者均由拦截器统一弹，答案已落库可重交 */
export async function answerQuestion(
  id: number,
  qid: number,
  params: AnswerParams,
): Promise<AnswerScoreVO> {
  const res = await http.put<Result<AnswerScoreVO>>(`/interview/${id}/answer/${qid}`, params)
  return res.data.data
}

/** 10 §A：发起追问（~5s，一发生成）。X4 深度顶/X5 场次超限的熔断 code=1 文案由拦截器统一弹 */
export async function followup(id: number, qid: number): Promise<FollowupVO> {
  const res = await http.post<Result<FollowupVO>>(`/interview/${id}/followup/${qid}`)
  return res.data.data
}

/** 07 §2 J3：交卷（1→2 原子边，秒回；报告异步启动——D5 报告页的入口） */
export async function finishInterview(id: number): Promise<void> {
  await http.post<Result<null>>(`/interview/${id}/finish`)
}

/** 07 §2 J3：查报告（分级响应：未交卷 code=1"请先交卷"拦截器弹；0/1/3 只回壳
 *  reportStatus；2 全量——前端据 reportStatus 轮询或引导 rebuild） */
export async function getReport(id: number): Promise<ReportVO> {
  const res = await http.get<Result<ReportVO>>(`/interview/${id}/report`)
  return res.data.data
}

/** 07 §2 S4/S11：手动重建报告（防抖与 retry 同构；烧钱操作，前端加确认弹窗——F4） */
export async function rebuildReport(id: number): Promise<void> {
  await http.post<Result<null>>(`/interview/${id}/report/rebuild`)
}
