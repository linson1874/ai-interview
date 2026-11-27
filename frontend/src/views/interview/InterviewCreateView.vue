<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageResumes } from '@/api/resume'
import { createInterview, retryInterview } from '@/api/interview'
import { usePolling } from '@/composables/usePolling'
import type { InterviewDetailVO, ResumeListItemVO } from '@/types/api'

// —— 页面卡 D3-1：面试创建页（06 合同 §1/§2/H5/T1/T6；拍板五等待态无返回、拍板六 count 1-10）——
const router = useRouter()

// —— 表单态 ——
const resumes = ref<ResumeListItemVO[]>([])
const form = ref<{ resumeId: number | null; count: number }>({ resumeId: null, count: 5 })
const submitting = ref(false)
const canSubmit = computed(() => form.value.resumeId !== null && !submitting.value)

// 场次 id：POST /interview 秒回后才有值，轮询地址因此用取址函数形式
const interviewId = ref<number | null>(null)

// 页面相位：表单态 / 等待态（status 0/4）/ 业务失败态（status=3）
// 注意与 hook 的 isFailed（网络层熔断）是两回事：一个是业务失败、一个是连接不稳
const phase = ref<'form' | 'waiting' | 'failed'>('form')

// 轮询 hook（合同11 §3 四拍板的落地原语）
const poller = usePolling<InterviewDetailVO>({
  url: () => `/interview/${interviewId.value}`,
  onData: (result) => {
    const detail = result.data
    if (detail.status === 1) {
      // status →1：题目就绪（R1 题目窗口此刻打开），跳答题页
      poller.stop()
      phase.value = 'form' // 归位，防组件复用时残留等待态
      router.push(`/interviews/${detail.id}/answer`)
      return
    }
    if (detail.status === 3) {
      // 死信边：生成失败——停轮询，画业务失败态
      poller.stop()
      phase.value = 'failed'
    }
    // status 0/4：继续等，hook 自己安排下一发
  },
  onGiveUp: () => {
    // 网络熔断（hook 层）：phase 保持 waiting，模板按 pollFailed 画"重试"按钮
  },
})
const { isFailed: pollFailed, retry: pollRetry } = poller

onMounted(async () => {
  // 简历下拉数据源：一页拉 50（F5 上限镜像）；简历超 50 份的场景记 backlog
  const pageVO = await pageResumes(1, 50).catch(() => null)
  if (pageVO) resumes.value = pageVO.list
})

async function onSubmit(): Promise<void> {
  if (!canSubmit.value) return // F7 防连点
  submitting.value = true
  try {
    // 06 T1：秒回 interviewId（远小于 30s），生成在后台异步进行
    interviewId.value = await createInterview({
      resumeId: form.value.resumeId!,
      count: form.value.count,
    })
    phase.value = 'waiting'
    poller.start()
  } catch {
    // 失败文案由拦截器统一弹出（文案出口唯一）
  } finally {
    submitting.value = false
  }
}

/** status=3 业务失败 → "重试生成"（H5：场次身份连续，retry 不换 id、count 沿用原值） */
async function onRetryGeneration(): Promise<void> {
  try {
    await retryInterview(interviewId.value!)
    phase.value = 'waiting'
    poller.start() // T6：重投成功 status 归 0，重新排队
  } catch {
    // 连点第二次 code=1"生成进行中，无需重试"——拦截器统一弹，这里静默
  }
}
</script>

