<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowDown, ArrowRight, ArrowUp, CaretBottom, ChatDotRound, Check, CircleCheck, Clock, Refresh, Warning } from '@element-plus/icons-vue'
import {
  interviewReportQuestions,
  reportImprovements,
  reportStrengths,
} from './interview-report'
import type { InterviewReportQuestion } from './interview-report'

const props = withDefaults(defineProps<{ initiallyGenerating?: boolean; backLabel?: string }>(), {
  initiallyGenerating: false,
  backLabel: '面试详情',
})
const emit = defineEmits<{ repeat: []; review: []; back: []; ready: [] }>()

const generating = ref(props.initiallyGenerating)
const selectedCategory = ref('全部')
const lowScoresOnly = ref(false)
const unmasteredOnly = ref(false)
const sortOrder = ref<'default' | 'ascending' | 'descending'>('default')
const expanded = ref<number[]>([])
let generateTimer: number | undefined
let scoreFrame: number | undefined
const animatedScore = ref(0)

const scoredCount = computed(() => interviewReportQuestions.filter((item) => item.score !== null).length)
const overallScore = computed(() => Math.round(
  interviewReportQuestions.reduce((sum, item) => sum + (item.score ?? 0), 0) / scoredCount.value / 5 * 100,
))
const categories = ['全部', '八股', '项目', '场景', 'HR']
const filteredQuestions = computed(() => {
  let rows = interviewReportQuestions.filter((item) => {
    if (selectedCategory.value !== '全部' && item.category !== selectedCategory.value) return false
    if (lowScoresOnly.value && (item.score === null || item.score > 2)) return false
    if (unmasteredOnly.value && item.mastered) return false
    return true
  })

  if (sortOrder.value === 'ascending') rows = [...rows].sort((a, b) => (a.score ?? -1) - (b.score ?? -1))
  if (sortOrder.value === 'descending') rows = [...rows].sort((a, b) => (b.score ?? -1) - (a.score ?? -1))
  return rows
})

function toggleExpanded(id: number) {
  expanded.value = expanded.value.includes(id)
    ? expanded.value.filter((item) => item !== id)
    : [...expanded.value, id]
}

function scoreAriaLabel(score: number | null) {
  return score === null ? '未评分' : `${score} 分，满分 5 分${score <= 2 ? '，需要加强' : ''}`
}

function animateScore() {
  window.cancelAnimationFrame(scoreFrame ?? 0)
  const startedAt = performance.now()
  const duration = 1100
  const tick = (now: number) => {
    const progress = Math.min((now - startedAt) / duration, 1)
    const eased = 1 - (1 - progress) ** 4
    animatedScore.value = Math.max(0, Math.min(100, Math.round(overallScore.value * eased)))
    if (progress < 1) scoreFrame = window.requestAnimationFrame(tick)
  }
  scoreFrame = window.requestAnimationFrame(tick)
}

function toggleSort() {
  sortOrder.value = sortOrder.value === 'default' ? 'ascending' : sortOrder.value === 'ascending' ? 'descending' : 'default'
}

onMounted(() => {
  if (generating.value) {
    generateTimer = window.setTimeout(() => {
      generating.value = false
      animateScore()
      emit('ready')
    }, 3600)
  } else animateScore()
})
onBeforeUnmount(() => {
  window.clearTimeout(generateTimer)
  window.cancelAnimationFrame(scoreFrame ?? 0)
})
</script>

