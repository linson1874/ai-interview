<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { getNickname, logout } from '@/api/auth'

// 内部区外壳（导航与版式；不含业务逻辑）。
// 分区口径按 docs/product/frontend-nav-audit.md 定死四区：简历 / 面试 / 报告 / 练习。
// 徽章双态：待回填（后端已就绪，前端页面未做）/ 未开发（前后端皆无）。
const route = useRoute()
const router = useRouter()

async function onLogout(): Promise<void> {
  await logout()
  await router.push('/login')
}

type NavItem = {
  index: string
  label: string
  zone: string
  to?: string
  badge?: '待回填' | '未开发'
}

const items: NavItem[] = [
  { index: '01', label: '我的简历', zone: '简历区', to: '/resumes' },
  { index: '02', label: '开始面试', zone: '面试区', to: '/interviews/new' },
  { index: '03', label: '面试报告', zone: '报告区', badge: '待回填' },
  { index: '04', label: '复习队列', zone: '练习区', badge: '待回填' },
  { index: '05', label: '知识问答', zone: '练习区', badge: '待回填' },
]

const nickname = getNickname()

function currentZone(path: string): string {
  if (path.startsWith('/interviews')) return '面试区'
  return '简历区'
}
</script>

<template>
  <div class="shell">
    <aside class="shell__rail">
      <div class="shell__brand">
        <span class="shell__brand-latin">AI</span>
        <span class="shell__brand-cn">面试系统</span>
      </div>

      <p class="shell__rail-label">导览目录</p>

      <nav class="shell__nav" aria-label="导览目录">
        <template v-for="item in items" :key="item.index">
          <RouterLink
            v-if="item.to"
            :to="item.to"
            class="shell__item"
            active-class="shell__item--on"
          >
            <span class="shell__index">{{ item.index }}</span>
            <span class="shell__label">{{ item.label }}</span>
          </RouterLink>
          <span v-else class="shell__item shell__item--off">
            <span class="shell__index">{{ item.index }}</span>
            <span class="shell__label">{{ item.label }}</span>
            <span class="shell__badge">{{ item.badge }}</span>
          </span>
        </template>
      </nav>

      <div class="shell__legend">
        <p class="shell__legend-label">分区</p>
        <ul class="shell__zones">
          <li><i class="shell__dot shell__dot--resume"></i>简历区</li>
          <li><i class="shell__dot shell__dot--interview"></i>面试区</li>
          <li><i class="shell__dot shell__dot--report"></i>报告区</li>
          <li><i class="shell__dot shell__dot--practice"></i>练习区</li>
        </ul>
      </div>

      <p class="shell__colophon">练习区 · 导览图</p>
    </aside>

    <div class="shell__main">
      <header class="shell__bar">
        <p class="shell__crumb">导览图 / {{ currentZone(route.path) }}</p>
        <div class="shell__user">
          <span v-if="nickname" class="shell__who">{{ nickname }}</span>
          <button class="shell__logout" type="button" @click="onLogout">退出登录</button>
        </div>
      </header>
      <main class="shell__content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: 100dvh;
  background: var(--zoo-paper);
  color: var(--zoo-ink);
  font-family: var(--ai-font-sans);
}

/* —— 左：整块陆地绿收成一条导航栏 —— */
.shell__rail {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 28px 18px 20px;
  background: var(--zoo-land);
  border-right: 1px solid var(--zoo-ink);
}

.shell__brand {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--zoo-ink);
}

.shell__brand-latin {
  display: block;
  font-family: 'Zoo Display', var(--ai-font-sans);
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.shell__brand-cn {
  display: block;
  margin-top: 2px;
  font-family: 'Zoo Display', var(--ai-font-sans);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.28em;
  line-height: 1.2;
}

.shell__rail-label {
  margin: 0;
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--zoo-ink);
}

.shell__nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.shell__item {
  display: grid;
  grid-template-columns: 24px 1fr auto;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 10px;
  border-radius: var(--zoo-cap);
  color: var(--zoo-ink);
  font-size: 13px;
  text-decoration: none;
}

.shell__item:hover {
  background: rgba(44, 46, 42, 0.12);
}

.shell__item--on,
.shell__item--on:hover {
  background: var(--zoo-ink);
  color: var(--zoo-water);
}

.shell__item--off {
  cursor: default;
}

.shell__item--off:hover {
  background: transparent;
}

.shell__index {
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.08em;
  opacity: 0.72;
}

.shell__label {
  white-space: nowrap;
}

/* 徽章：待回填 / 未开发，奶油小胶囊，别用低透明墨（绿地对比度不够） */
.shell__badge {
  padding: 2px 8px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: var(--zoo-water);
  color: var(--zoo-ink);
  font-size: 10px;
  letter-spacing: 0.06em;
  line-height: 1.4;
  white-space: nowrap;
}

.shell__legend {
  margin-top: auto;
  padding-top: 14px;
  border-top: 1px solid rgba(44, 46, 42, 0.35);
}

.shell__legend-label {
  margin: 0 0 8px;
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.18em;
  color: var(--zoo-ink);
}

.shell__zones {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 10px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 12px;
  color: var(--zoo-ink);
}

.shell__zones li {
  display: flex;
  align-items: center;
  gap: 7px;
}

.shell__dot {
  display: block;
  width: 8px;
  height: 8px;
  border: 1px solid var(--zoo-ink);
  border-radius: 50%;
}

.shell__dot--resume {
  background: var(--zoo-coral);
}

.shell__dot--interview {
  background: var(--zoo-sky);
}

.shell__dot--report {
  background: var(--zoo-lilac);
}

.shell__dot--practice {
  background: var(--zoo-land);
}

.shell__colophon {
  margin: 0;
  font-size: 10px;
  letter-spacing: 0.18em;
  line-height: 1;
  color: var(--zoo-ink);
}

/* —— 右：顶栏 + 内容 —— */
.shell__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.shell__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  height: 54px;
  padding: 0 36px;
  border-bottom: 1px solid var(--zoo-ink);
  background: var(--zoo-paper);
}

.shell__crumb {
  margin: 0;
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--zoo-ink-soft);
}

.shell__user {
  display: flex;
  align-items: center;
  gap: 14px;
}

.shell__logout {
  height: 30px;
  padding: 0 14px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: transparent;
  color: var(--zoo-ink);
  font-family: inherit;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}

.shell__logout:hover {
  background: rgba(44, 46, 42, 0.08);
}

.shell__logout:focus-visible {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}

.shell__who {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--zoo-ink-soft);
}

.shell__content {
  flex: 1;
  min-width: 0;
  padding: 30px 36px 40px;
}
</style>
