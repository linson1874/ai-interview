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
