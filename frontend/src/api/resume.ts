/**
 * resume 域 API（#77 映射：03 简历 —— ResumeController：/resume/**，全部受保护）
 */
import http from './http'
import type {
  PageVO,
  ResumeDetailVO,
  ResumeListItemVO,
  ResumeSaveParams,
  Result,
} from '@/types/api'

/** 03 §3：分页列表（排序 createTime 降序 + id 降序；size 上限 50 由后端 D2 兜底，前端选择器不给出 >50 的选项） */
export async function pageResumes(page: number, size: number): Promise<PageVO<ResumeListItemVO>> {
  const res = await http.get<Result<PageVO<ResumeListItemVO>>>('/resume/page', {
    params: { page, size },
  })
  return res.data.data
}

/** 03 §3 + D5：详情。不存在/非本人/已删 → HTTP 404 + code=404，文案由拦截器统一弹出 */
export async function getResume(id: number): Promise<ResumeDetailVO> {
  const res = await http.get<Result<ResumeDetailVO>>(`/resume/${id}`)
  return res.data.data
}

/** 03 §3：新建 */
export async function createResume(params: ResumeSaveParams): Promise<void> {
  await http.post<Result<null>>('/resume', params)
}

/** 03 §3 + D6：全量替换——每次提交完整形状，未携带的可空字段=清空 */
export async function updateResume(id: number, params: ResumeSaveParams): Promise<void> {
  await http.put<Result<null>>(`/resume/${id}`, params)
}

/** 03 §3 + D7：幂等删除——无论归属/存在与否一律 code=0，前端没有"删除失败"态 */
export async function deleteResume(id: number): Promise<void> {
  await http.delete<Result<null>>(`/resume/${id}`)
}