<template>
  <section class="create" aria-labelledby="create-title">
    <div class="create__main">
      <!-- 页头三相位共用：标题与墨线不随状态消失，aria 引用也不悬空 -->
      <header class="create__head">
        <h1 id="create-title" class="create__title">开始面试</h1>
        <p class="create__sub">选一份简历、定题量，系统按简历内容出题。</p>
      </header>

      <!-- 表单态 -->
      <template v-if="phase === 'form'">
        <el-form
          label-position="top"
          class="create-form"
          @submit.prevent="onSubmit"
        >
          <el-form-item label="选择简历">
            <el-select
              v-model="form.resumeId"
              placeholder="选择一份简历出题"
              class="create-form__select"
            >
              <el-option
                v-for="r in resumes"
                :key="r.id"
                :label="`${r.realName}（${r.targetJob}）`"
                :value="r.id"
              />
            </el-select>
            <div v-if="resumes.length === 0" class="create-form__hint">
              还没有简历，
              <el-link type="primary" @click="router.push('/resumes/new')">先去创建一份</el-link>
            </div>
          </el-form-item>
          <el-form-item label="题目数量">
            <!-- 拍板六：1-10 镜像合同 count（06 §1.1 场次属性，T9：retry 沿用原值） -->
            <el-input-number v-model="form.count" :min="1" :max="10" />
            <span class="create-form__note">1-10 题；生成失败重试沿用本数</span>
          </el-form-item>
          <button class="create__cta" type="submit" :disabled="!canSubmit">
            {{ submitting ? '创建中…' : '创建并生成题目' }}
          </button>
        </el-form>
      </template>

      <!-- 等待态（status 0/4）：轮询由 hook 驱动 -->
      <template v-else-if="phase === 'waiting'">
        <div class="phase">
          <p class="phase__title">题目生成中</p>
          <p class="phase__note">
            系统正按你的简历出题，生成完会自动跳进答题页——本页不用刷新，也别关。
          </p>
          <div class="phase__meter" aria-hidden="true">
            <span class="phase__meter-run"></span>
          </div>
          <!-- 网络层熔断：hook 的重试（与业务失败态并存、语义不同） -->
          <button v-if="pollFailed" type="button" class="phase__ghost" @click="pollRetry">
            连接不稳，重试
          </button>
        </div>
      </template>

      <!-- 业务失败态（status=3）：H5 失败出口 -->
      <template v-else>
        <div class="phase phase--failed">
          <p class="phase__title">本次题目生成失败</p>
          <p class="phase__note">
            系统侧失败，你的场次还在——重投同一场即可，不必重新创建，题数沿用原值。
          </p>
          <button type="button" class="create__cta" @click="onRetryGeneration">重试生成</button>
        </div>
      </template>
    </div>

    <aside class="create__side">
      <p class="create__here">
        <i class="create__here-dot" aria-hidden="true"></i>面试区 · 你在这里
      </p>

      <ol class="create__flow">
        <li class="create__step" :class="{ 'create__step--on': phase === 'form' }">
          <span class="create__step-no">01</span>
          <span class="create__step-body">选简历、定题量，创建一场面试。</span>
        </li>
        <li
          class="create__step"
          :class="{ 'create__step--on': phase === 'waiting' || phase === 'failed' }"
        >
          <span class="create__step-no">02</span>
          <span class="create__step-body">后台按简历生成题目，本页轮询等待。</span>
        </li>
        <li class="create__step">
          <span class="create__step-no">03</span>
          <span class="create__step-body">题目就绪自动跳答题页，逐题作答、即时评分。</span>
        </li>
        <li class="create__step">
          <span class="create__step-no">04</span>
          <span class="create__step-body">
            交卷出报告；答错的知识点进复习队列，下场面试开场热身。
            <span class="create__step-badge">待回填</span>
          </span>
        </li>
      </ol>

      <p class="create__colophon">面试区 · 出题 → 作答 → 报告</p>
    </aside>
  </section>
</template>

<style scoped>
.create {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 36px;
  align-items: start;
}

.create__main {
  min-width: 0;
  max-width: 560px;
}

.create__head {
  padding-bottom: 18px;
  border-bottom: 2px solid var(--zoo-ink);
}

.create__title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.02em;
  color: var(--zoo-ink);
}

.create__sub {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--zoo-ink-soft);
}

.create-form {
  margin-top: 22px;
}

.create-form__select {
  width: 100%;
}

