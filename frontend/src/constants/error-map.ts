/**
 * F2 错误码 → 前端文案映射表（全项目文案唯一出口，合同11 §1③ 拍板：绝不散落进页面 if-else）
 *
 * 影子来源：docs/api/01-user-api.md 错误码表 v0.4（0/1/2/3/400/404/405/500）。
 * 同步纪律（#83）：01 合同升版 checklist 有一条"同步本文件"——码表改版这里必须跟，
 * 影子不跟 = 静默漂移。
 * 注意：0 是成功码不是错误码，不进本表。
 */
export const ERROR_MESSAGES: Record<number, string> = {
  1: '操作失败，请稍后重试',
  2: '输入内容不符合要求，请检查后重试',
  // 3 有 carve-out（§2②）：不弹 toast，静默清态跳登录，本条仅留档备查
  3: '登录已失效，请重新登录',
  400: '请求格式错误（开发期排查）',
  // F6：404 资源类统一话术，03 合同 §1 的用户侧翻译
  404: '该资源不存在或已删除',
  405: '请求方式错误（开发期排查）',
  500: '系统繁忙，请稍后重试',
}

/** 网络层错误统一兜底文案（§2 家法：服务繁忙类） */
export const NETWORK_ERROR_TEXT = '网络异常，请稍后重试'

/**
 * 文案仲裁：后端 msg 有业务内容（如"用户名已存在"）时优先；
 * msg 为空查本表；表里也没有（未知新码）落网络兜底。
 * 三个出口全收口在这一个函数，谁都不许自己拼文案。
 */
export function resolveErrorText(code: number, backendMsg?: string | null): string {
  if (backendMsg) return backendMsg
  return ERROR_MESSAGES[code] ?? NETWORK_ERROR_TEXT
}
