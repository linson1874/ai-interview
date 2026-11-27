<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReport, rebuildReport } from '@/api/interview'
import { usePolling } from '@/composables/usePolling'
import type { ReportVO } from '@/types/api'

// —— 页面卡 D5-1：报告页（07 J3/S3/S4/S11 + F4 rebuild 确认；usePolling 第二次服役）——
const route = useRoute()
const router = useRouter()
const interviewId = Number(route.params.id)

const report = ref<ReportVO | null>(null)
const pageLoading = ref(true)
// 页面相位：生成中（轮询壳 reportStatus 0/1）/ 失败（3，rebuild 引导）/ 就绪（2，全量渲染）
const phase = ref<'generating' | 'failed' | 'ready'>('generating')

const poller = usePolling<ReportVO>({
  url: () => `/interview/${interviewId}/report`,
  onData: (result) => {
    report.value = result.data
    if (result.data.reportStatus === 2) {
      // 壳→全量迁移：停轮询，渲染
      poller.stop()
      phase.value = 'ready'
    } else if (result.data.reportStatus === 3) {
      poller.stop()
      phase.value = 'failed'
    }
    // 0/1：继续等，hook 自己安排下一发
  },
  onGiveUp: () => {
    // 网络熔断：phase 保持 generating，模板按 pollFailed 画"重试"
  },
})
const { isFailed: pollFailed, retry: pollRetry } = poller

async function initialLoad(): Promise<void> {
  try {
    const vo = await getReport(interviewId)
    report.value = vo
    if (vo.reportStatus === 2) {
      phase.value = 'ready'
    } else if (vo.reportStatus === 3) {
      phase.value = 'failed'
    } else {
      phase.value = 'generating'
      poller.start()
    }
  } catch {
    // 未交卷 code=1"请先交卷生成报告"等——拦截器已弹，回答题页
    router.push(`/interviews/${interviewId}/answer`)
  } finally {
    pageLoading.value = false
  }
}

async function onRebuild(): Promise<void> {
  // F4：rebuild 是烧钱操作，确认弹窗（与交卷确认同款交互）
  const action = await ElMessageBox.confirm(
    '重新生成报告将再次调用大模型，确定吗？',
    '重新生成确认',
    {
      confirmButtonText: '重新生成',
      cancelButtonText: '取消',
      type: 'warning',
    },
  ).catch(() => null)
  if (action !== 'confirm') return
  try {
    await rebuildReport(interviewId)
    phase.value = 'generating'
    poller.start() // S11：rebuild 边翻回 0 重新排队
  } catch {
    // 防抖 code=1 等文案由拦截器统一弹
  }
}