.create-form__hint {
  margin-top: 8px;
  font-size: 13px;
  color: var(--zoo-ink-soft);
}

.create-form__note {
  margin-left: 12px;
  font-size: 12px;
  color: var(--zoo-ink-soft);
}

/* 全页唯一主操作黄 */
.create__cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 20px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: var(--zoo-sun);
  color: var(--zoo-ink);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
}

.create__cta:hover:not(:disabled) {
  background: var(--zoo-sun-deep);
}

.create__cta:active:not(:disabled) {
  background: var(--zoo-sun-press);
}

.create__cta:focus-visible {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}

.create__cta:disabled {
  border-color: rgba(44, 46, 42, 0.45);
  background: rgba(44, 46, 42, 0.06);
  color: var(--zoo-ink);
  cursor: not-allowed;
}

/* —— 等待 / 失败：奶油状态块 —— */
.phase {
  margin-top: 22px;
  padding: 26px 24px;
  border: 1px solid var(--zoo-ink);
  background: var(--zoo-water);
}

.phase--failed {
  border-color: var(--zoo-alert);
}

.phase__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--zoo-ink);
}

.phase--failed .phase__title {
  color: var(--zoo-alert);
}

.phase__note {
  margin: 10px 0 18px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--zoo-ink-soft);
}

/* 生成中：一格一格走的墨色游标（steps 机械感，配导览图墨线世界） */
.phase__meter {
  position: relative;
  max-width: 320px;
  height: 10px;
  overflow: hidden;
  border: 1px solid var(--zoo-ink);
}

.phase__meter-run {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 40%;
  background: var(--zoo-ink);
  animation: create-run 1.4s steps(6, end) infinite;
}

@keyframes create-run {
  from {
    transform: translateX(-110%);
  }

  to {
    transform: translateX(360%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .phase__meter-run {
    animation: none;
  }
}

.phase__ghost {
  margin-top: 16px;
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: transparent;
  color: var(--zoo-ink);
  font-family: inherit;
  font-size: 12px;
  line-height: 1;
  cursor: pointer;
}

.phase__ghost:hover {
  background: rgba(44, 46, 42, 0.08);
}

.phase__ghost:focus-visible {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}

/* —— 右侧水湾：面试区流程 —— */
.create__side {
  position: sticky;
  top: 24px;
  padding: 20px 18px 16px;
  border: 1px solid var(--zoo-ink);
  background: var(--zoo-water);
}

.create__here {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--zoo-ink);
}

.create__here-dot {
  display: block;
  width: 8px;
  height: 8px;
  border: 1px solid var(--zoo-ink);
  border-radius: 50%;
  background: var(--zoo-sky);
}

.create__flow {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.create__step {
  display: grid;
  grid-template-columns: 24px 1fr;
  gap: 8px;
  align-items: start;
  padding: 8px 10px;
  border-radius: var(--zoo-cap);
}

.create__step--on {
  background: var(--zoo-ink);
}

.create__step--on .create__step-no,
.create__step--on .create__step-body {
  color: var(--zoo-water);
}

.create__step--on .create__step-badge {
  border-color: var(--zoo-water);
}

.create__step-no {
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.08em;
  line-height: 2;
  color: var(--zoo-ink-soft);
}

.create__step-body {
  font-size: 12px;
  line-height: 1.7;
  color: var(--zoo-ink);
}

.create__step-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 8px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  font-size: 10px;
  letter-spacing: 0.06em;
  line-height: 1.5;
  white-space: nowrap;
  color: var(--zoo-ink);
}

.create__colophon {
  margin: 16px 0 0;
  padding-top: 12px;
  border-top: 1px solid var(--zoo-paper-line);
  font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: var(--zoo-ink-soft);
}

/* 兜底：窄屏单列（不在迭代范围） */
@media (max-width: 1099px) {
  .create {
    grid-template-columns: minmax(0, 1fr);
  }

  .create__side {
    position: static;
  }

  .create__main {
    max-width: none;
  }
}
</style>
