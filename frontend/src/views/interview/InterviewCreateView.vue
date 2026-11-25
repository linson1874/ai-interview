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
  <div class="interview-create">
    <!-- 表单态 -->
    <template v-if="phase === 'form'">
      <header class="page-bar">
        <h1 class="page-title">创建面试</h1>
      </header>
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
        <el-button
          type="primary"
          native-type="submit"
          :loading="submitting"
          :disabled="form.resumeId === null"
        >
          创建并生成题目
        </el-button>
      </el-form>
    </template>

    <!-- 等待态（status 0/4）：轮询由 hook 驱动 -->
    <template v-else-if="phase === 'waiting'">
      <div class="waiting">
        <p class="waiting__title">题目生成中，大模型需要一点时间…</p>
        <!-- 网络层熔断：hook 的重试（与业务失败态并存、语义不同） -->
        <el-button v-if="pollFailed" @click="pollRetry">重试</el-button>
      </div>
    </template>

    <!-- 业务失败态（status=3）：H5 失败出口 -->
    <template v-else>
      <div class="waiting">
        <p class="waiting__title">本次题目生成失败</p>
        <p class="waiting__note">系统侧失败，你的场次还在——点下面重投同一场，不必重新创建。</p>
        <el-button type="primary" @click="onRetryGeneration">重试生成</el-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 骨架期朴素排版：设计官排到面试页时接管（完成环第 5 步） */
.interview-create {
  max-width: 560px;
  margin: 0 auto;
  padding: 32px 24px;
}
.page-bar {
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  color: var(--zoo-ink);
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
.waiting {
  padding: 64px 0;
  text-align: center;
}
.waiting__title {
  font-size: 16px;
  color: var(--zoo-ink);
}
.waiting__note {
  margin: 8px 0 16px;
  font-size: 13px;
  color: var(--zoo-ink-soft);
}
</style>
