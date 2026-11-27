<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowRight, Check, Clock, Search, TrendCharts } from '@element-plus/icons-vue'
import './review-center.css'
import { mistakeQuestions, reviewTasks, weakDomains } from './review-data'
import type { ReviewOutcome } from './review-data'

const props = defineProps<{ completed: Record<string, ReviewOutcome> }>()
const emit = defineEmits<{ practice: [taskId: string]; interview: [] }>()

const selectedRole = ref('Java 后端')
const queueFilter = ref<'全部' | '待开始' | '已完成'>('全部')
const mistakeFilter = ref('全部')
const search = ref('')
const roles = ['Java 后端', 'Agent 工程', '前端工程']
const mistakeCategories = ['全部', '八股', '项目', '场景']
const tasks = computed(() => reviewTasks.filter((task) => task.role === selectedRole.value))
const roleDomains = computed(() => weakDomains.filter((domain) => domain.role === selectedRole.value))
const completedCount = computed(() => tasks.value.filter((task) => props.completed[task.id]).length)
const visibleTasks = computed(() => tasks.value.filter((task) => {
  if (queueFilter.value === '待开始' && props.completed[task.id]) return false
  if (queueFilter.value === '已完成' && !props.completed[task.id]) return false
  return true
}))
const visibleMistakes = computed(() => mistakeQuestions.filter((item) => {
  if (mistakeFilter.value !== '全部' && item.category !== mistakeFilter.value) return false
  const task = reviewTasks.find((entry) => entry.id === item.taskId)
  if (task?.role !== selectedRole.value) return false
  return `${item.title} ${item.domain}`.toLowerCase().includes(search.value.trim().toLowerCase())
}))

function statusLabel(id: string) {
  return props.completed[id] === 'mastered' ? '已掌握' : props.completed[id] === 'needs-review' ? '已复习 · 待巩固' : ''
}

function startToday() {
  const task = tasks.value.find((entry) => !props.completed[entry.id])
  if (task) emit('practice', task.id)
}

