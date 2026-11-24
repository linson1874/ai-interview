/**
 * 请求层 axios 实例与拦截器（合同11 §2 —— #75 底线件一，学员手写）
 *
 * src/api 目录映射约定（#77）：文件按合同域切分，不按 URL 前缀切分。
 *   auth.ts      ← 01 用户 / 02 认证（UserController：/user/**）
 *   resume.ts    ← 03 简历（ResumeController：/resume/**）
 *   ai.ts        ← 04 对话 / 05 出题 / 08 知识库（AiController：/ai/**）
 *   interview.ts ← 06 场次 / 07 评分报告（InterviewController：/interview/**）
 *   review.ts    ← 09 复习（ReviewController：/review/**）
 *   followup.ts  ← 10 追问（FollowupController：/interview/**，与 06 同前缀不同合同）
 */
import axios from 'axios'
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { NETWORK_ERROR_TEXT, resolveErrorText } from '@/constants/error-map'
import type { Result } from '@/types/api'

// —— token 存取（§1② 拍板：localStorage，活过刷新）——
const TOKEN_KEY = 'ai_interview_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

let isHandling401 = false // §4 全局锁：并发 401 只放第一个进来

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  isHandling401 = false // 锁释放时机②：登录成功写回新 token 时复位（#81）
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 轮询流量元数据位：置 true 的请求不进全局 loading 计数（§2③ 铁律①，实现二选一取元数据方案） */
declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    skipGlobalProgress?: boolean
  }
}

/** 顶部进度条数据源：count > 0 时展示（铁律③），进度条组件 D2 再接 */
export const globalLoading = reactive({ count: 0 })

const http = axios.create({
  // 走 vite 代理（vite.config.ts server.proxy）：后端无 context-path（案#19），
  // '/api' 前缀只活在前端，转发前被 rewrite 剥掉
  baseURL: '/api',
  timeout: 15000,
})

// —— 请求拦截器：计数 + token 注入 ——
http.interceptors.request.use((config) => {
  if (!config.skipGlobalProgress) {
    globalLoading.count++
  }
  const token = getToken()
  // falsy 守卫（#82）：token 为 null/undefined/空串时不加头，绝不拼出 "Bearer null"
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

/** 401 统一处理：静默清态跳登录（F6），不弹 toast（§2② carve-out） */
function handle401(): void {
  if (isHandling401) return // 竞态锁：并发 401 只处理第一个，其余静默丢弃
  isHandling401 = true
  clearToken()
  router
    .push('/login') // /login 路由 D2 注册后生效
    .finally(() => {
      isHandling401 = false // 锁释放时机①：跳转登录执行后复位（#81）
    })
}

// —— 响应拦截器：业务码分发 + 递减收口 ——
http.interceptors.response.use(
  (response) => {
    // 递减收口路径①：响应成功
    if (!response.config.skipGlobalProgress) {
      globalLoading.count--
    }
    const result = response.data as Result<unknown>
    if (result.code === 0) {
      return response // 成功原样放行，调用方从 response.data 里取
    }
    if (result.code === 3) {
      handle401()
      return Promise.reject(result)
    }
    // 其余非 0 code：统一文案出口查表（F2），后端 msg 有业务内容时优先
    ElMessage.error(resolveErrorText(result.code, result.msg))
    return Promise.reject(result)
  },
  // error 形状随失败阶段变化（网络层根本没有 response），收窄成本高于收益——
  // 此处 any 是"为什么类型系统给不了"的家法注释位（§0）
  (error: any) => {
    // 递减收口路径②③：请求失败与被取消（CanceledError）都落本分支；config 可能缺省要防守
    if (error.config && !error.config.skipGlobalProgress) {
      globalLoading.count--
    }
    if (axios.isCancel(error)) {
      // 主动取消（轮询卸载 abort）不是故障，静默即可
      return Promise.reject(error)
    }
    if (error.response?.status === 401) {
      handle401()
      return Promise.reject(error)
    }
    // 网络层错误统一兜底（§2 家法）
    ElMessage.error(NETWORK_ERROR_TEXT)
    return Promise.reject(error)
  },
)

export default http