<template>
  <div class="report-preview" aria-labelledby="report-title">
    <div class="report-toolbar">
      <el-button class="report-back" text @click="emit('back')">← {{ backLabel }}</el-button>
      <span class="report-sample"><span aria-hidden="true"></span>示例报告 · 演示数据</span>
    </div>

    <section class="report-intro">
      <div class="report-intro-copy">
        <h1 id="report-title">本场面试报告</h1>
        <p class="report-meta">Java 后端开发实习生 <i></i> 技术面 <i></i> 标准难度 <i></i> 2026年11月26日</p>
      </div>
      <div class="report-actions-top">
        <el-button :icon="Refresh" @click="emit('repeat')">再面一次</el-button>
        <el-button :icon="ArrowRight" @click="emit('review')">去复习中心</el-button>
      </div>
    </section>

    <section class="report-score-summary" aria-label="面试总评">
      <div class="score-review" :class="{ 'review-loading': generating }" aria-live="polite">
        <template v-if="generating">
          <span class="skeleton-label"></span>
          <span class="skeleton-line skeleton-line-wide"></span>
          <span class="skeleton-line"></span>
          <p>AI 面试官正在复盘你的表现…</p>
        </template>
        <template v-else>
          <span class="report-verdict-label">本场评估</span>
          <h2 class="report-core-line">项目经验较扎实，基础机制与经历证据仍需补强。</h2>
          <div class="report-summary-detail">
            <p>本场表现呈现出项目与场景题强于基础机制题的特点。介绍订单系统时，能够结合个人负责内容说明事务边界和幂等处理；面对接口超时，也能按链路、应用和下游依赖逐层排查，回答有步骤且具备可执行性。</p>
            <p>需要优先加强的是原理解释和结果证据。部分基础题能说出结论，但对机制、适用条件和取舍说明不足；经历题则偏重描述做法，个人贡献、前后变化及结果依据不够明确。遇到追问时能补充信息，但答案收束还不够稳定。</p>
          </div>
          <div class="report-summary-stats">
            <span><el-icon aria-hidden="true"><CircleCheck /></el-icon><b>2</b><small>项优势</small></span>
            <span><el-icon aria-hidden="true"><Warning /></el-icon><b>3</b><small>项改进</small></span>
            <span><el-icon aria-hidden="true"><ChatDotRound /></el-icon><b>6</b><small>道题目</small></span>
          </div>
        </template>
      </div>
      <div class="report-score-result" :class="{ 'result-loading': generating }" aria-live="polite">
        <template v-if="generating">
          <span class="skeleton-score-number"></span>
          <span class="skeleton-ring"></span>
        </template>
        <template v-else>
          <el-progress class="report-score-ring" type="circle" :percentage="animatedScore" :width="116" :stroke-width="8" :show-text="true" color="var(--accent)" :aria-label="`综合评分 ${animatedScore} 分`">
            <span class="report-score-in-ring"><strong>{{ animatedScore }}</strong><small>分</small></span>
          </el-progress>
          <span class="score-rating">岗位表现 <b>良好</b></span>
          <small>{{ scoredCount }} 道题计分</small>
        </template>
      </div>
    </section>

    <section class="insight-grid" aria-label="优势与改进方向">
      <article class="insight-column strengths-column">
        <header><span class="insight-mark"><el-icon aria-hidden="true"><Check /></el-icon></span><h2>表现优势</h2></header>
        <ul class="insight-list">
          <li v-for="item in reportStrengths" :key="item.title"><h3>{{ item.title }}</h3><p>{{ item.detail }}</p></li>
        </ul>
      </article>
      <article class="insight-column improvements-column">
        <header><span class="insight-mark"><el-icon aria-hidden="true"><Warning /></el-icon></span><h2>改进建议</h2></header>
        <ul class="insight-list">
          <li v-for="item in reportImprovements" :key="item.title"><h3>{{ item.title }}</h3><p>{{ item.detail }}</p></li>
        </ul>
      </article>
    </section>

    <section class="question-section" aria-labelledby="question-section-title">
      <header class="question-section-heading">
        <div>
          <h2 id="question-section-title">逐题复盘</h2>
          <p>每道原题与追问分别评价，评分可以回到具体回答。</p>
        </div>
        <span class="question-count">{{ filteredQuestions.length }} / {{ interviewReportQuestions.length }} 题</span>
      </header>

      <div class="question-filters" aria-label="逐题筛选">
        <div class="category-filters" role="group" aria-label="按分类筛选">
          <button v-for="category in categories" :key="category" type="button" :aria-pressed="selectedCategory === category" @click="selectedCategory = category">{{ category }}</button>
        </div>
        <div class="filter-tools">
          <button class="filter-toggle" type="button" :aria-pressed="lowScoresOnly" @click="lowScoresOnly = !lowScoresOnly">只看低分</button>
          <button class="filter-toggle" type="button" :aria-pressed="unmasteredOnly" @click="unmasteredOnly = !unmasteredOnly">只看未掌握</button>
          <el-button class="sort-button" text :icon="sortOrder === 'descending' ? ArrowDown : ArrowUp" @click="toggleSort">
            {{ sortOrder === 'default' ? '按分数排序' : sortOrder === 'ascending' ? '由低到高' : '由高到低' }}
          </el-button>
        </div>
      </div>

      <div v-if="filteredQuestions.length" class="question-list">
          <TransitionGroup name="question-list" tag="div" class="question-list-items">
          <article v-for="(question, index) in filteredQuestions" :key="question.id" class="question-item">
          <button class="question-summary" type="button" :aria-expanded="expanded.includes(question.id)" @click="toggleExpanded(question.id)">
            <span class="question-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="question-summary-main">
              <span class="question-labels"><span class="category-label">{{ question.category }}</span><span class="topic-label">{{ question.topic }}</span><span v-if="!question.mastered" class="mastery-label">{{ question.score === null ? '待复习' : '需加强' }}</span></span>
              <strong>{{ question.title }}</strong>
            </span>
            <span v-if="question.score === null" class="question-score-empty" :aria-label="scoreAriaLabel(question.score)">未评分</span>
            <span v-else class="question-score-points" :class="{ 'points-low': question.score <= 2, 'points-mid': question.score === 3, 'points-high': question.score >= 4 }" :aria-label="scoreAriaLabel(question.score)" :title="scoreAriaLabel(question.score)">
              <el-icon v-if="question.score <= 2" class="score-warning" aria-hidden="true"><Warning /></el-icon>
              <span v-for="point in 5" :key="point" :class="{ 'point-filled': point <= question.score }"></span>
            </span>
            <el-icon class="question-chevron" aria-hidden="true"><CaretBottom /></el-icon>
          </button>
          <div v-if="expanded.includes(question.id)" class="question-detail">
            <div class="answer-excerpt">
              <span class="answer-speaker">你的回答</span>
              <p>{{ question.answer }}</p>
              <div class="answer-assessment"><strong>{{ question.score === null ? '等待评价' : `${question.score} / 5` }}</strong><span>{{ question.comment }}</span></div>
            </div>
            <div class="dialogue-tree" aria-label="本题追问与逐轮评价">
              <div v-for="(turn, turnIndex) in question.dialogue" :key="turnIndex" class="dialogue-turn" :class="{ 'dialogue-turn-candidate': turn.role === '我' }">
                <span class="turn-rail" aria-hidden="true"></span>
                <div class="turn-copy">
                  <span class="turn-role">{{ turn.role }}<small v-if="turnIndex > 1">追问 {{ Math.ceil((turnIndex - 1) / 2) }}</small></span>
                  <p>{{ turn.text }}</p>
                  <div v-if="turn.score !== undefined" class="turn-feedback"><strong>{{ turn.score }} / 5</strong><span>{{ turn.comment }}</span></div>
                </div>
              </div>
            </div>
            <footer class="question-detail-footer"><span><Clock aria-hidden="true" /> 本题评价已纳入面试复盘</span><el-button text @click="toggleExpanded(question.id)">收起详情 <el-icon class="el-icon--right"><ArrowUp /></el-icon></el-button></footer>
          </div>
        </article>
          </TransitionGroup>
      </div>
      <div v-else class="question-empty">没有符合这些条件的题目。<button type="button" @click="selectedCategory = '全部'; lowScoresOnly = false; unmasteredOnly = false">清除筛选</button></div>
    </section>

    <footer class="report-bottom-cta">
      <div><h2>把本场反馈带进下一次练习</h2><p>建议从基础机制理解和经历表达两方面继续加强。</p></div>
      <div><el-button class="cta-repeat" @click="emit('repeat')">再面一次 <el-icon class="el-icon--right"><Refresh /></el-icon></el-button><el-button type="primary" @click="emit('review')">去复习中心 <el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button></div>
    </footer>
    <p class="report-disclaimer">本页内容为交互演示数据，不代表真实面试评分。</p>
  </div>
</template>

<style src="./interview-report.css"></style>
