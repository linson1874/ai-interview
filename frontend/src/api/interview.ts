/**
 * interview 域 API（#77 映射：06 场次 / 07 作答报告 —— InterviewController：/interview/**，全部受保护）
 */
import http from './http'
import type { InterviewCreatedVO, InterviewDetailVO, Result } from '@/types/api'

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
