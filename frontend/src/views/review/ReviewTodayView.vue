<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { answerReview, todayReview } from '@/api/review'
import type { ReviewAnswerVO, ReviewItemVO } from '@/types/api'

// —— 页面卡 D5-2：复习页（09 C(c)/B 转移可见化/A 诚实标签/F7 防连点）——
interface CardState {
  answering: boolean
  draft: string
  receipt: ReviewAnswerVO | null
}

const items = ref<ReviewItemVO[]>([])
const listLoading = ref(false)
// 每张卡片的局部状态（draft/回执），按 itemId 索引
const states = reactive<Record<number, CardState>>({})

function stateOf(itemId: number): CardState {
  if (!states[itemId]) {
    states[itemId] = { answering: false, draft: '', receipt: null }
  }
  return states[itemId]
}

// 09 A 裁决三级阶梯的诚实标签——必须呈现，"未经人工审核"的明示由这个字段承载
const SOURCE_LABELS: Record<string, string> = {
  kb: '题池材料（已人工审核）',
  llm: 'AI 材料（未经人工审核）',
  none: '无参考材料（裸题干）',
}

// 09 B 转移可见化：result 三态词 → 用户能看见的走向
const RESULT_LABELS: Record<string, string> = {
  promote: '前进',
  demote: '回退',
  freeze: '原地冻结',
}

function formatTime(iso: string | null): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}

async function load(): Promise<void> {
  listLoading.value = true
  try {
    items.value = await todayReview()
  } catch {
    // 失败文案由拦截器统一弹出
  } finally {
    listLoading.value = false
  }
}

async function onSubmit(item: ReviewItemVO): Promise<void> {
  const st = stateOf(item.itemId)
  const text = st.draft.trim()
  if (!text) return // 后端 @NotBlank 的前端前置拦
  if (st.answering) return // F7 防连点（后端 CAS 是主闸，这里只是体验辅佐）
  if (item.status === 'processing') return // processing 项禁提交（W7 复活路径可见性）
  st.answering = true
  try {
    st.receipt = await answerReview({ itemId: item.itemId, answer: text })
    ElMessage.success(`判分完成：${st.receipt.score} 分`)
  } catch {
    // CAS 弹回明示文案等——拦截器统一弹
  } finally {
    st.answering = false
  }
}

/** 重拉清单：刚复习完的项 nextReviewAt 已后移，今日不再到期——它从清单消失 */
async function onContinue(): Promise<void> {
  await load()
}

onMounted(load)
</script>

<template>
  <div v-loading="listLoading" class="review-page">
    <header class="review-page__bar">
      <h1 class="review-page__title">今日复习</h1>
      <el-button :disabled="listLoading" @click="load">刷新清单</el-button>
    </header>

    <!-- 空态：今日无到期（乐见其成的空态） -->
    <el-empty v-if="!listLoading && items.length === 0" description="今日没有到期的复习项，复习完的会按遗忘曲线回来" />

    <template v-else>
      <div v-for="item in items" :key="item.itemId" class="review-card">
        <!-- 卡头：来源诚实标签（A 裁决） + 盒号 + 上次分 -->
        <div class="review-card__head">
          <el-tag :type="item.materialSource === 'kb' ? 'success' : 'warning'">
            {{ SOURCE_LABELS[item.materialSource] ?? item.materialSource }}
          </el-tag>
          <el-tag type="info">盒 {{ item.boxNo }}</el-tag>
          <el-tag v-if="item.lastScore !== null" type="info">上次 {{ item.lastScore }} 分</el-tag>
          <el-tag v-if="item.status === 'processing'" type="danger">处理中（稍后再试）</el-tag>
          <span class="review-card__next">下次到期：{{ formatTime(item.nextReviewAt) }}</span>
        </div>

        <!-- F3：题干文本插值 -->
        <p class="review-card__q">{{ item.question }}</p>

        <!-- 复习材料（拍板：默认展开——复习是开卷练习，材料就是复习本身） -->
        <div v-if="item.material" class="review-card__material">
          <p class="review-card__material-label">复习材料</p>
          <p class="review-card__material-body">{{ item.material }}</p>
        </div>

        <!-- 回执态：09 B 转移可见化 -->
        <div v-if="stateOf(item.itemId).receipt" class="review-card__receipt">
          <p class="review-card__receipt-score">
            得分：{{ stateOf(item.itemId).receipt!.score }} / 5 ·
            {{ RESULT_LABELS[stateOf(item.itemId).receipt!.result] }}到盒
            {{ stateOf(item.itemId).receipt!.boxNo }}
          </p>
          <p class="review-card__receipt-comment">
            {{ stateOf(item.itemId).receipt!.comment }}
          </p>
          <p class="review-card__receipt-next">
            下次到期：{{ formatTime(stateOf(item.itemId).receipt!.nextReviewAt) }}——这是复习闭环给你的承诺
          </p>
          <el-button type="primary" @click="onContinue">继续复习</el-button>
        </div>

        <!-- 作答态 -->
        <template v-else>
          <el-input
            v-model="stateOf(item.itemId).draft"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            :disabled="item.status === 'processing'"
            placeholder="凭记忆作答，提交后判分"
          />
          <el-button
            type="primary"
            class="review-card__submit"
            :loading="stateOf(item.itemId).answering"
            :disabled="!stateOf(item.itemId).draft.trim() || item.status === 'processing'"
            @click="onSubmit(item)"
          >
            提交复习
          </el-button>
        </template>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 骨架期朴素排版：设计官接管时重排（完成环第 5 步） */
.review-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px 24px;
}
.review-page__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.review-page__title {
  margin: 0;
  font-size: 20px;
  color: var(--zoo-ink);
}
.review-card {
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--ai-radius-md);
  background: #ffffff;
}
.review-card__head {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  margin-bottom: 10px;
}
.review-card__next {
  margin-left: auto;
  font-size: 12px;
  color: var(--zoo-ink-soft);
}
.review-card__q {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--zoo-ink);
}
.review-card__material {
  padding: 10px 14px;
  margin-bottom: 12px;
  border-left: 3px solid var(--zoo-land);
  background: var(--zoo-water);
  border-radius: 0 var(--ai-radius-sm) var(--ai-radius-sm) 0;
}
.review-card__material-label {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--zoo-ink-soft);
}
.review-card__material-body {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--zoo-ink);
  white-space: pre-wrap;
}
.review-card__receipt {
  padding: 12px 14px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--ai-radius-sm);
  background: var(--zoo-water);
}
.review-card__receipt-score {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--zoo-ink);
}
.review-card__receipt-comment {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--zoo-ink);
}
.review-card__receipt-next {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--zoo-ink-soft);
}
.review-card__submit {
  margin-top: 10px;
}
</style>
