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
