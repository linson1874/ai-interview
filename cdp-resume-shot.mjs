/**
 * cdp-resume-shot.mjs — 简历名录页取证截图。
 * 不碰真实后端：用 CDP 的 Fetch 域拦截 /api/*，喂**合成**数据（姓名/时间/技能均为假）。
 * 用法: node cdp-resume-shot.mjs <origin> <outDir>
 */
import { spawn } from 'node:child_process'
import { setTimeout as sleep } from 'node:timers/promises'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

const origin = process.argv[2] || 'http://127.0.0.1:5173'
const outDir = process.argv[3] || '.impeccable/review'
const chrome = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const PORT = 9227
fs.mkdirSync(outDir, { recursive: true })
const profile = path.join(os.tmpdir(), 'cdp-resume-' + Date.now())

const ROWS = [
  { id: 4, realName: '林一舟', targetJob: 'Java 后端开发', skills: ['Spring Boot', 'MySQL', 'Redis'], createTime: '2026-11-24 21:40:12' },
  { id: 3, realName: '林一舟', targetJob: 'AI 应用开发', skills: ['Spring AI', 'RAG', 'pgvector'], createTime: '2026-11-22 10:05:33' },
  { id: 2, realName: '林一舟', targetJob: '全栈开发', skills: ['Vue 3', 'TypeScript'], createTime: '2026-11-18 19:22:07' },
  { id: 1, realName: '林一舟', targetJob: '实习 · 后端', skills: ['Java', 'Docker'], createTime: '2026-11-11 09:12:45' },
]
let mode = 'normal'

const child = spawn(
  chrome,
  ['--headless=new', `--remote-debugging-port=${PORT}`, '--no-first-run', '--disable-gpu', '--hide-scrollbars', `--user-data-dir=${profile}`, 'about:blank'],
  { stdio: 'ignore' },
)

let version
for (let i = 0; i < 80; i++) {
  try {
    version = await (await fetch(`http://127.0.0.1:${PORT}/json/version`)).json()
    break
  } catch {
    await sleep(250)
  }
}
if (!version) {
  console.error('chrome not ready')
  process.exit(1)
}

