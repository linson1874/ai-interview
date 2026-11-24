/**
 * auth 域 API（#77 映射：01 用户 / 02 认证 —— UserController：/user/**）
 */
import http, { setToken } from './http'
import type { LoginParams, LoginVO, RegisterParams, Result } from '@/types/api'

/** 昵称存根（02 合同 LoginVO 携带，供后续顶栏展示；token 本体归 http.ts 管） */
const NICKNAME_KEY = 'ai_interview_nickname'

export function setNickname(nickname: string): void {
  localStorage.setItem(NICKNAME_KEY, nickname)
}

export function getNickname(): string | null {
  return localStorage.getItem(NICKNAME_KEY)
}

/** 01 合同 §2：注册。失败 code=1/2 文案由拦截器统一弹出，本层只做请求与形状转换 */
export async function register(params: RegisterParams): Promise<void> {
  await http.post<Result<null>>('/user/register', params)
}

/** 02 合同 §2：登录。成功即落 token（顺带复位 401 锁——http.setToken 的释放时机②） */
export async function login(params: LoginParams): Promise<LoginVO> {
  const res = await http.post<Result<LoginVO>>('/user/login', params)
  setToken(res.data.data.token)
  setNickname(res.data.data.nickname)
  return res.data.data
}
