<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowLeft, ArrowRight, Check, CircleCheck, Refresh, Warning } from '@element-plus/icons-vue'
import './review-practice.css'
import { reviewTasks } from './review-data'
import type { ReviewOutcome } from './review-data'

const props = defineProps<{ taskId: string }>()
const emit = defineEmits<{ back: []; complete: [taskId: string, outcome: ReviewOutcome] }>()
const task = computed(() => reviewTasks.find((entry) => entry.id === props.taskId) ?? reviewTasks[0])
const roleTasks = computed(() => reviewTasks.filter((entry) => entry.role === task.value.role))
const taskIndex = computed(() => Math.max(1, roleTasks.value.findIndex((entry) => entry.id === task.value.id) + 1))
const answer = ref('')
const submitted = ref(false)
const recommendedOutcome = computed(() => task.value.suggestedOutcome)

function submitAnswer() {
  if (!answer.value.trim()) return
  submitted.value = true
}
</script>

<template>
  <div class="review-practice" aria-labelledby="practice-title">
    <header class="practice-topbar">
      <el-button text class="practice-back" @click="emit('back')"><el-icon><ArrowLeft /></el-icon>复习中心</el-button>
      <div class="practice-progress"><span>今日任务</span><strong>第 {{ taskIndex }} / {{ roleTasks.length }} 项</strong><span class="practice-progress-track"><i :style="{ width: `${taskIndex / roleTasks.length * 100}%` }"></i></span></div>
      <span class="practice-role">{{ task.role }} · {{ task.domain }}</span>
    </header>

    <div class="practice-layout">
      <main class="practice-main">
        <div class="practice-context"><span>{{ task.source }}</span><span>{{ task.due }}</span></div>
        <h1 id="practice-title">{{ task.title }}</h1>
        <p class="practice-instruction">先不看参考答案，用自己的话回忆。可以写下关键原理、适用条件和一个例子。</p>

        <section class="recall-prompt" aria-labelledby="recall-title">
          <span class="prompt-label">回忆题</span>
          <h2 id="recall-title">{{ task.prompt }}</h2>
        </section>

        <section class="answer-editor" aria-label="输入你的回答">
          <label for="review-answer">我的回答</label>
          <el-input id="review-answer" v-model="answer" type="textarea" :rows="7" maxlength="1200" show-word-limit :disabled="submitted" placeholder="写下你记得的内容，不确定的部分也可以先写出来。" />
          <div class="answer-editor-footer"><span>建议先独立回忆，再查看 AI 反馈。</span><el-button type="primary" :disabled="!answer.trim() || submitted" @click="submitAnswer">提交回答<el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button></div>
        </section>

        <Transition name="feedback-reveal">
          <section v-if="submitted" class="ai-review" aria-live="polite" aria-labelledby="feedback-title">
            <header class="ai-review-heading"><span class="ai-review-icon"><el-icon><CircleCheck /></el-icon></span><div><span>AI 复习反馈 · 示例</span><h2 id="feedback-title">{{ recommendedOutcome === 'mastered' ? '这项内容基本掌握' : '方向正确，还需要补全关键机制' }}</h2></div><strong class="feedback-score">{{ task.score }}</strong></header>
            <blockquote class="submitted-answer"><span>你的回答</span><p>{{ answer }}</p></blockquote>
            <div class="feedback-columns"><article><h3>回答评价</h3><p>{{ task.feedback }}</p></article><article><h3>关键知识点</h3><p>{{ task.explanation }}</p></article></div>
            <div class="mastery-decision"><div><strong>AI 建议：{{ recommendedOutcome === 'mastered' ? '已掌握' : '还需复习' }}</strong><span>你可以根据自己的感受调整这项状态。</span></div><div><el-button :type="recommendedOutcome === 'mastered' ? 'primary' : 'default'" @click="emit('complete', task.id, 'mastered')"><el-icon><Check /></el-icon>标记已掌握</el-button><el-button :type="recommendedOutcome === 'needs-review' ? 'primary' : 'default'" @click="emit('complete', task.id, 'needs-review')"><el-icon><Refresh /></el-icon>安排再复习</el-button></div></div>
          </section>
        </Transition>
      </main>

      <aside class="practice-aside">
        <section class="aside-section aside-source"><span class="aside-eyebrow">复习来源</span><h2>{{ task.source.split(' · ')[0] }}</h2><p>上次回答：{{ task.answer }}</p><span class="source-score"><el-icon aria-hidden="true"><Warning /></el-icon>本题得分 <strong>{{ task.score }}</strong></span></section>
        <section class="aside-section"><h3>本次复习重点</h3><ul><li>先说清楚核心机制</li><li>补上适用条件或边界</li><li>用一个场景验证理解</li></ul></section>
        <section class="aside-next"><strong>复习完成后</strong><p>AI 会根据你的回答给出掌握建议，你可以手动调整下次复习安排。</p></section>
      </aside>
    </div>
    <footer class="practice-footer"><span>示例复习数据 · 不会保存你的输入</span><el-button text @click="emit('back')">暂时离开</el-button></footer>
  </div>
</template>