function scrollToMistakes() {
  document.getElementById('mistakes-title')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <div class="review-center" aria-labelledby="review-center-title">
    <header class="review-welcome">
      <div>
        <p class="review-date">周四 · 11月26日</p>
        <h2 id="review-center-title">把薄弱项练成稳定发挥</h2>
        <p>从最近几场面试里挑出值得再练的内容，今天先完成 {{ Math.max(tasks.length - completedCount, 0) }} 项。</p>
      </div>
      <div class="review-role-select">
        <label for="review-role">复习方向</label>
        <el-select id="review-role" v-model="selectedRole" aria-label="选择复习岗位方向">
          <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
        </el-select>
      </div>
    </header>

    <section class="review-today" aria-labelledby="today-title">
      <div class="today-progress">
        <div class="today-progress-heading">
          <div><h3 id="today-title">今日复习</h3></div>
          <span class="today-count"><strong>{{ completedCount }}</strong><span> / {{ tasks.length }} 项</span></span>
        </div>
        <el-progress :percentage="Math.round(completedCount / Math.max(tasks.length, 1) * 100)" :show-text="false" :stroke-width="7" color="var(--accent)" aria-label="今日复习进度" />
        <div class="today-progress-foot"><span>预计约 {{ Math.max(tasks.length - completedCount, 0) * 5 }} 分钟</span><span>连续复习 3 天</span></div>
      </div>
      <div class="today-action">
        <p>优先从逾期且得分较低的知识点开始。</p>
        <el-button type="primary" :disabled="completedCount === tasks.length" @click="startToday">
          {{ completedCount === tasks.length ? '今日已完成' : '开始今日复习' }}<el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </section>

    <section class="review-work-grid" aria-label="今日任务与掌握情况">
      <div class="review-queue-panel">
        <header class="review-section-head">
          <div><h3>今日任务</h3><p>按薄弱程度和到期时间排列</p></div>
          <div class="review-segment" role="group" aria-label="筛选今日任务">
            <button v-for="filter in ['全部', '待开始', '已完成'] as const" :key="filter" type="button" :aria-pressed="queueFilter === filter" @click="queueFilter = filter">{{ filter }}</button>
          </div>
        </header>
        <div v-if="visibleTasks.length" class="review-task-list">
          <article v-for="(task, index) in visibleTasks" :key="task.id" class="review-task-row">
            <span class="task-order">{{ String(index + 1).padStart(2, '0') }}</span>
            <div class="task-main">
              <div class="task-labels"><span class="domain-label">{{ task.domain }}</span><span :class="['task-priority', task.priority === '优先复习' ? 'priority-high' : '']">{{ statusLabel(task.id) || task.priority }}</span></div>
              <h4>{{ task.title }}</h4>
              <p>{{ task.source }}</p>
            </div>
            <span class="task-due" :class="{ overdue: task.due.includes('逾期') }"><el-icon aria-hidden="true"><Clock /></el-icon>{{ statusLabel(task.id) || task.due }}</span>
            <el-button class="task-start" text :aria-label="`${statusLabel(task.id) ? '再次复习' : '开始复习'}：${task.title}`" @click="emit('practice', task.id)">
              {{ statusLabel(task.id) ? '再练一次' : '开始复习' }}<el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </article>
        </div>
        <div v-else class="review-empty"><el-icon aria-hidden="true"><Check /></el-icon><strong>今日复习已完成</strong><span>可以到薄弱知识点继续巩固。</span></div>
      </div>

      <aside class="mastery-panel" aria-labelledby="mastery-title">
        <header class="review-section-head"><div><h3 id="mastery-title">薄弱知识点</h3><p>{{ selectedRole }} · 按当前掌握度排列</p></div><el-icon aria-hidden="true"><TrendCharts /></el-icon></header>
        <div class="mastery-list">
          <button v-for="domain in roleDomains" :key="domain.id" class="mastery-row" type="button" @click="emit('practice', domain.taskId)">
            <span class="mastery-copy"><strong>{{ domain.name }}</strong><small>{{ domain.count }} 个待巩固点 · {{ domain.detail }}</small></span>
            <span class="mastery-meter"><span :class="`tone-${domain.tone}`" :style="{ width: `${domain.mastery}%` }"></span></span>
            <b>{{ domain.mastery }}%</b>
          </button>
        </div>
        <button class="mastery-link" type="button" @click="scrollToMistakes">查看错题本 <el-icon aria-hidden="true"><ArrowRight /></el-icon></button>
      </aside>
    </section>

    <section class="mistakes-section" aria-labelledby="mistakes-title">
      <header class="review-section-head mistakes-heading">
        <div><h3 id="mistakes-title">面试错题本</h3><p>仅收录低分或 AI 判断尚未掌握的问题，保留当时的回答与点评。</p></div>
        <label class="mistake-search"><el-icon aria-hidden="true"><Search /></el-icon><input v-model="search" aria-label="搜索错题" placeholder="搜索题目或知识点"></label>
      </header>
      <div class="mistake-tools" role="group" aria-label="按问题类型筛选">
        <button v-for="category in mistakeCategories" :key="category" type="button" :aria-pressed="mistakeFilter === category" @click="mistakeFilter = category">{{ category }}</button>
        <span>{{ visibleMistakes.length }} 道题</span>
      </div>
      <div v-if="visibleMistakes.length" class="mistake-list">
        <article v-for="item in visibleMistakes" :key="item.id" class="mistake-row">
          <div class="mistake-main"><div class="mistake-labels"><span>{{ item.category }}</span><span>{{ item.domain }}</span><time>{{ item.date }}</time></div><h4>{{ item.title }}</h4><p>{{ item.answer }}</p></div>
          <span class="mistake-score" :class="{ 'score-low': item.score <= 2 }"><strong>{{ item.score }}</strong><small>/ 5 分</small></span>
          <el-button text @click="emit('practice', item.taskId)">重新作答<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button>
        </article>
      </div>
      <div v-else class="mistake-empty">没有找到符合条件的错题。</div>
    </section>

    <footer class="review-center-footer"><span>复习题目和 AI 点评为界面示例数据。</span><el-button text @click="emit('interview')">去模拟面试<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button></footer>
  </div>
</template>

