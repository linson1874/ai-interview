/**
 * 通用轮询 hook（合同11 §3 —— #75 底线件二，学员手写）
 * 四道拍板在此落成代码：①固定间隔+退避 ②abort+ignore 双保险 ③熔断+重试按钮 ④可见性暂停
 * 三处复用：interview status / reportStatus / 复习到期，全都踩这个原语
 */
import { onBeforeUnmount, ref } from 'vue'
import axios from 'axios'
import http from '@/api/http'
import type { Result } from '@/types/api'

/** 轮询配置 */
interface PollOptions<T> {
  /**
   * 轮询地址（GET）。静态串或取址函数二选一——
   * 地址依赖先行动作结果时用函数形式（如 interviewId 要等 POST /interview 秒回后才有值）
   */
  url: string | (() => string)
  /** 每次拿到 code=0 数据的回调——已卸载/已过期时不会被调（双保险之二） */
  onData: (data: Result<T>) => void
  /** 连续失败达到上限 → 熔断，页面据此画"重试"按钮（§3③） */
  onGiveUp: () => void
}

const BASE_INTERVAL = 3000 // §3①：固定 3s 起
const MAX_RETRIES = 3 // §3③：连续失败 3 次熔断——退避用尽即停，一直转圈是骗人

export function usePolling<T>(options: PollOptions<T>) {
  /** 轮询中标志（页面区分等待态用） */
  const isPolling = ref(false)
  /** 熔断失败态：true = 轮询已停，页面展示"重试"按钮 */
  const isFailed = ref(false)

  let timer: ReturnType<typeof setTimeout> | null = null
  let abortController: AbortController | null = null
  let failCount = 0
  let stopped = false // 主动停止（卸载/stop）后一切回调作废
  let ignoreCallback = false // 双保险之二：abort 掐得住"在途"，掐不住"已到达但回调未执行"的竞态窗口

  function clearTimer(): void {
    if (timer !== null) {
      clearTimeout(timer)
      timer = null
    }
  }

  function scheduleNext(delay: number): void {
    if (stopped) return
    clearTimer()
    timer = setTimeout(() => {
      timer = null
      tick()
    }, delay)
  }

  function tick(): void {
    // 双保险之一：AbortController 真实取消在途请求（传输层取消）
    abortController = new AbortController()
    const url = typeof options.url === 'function' ? options.url() : options.url
    http
      .get<Result<T>>(url, {
        signal: abortController.signal,
        // §2③ 铁律①：轮询是后台心跳不是用户等待，skipGlobalProgress 不进全局计数
        skipGlobalProgress: true,
      })
      .then((res) => {
        if (stopped || ignoreCallback) return
        failCount = 0 // 成功即复位失败计数
        options.onData(res.data)
        scheduleNext(BASE_INTERVAL)
      })
      .catch((err) => {
        // 取消不算失败：卸载/停止/切走暂停掐掉的请求不计入退避账本
        if (stopped || ignoreCallback || axios.isCancel(err)) return
        failCount += 1
        if (failCount >= MAX_RETRIES) {
          // §3③：熔断——报错是唯一诚实选项，但"重试"按钮在页面内复位续跑，状态零损失
          isFailed.value = true
          isPolling.value = false
          options.onGiveUp()
          return
        }
        // §3①：连续失败退避 3s → 6s → 12s（2 ** failCount），防把故障中的后端压死
        scheduleNext(BASE_INTERVAL * 2 ** failCount)
      })
  }

  /** 启动轮询：立刻发第一发（避免干等 3s 才见第一帧数据） */
  function start(): void {
    if (isPolling.value || isFailed.value) return // 防重复启动
    stopped = false
    ignoreCallback = false
    failCount = 0
    isFailed.value = false
    isPolling.value = true
    document.addEventListener('visibilitychange', onVisibilityChange)
    tick()
  }

  /** 手动停止（卸载时会自动调） */
  function stop(): void {
    stopped = true
    isPolling.value = false
    clearTimer()
    abortController?.abort() // 双保险之一：传输层取消在途请求
    document.removeEventListener('visibilitychange', onVisibilityChange)
  }

  /** §3③：熔断后的"重试"——复位失败计数并立刻续跑，不弹"请刷新"（刷新丢作答状态=负收益） */
  function retry(): void {
    if (!isFailed.value) return
    isFailed.value = false
    failCount = 0
    isPolling.value = true
    tick()
  }

  /** §3④：切走暂停（清定时器+掐在途），切回立刻触发一次并复位失败计数（#81） */
  function onVisibilityChange(): void {
    if (stopped || isFailed.value) return
    if (document.hidden) {
      clearTimer()
      abortController?.abort()
    } else {
      failCount = 0 // 用户切走期间攒下的网络抖动失败，不该由"回来"这一下买单
      tick()
    }
  }

  // §3②：卸载必停——定时器、在途连接、监听器随卸载全部释放，不留悬空引用
  onBeforeUnmount(stop)

  return { isPolling, isFailed, start, stop, retry }
}
