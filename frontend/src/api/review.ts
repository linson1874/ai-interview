/**
 * review 域 API（#77 映射：09 复习闭环 —— ReviewController：/review/**，全部受保护）
 */
import http from './http'
import type { ReviewAnswerParams, ReviewAnswerVO, ReviewItemVO, Result } from '@/types/api'

/** 09 C(c)：今日到期清单（读账本不读投影；空数组=今日无事——乐见其成的空态） */
export async function todayReview(): Promise<ReviewItemVO[]> {
  const res = await http.get<Result<ReviewItemVO[]>>('/review/today')
  return res.data.data
}

/** 09：提交复习作答（~10s，判分复用 07 链）。F2① CAS 防双击、F2② 快照防双跳——
 *  processing 项提交会被后端 CAS 弹回明示文案（拦截器统一弹），前端 disable 只是体验辅佐 */
export async function answerReview(params: ReviewAnswerParams): Promise<ReviewAnswerVO> {
  const res = await http.post<Result<ReviewAnswerVO>>('/review/answer', params)
  return res.data.data
}