const tab = await (await fetch(`http://127.0.0.1:${PORT}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })).json()
const ws = new WebSocket(tab.webSocketDebuggerUrl)
await new Promise((res, rej) => {
  ws.addEventListener('open', res)
  ws.addEventListener('error', rej)
})

let seq = 0
const pending = new Map()
const send = (method, params = {}) =>
  new Promise((res) => {
    const id = ++seq
    pending.set(id, res)
    ws.send(JSON.stringify({ id, method, params }))
  })

const b64 = (obj) => Buffer.from(JSON.stringify(obj), 'utf8').toString('base64')

// 拦截 /api/*：名录列表按时长/空态返回，其余一律 code 0
async function onPaused(params) {
  const url = params.request.url
  const respond = (body) =>
    send('Fetch.fulfillRequest', {
      requestId: params.requestId,
      responseCode: 200,
      responseHeaders: [
        { name: 'content-type', value: 'application/json; charset=utf-8' },
        { name: 'access-control-allow-origin', value: '*' },
      ],
      body: b64(body),
    }).catch(() => {})

  if (url.includes('/resume/page')) {
    if (mode === 'slow') await sleep(2500)
    const u = new URL(url)
    const page = Number(u.searchParams.get('page') || 1)
    const size = Number(u.searchParams.get('size') || 10)
    const many = Array.from({ length: 12 }, (_, i) => ({ ...ROWS[i % ROWS.length], id: 100 + i }))
    const all = mode === 'empty' ? [] : mode === 'many' ? many : ROWS
    const start = (page - 1) * size
    await respond({ code: 0, msg: 'ok', data: { list: all.slice(start, start + size), total: all.length, page, size } })
    return
  }
  if (url.includes('/user/me')) {
    await respond({ code: 0, msg: 'ok', data: { id: 1, username: 'demo', nickname: '林一舟' } })
    return
  }
  await respond({ code: 0, msg: 'ok', data: null })
}

ws.addEventListener('message', (event) => {
  const msg = JSON.parse(event.data)
  if (msg.id && pending.has(msg.id)) {
    pending.get(msg.id)(msg)
    pending.delete(msg.id)
    return
  }
  if (msg.method === 'Fetch.requestPaused') onPaused(msg.params)
  if (msg.method === 'Runtime.exceptionThrown') console.log('PAGE ERROR:', JSON.stringify(msg.params.exceptionDetails).slice(0, 600))
  if (msg.method === 'Runtime.consoleAPICalled') console.log('CONSOLE:', (msg.params.args || []).map((a) => a.value || a.description || '').join(' ').slice(0, 300))
})

const evaluate = async (expression) => {
  const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (r.result?.exceptionDetails) throw new Error(JSON.stringify(r.result.exceptionDetails).slice(0, 300))
  return r.result?.result?.value
}
const setSize = (width, height) =>
  send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 700 })
const shot = async (name) => {
  const r = await send('Page.captureScreenshot', { format: 'png' })
  fs.writeFileSync(path.join(outDir, name), Buffer.from(r.result.data, 'base64'))
  console.log('saved', name)
}
const cleanUi = `(() => {
  document.querySelectorAll('vue-devtools, #vue-devtools, #__vue-devtools').forEach((el) => el.remove())
  for (const el of document.body.children) {
    const tag = el.tagName.toLowerCase()
    if (tag.includes('devtools') || String(el.id || '').toLowerCase().includes('devtools')) el.style.display = 'none'
  }
  return true
})()`

await send('Page.enable')
await send('Runtime.enable')
await send('Network.enable')
await send('Network.setCacheDisabled', { cacheDisabled: true })
await send('Fetch.enable', { patterns: [{ urlPattern: '*/api/*', requestStage: 'Request' }] })

async function enter(width, height) {
  await setSize(width, height)
  await send('Page.navigate', { url: origin + '/login' })
  await sleep(1500)
  await evaluate(`localStorage.setItem('ai_interview_token','demo'); localStorage.setItem('ai_interview_nickname','林一舟'); true`)
}

async function goto(url) {
  await send('Page.navigate', { url })
  await sleep(1600)
  await evaluate(cleanUi)
}

await enter(1440, 900)

mode = 'normal'
await goto(origin + '/resumes')
await shot('roster-1440.png')

mode = 'empty'
await goto(origin + '/resumes')
await shot('roster-empty.png')

mode = 'slow'
await send('Page.navigate', { url: origin + '/resumes' })
await sleep(800)
await evaluate(cleanUi)
await shot('roster-loading.png')
await sleep(2600)

mode = 'many'
await goto(origin + '/resumes')
await shot('roster-12rows.png')

await setSize(1366, 768)
await goto(origin + '/resumes')
const overflow = await evaluate(`({ scrollW: document.documentElement.scrollWidth, clientW: document.documentElement.clientWidth })`)
console.log('OVERFLOW@1366:', JSON.stringify(overflow))
await shot('roster-1366.png')

await setSize(1440, 900)
await goto(origin + '/resumes')
await evaluate(`[...document.querySelectorAll('[data-label="操作"] .roster-op')].pop()?.click(); true`)
await sleep(700)
await shot('roster-confirm.png')

await goto(origin + '/resumes')
const metrics = await evaluate(`(() => {
  const rect = (sel) => { const el = document.querySelector(sel); if (!el) return null; const r = el.getBoundingClientRect(); return { l: Math.round(r.left), r: Math.round(r.right), t: Math.round(r.top), b: Math.round(r.bottom) } }
  return {
    rail: rect('.shell__rail'),
    content: rect('.shell__content'),
    activeNav: (document.querySelector('.shell__item--on') || {}).innerText,
    badges: [...document.querySelectorAll('.shell__badge')].map((b) => b.innerText),
    rows: document.querySelectorAll('.roster-table__row').length,
    path: location.pathname,
    scrollW: document.documentElement.scrollWidth,
    clientW: document.documentElement.clientWidth,
  }
})()`)
console.log('METRICS:', JSON.stringify(metrics, null, 2))

ws.close()
child.kill()
await sleep(300)
try {
  fs.rmSync(profile, { recursive: true, force: true })
} catch {}