/** S3 evidence 溯源：滚动定位到逐题明细对应行 */
function scrollToDetail(qid: number): void {
  document.getElementById(`detail-${qid}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

// P10 样本闸：已评<3 时 overallScore=null，前端必须显式呈现"仅定性"
const sampleInsufficient = computed(() => report.value?.overallScore == null)

function scoreTagType(score: number | null): 'success' | 'warning' | 'danger' | 'info' {
  if (score === null) return 'info'
  if (score >= 4) return 'success'
  if (score >= 3) return 'warning'
  return 'danger'
}

function formatTime(iso: string | null): string {
  return iso ? iso.replace('T', ' ').slice(0, 16) : '—'
}

onMounted(initialLoad)
</script>

<template>
  <div v-loading="pageLoading" class="report-page">
    <!-- 相位一：生成中（壳 reportStatus 0/1） -->
    <div v-if="phase === 'generating'" class="report-waiting">
      <p class="report-waiting__title">报告生成中，本地算分与大模型定性同时进行…</p>
      <el-button v-if="pollFailed" @click="pollRetry">重试</el-button>
    </div>

    <!-- 相位二：失败（reportStatus 3）→ F4 rebuild 引导 -->
    <div v-else-if="phase === 'failed'" class="report-waiting">
      <p class="report-waiting__title">报告生成失败</p>
      <el-button type="primary" @click="onRebuild">重新生成报告</el-button>
    </div>

    <!-- 相位三：就绪（reportStatus 2）全量渲染 -->
    <template v-else-if="report">
      <header class="report-page__bar">
        <h1 class="report-page__title">面试报告</h1>
        <el-button @click="onRebuild">重新生成</el-button>
      </header>

      <!-- 总分：P10 样本闸 -->
      <div class="report-hero">
        <template v-if="!sampleInsufficient">
          <p class="report-hero__score">{{ report.overallScore }}</p>
          <p class="report-hero__label">综合得分（满分 100）</p>
          <p
            v-if="
              report.scoredCount !== null &&
              report.totalCount !== null &&
              report.scoredCount < report.totalCount
            "
            class="report-hero__note"
          >
            已评 {{ report.scoredCount }}/共 {{ report.totalCount }}，未评分题已剔除分母（P11）
          </p>
        </template>
        <template v-else>
          <p class="report-hero__score">—</p>
          <p class="report-hero__label">
            已评 {{ report.scoredCount }}/共 {{ report.totalCount }}，样本不足，仅定性评价（P10）
          </p>
        </template>
      </div>

      <!-- 定性段 -->
      <section class="report-section">
        <h2 class="report-section__title">整体评价</h2>
        <p class="report-section__body">{{ report.summary }}</p>
      </section>

      <!-- 薄弱点：evidence 点击溯源（S3） -->
      <section class="report-section">
        <h2 class="report-section__title">薄弱点</h2>
        <template v-if="report.weakPoints?.length">
          <div v-for="(wp, i) in report.weakPoints" :key="i" class="weak-card">
            <div class="weak-card__head">
              <el-tag>{{ wp.category }}</el-tag>
            </div>
            <p class="weak-card__reason">{{ wp.reason }}</p>
            <p class="weak-card__evidence">
              证据：
              <el-button
                v-for="qid in wp.evidence"
                :key="qid"
                link
                type="primary"
                @click="scrollToDetail(qid)"
              >
                题 #{{ qid }}
              </el-button>
            </p>
          </div>
        </template>
        <p v-else class="report-section__body">本次没有明显薄弱点。</p>
      </section>

      <!-- 逐题明细 -->
      <section class="report-section">
        <h2 class="report-section__title">逐题明细</h2>
        <div
          v-for="d in report.details"
          :id="`detail-${d.id}`"
          :key="d.id"
          class="detail-card"
        >
          <div class="detail-card__head">
            <el-tag>{{ d.category }}</el-tag>
            <el-tag type="info">难度{{ d.difficulty }}</el-tag>
            <el-tag :type="scoreTagType(d.score)">
              {{ d.score === null ? '未评分' : `${d.score}分` }}
            </el-tag>
          </div>
          <p class="detail-card__q">{{ d.question }}</p>
          <p class="detail-card__label">你的作答</p>
          <!-- F3 渲染纪律：全文本插值 -->
          <p class="detail-card__a">{{ d.userAnswer ?? '（未作答）' }}</p>
          <p class="detail-card__label">点评</p>
          <p class="detail-card__c">{{ d.scoreComment ?? '—' }}</p>
        </div>
      </section>

      <!-- F9（拍板落点）：页脚注记 -->
      <footer class="report-foot">
        已评 {{ report.scoredCount }}/共 {{ report.totalCount }} · 总分由系统本地算术合成，定性段由大模型产出（golden rule：模型不算数）
      </footer>
    </template>
  </div>
</template>

<style scoped>
/* 骨架期朴素排版：设计官接管时重排（完成环第 5 步） */
.report-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 24px;
}
.report-waiting {
  padding: 64px 0;
  text-align: center;
}
.report-waiting__title {
  margin-bottom: 16px;
  font-size: 16px;
  color: var(--zoo-ink);
}
.report-page__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.report-page__title {
  margin: 0;
  font-size: 20px;
  color: var(--zoo-ink);
}
.report-hero {
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--ai-radius-md);
  background: var(--zoo-water);
  text-align: center;
}
.report-hero__score {
  margin: 0;
  font-size: 48px;
  font-weight: 700;
  color: var(--zoo-ink);
}
.report-hero__label {
  margin: 8px 0 0;
  color: var(--zoo-ink-soft);
  font-size: 13px;
}
.report-hero__note {
  margin: 4px 0 0;
  color: var(--zoo-ink-soft);
  font-size: 12px;
}
.report-section {
  margin-bottom: 24px;
}
.report-section__title {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--zoo-ink);
}
.report-section__body {
  margin: 0;
  line-height: 1.7;
  color: var(--zoo-ink);
  white-space: pre-wrap;
}
.weak-card {
  padding: 12px 16px;
  margin-bottom: 12px;
  border: 1px dashed var(--zoo-ink);
  border-radius: var(--ai-radius-md);
}
.weak-card__head {
  margin-bottom: 6px;
}
.weak-card__reason {
  margin: 0 0 6px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--zoo-ink);
}
.weak-card__evidence {
  margin: 0;
  font-size: 12px;
  color: var(--zoo-ink-soft);
}
.detail-card {
  padding: 16px;
  margin-bottom: 12px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--ai-radius-md);
  background: #ffffff;
}
.detail-card__head {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.detail-card__q {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.6;
  color: var(--zoo-ink);
}
.detail-card__label {
  margin: 8px 0 2px;
  font-size: 12px;
  color: var(--zoo-ink-soft);
}
.detail-card__a {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--zoo-ink);
  white-space: pre-wrap;
}
.detail-card__c {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--zoo-ink);
}
.report-foot {
  padding: 16px 0;
  font-size: 12px;
  color: var(--zoo-ink-soft);
  text-align: center;
}
</style>
