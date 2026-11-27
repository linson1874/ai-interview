<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight, ChatDotRound, Check, Microphone, User, VideoPause } from '@element-plus/icons-vue'
import { exampleHistory, frontendTopics, generalTopics } from './answer-examples'
import type { AnswerSession, Exchange, TopicHistory } from './answer-examples'

const props = withDefaults(defineProps<AnswerSession>(), { usesResume: undefined })
const emit = defineEmits<{ leave: []; detail: [] }>()

const topics = props.seeded || props.role.includes('前端') ? frontendTopics : generalTopics(props.role)
const topicIndex = ref(props.seeded ? 2 : 0)
const topic = computed(() => topics[topicIndex.value]!)
const recording = ref(false)
const seconds = ref(0)
const draft = ref('')
const phase = ref<'answer' | 'review' | 'thinking' | 'finished'>('answer')
const exitOpen = ref(false)
const viewedTopic = ref<number | null>(null)
const dialogueViewport = ref<HTMLElement>()
const dialogueHeading = ref<HTMLElement>()
const reviewField = ref<HTMLTextAreaElement>()
const roomTitle = ref<HTMLElement>()
const history = ref<TopicHistory[]>(props.seeded ? [...exampleHistory] : [])
const exchanges = ref<Exchange[]>(props.seeded ? [
  { speaker: 'interviewer', text: topic.value.question },
  { speaker: 'candidate', text: '我会先看最大内容绘制，确认首屏元素的资源什么时候发起请求。然后对照性能面板，区分是网络加载慢还是主线程执行慢。', keyword: topic.value.keyword },
  { speaker: 'interviewer', text: topic.value.followup, keyword: topic.value.keyword },
] : [{ speaker: 'interviewer', text: props.initialQuestion || topic.value.question }])
const wave = [6, 10, 15, 9, 17, 12, 7]
let recordingTimer: number | undefined
let thinkingTimer: number | undefined

const isVoice = computed(() => props.format === '语音')
const previousTopic = computed(() => viewedTopic.value === null ? undefined : history.value[viewedTopic.value])
const topicTurn = computed(() => exchanges.value.filter((item) => item.speaker === 'interviewer').length)
const sessionStatus = computed(() => {
  if (phase.value === 'finished') return '已交卷'
  if (phase.value === 'thinking') return '面试官正在思考'
  if (phase.value === 'review') return '等待确认转写'
  if (recording.value) return '正在录音'
  return '轮到你作答'
})
const submittedCount = computed(() => {
  const groups = history.value.map((item) => item.exchanges)
  if (!groups.includes(exchanges.value)) groups.push(exchanges.value)
  return groups.flat().filter((entry) => entry.speaker === 'candidate').length
})
const timerLabel = computed(() => `${String(Math.floor(seconds.value / 60)).padStart(2, '0')}:${String(seconds.value % 60).padStart(2, '0')}`)
const displayedExchanges = computed(() => (previousTopic.value?.exchanges ?? exchanges.value).map((entry) => {
  const keyword = entry.keyword
  if (!keyword || !entry.text.includes(keyword)) return { ...entry, before: entry.text, highlight: '', after: '' }

  const highlighted = entry.speaker === 'candidate'
    ? entry.text.match(/[^。！？.!?]+[。！？.!?]?/g)?.find((sentence) => sentence.includes(keyword)) ?? keyword
    : keyword
  const start = entry.text.indexOf(highlighted)
  return {
    ...entry,
    before: entry.text.slice(0, start),
    highlight: highlighted,
    after: entry.text.slice(start + highlighted.length),
  }
}))

function stopRecording() {
  window.clearInterval(recordingTimer)
  recordingTimer = undefined
  recording.value = false
}

function clearDraft() {
  stopRecording()
  draft.value = ''
  seconds.value = 0
}

function restartAnswer() {
  clearDraft()
  phase.value = 'answer'
  startRecording()
}

function startRecording() {
  recording.value = true
  recordingTimer = window.setInterval(() => {
    seconds.value += 1
  }, 1000)
}

async function reviewTranscript() {
  stopRecording()
  draft.value = topic.value.transcript
  phase.value = 'review'
  await nextTick()
  reviewField.value?.focus()
}

function finish() {
  window.clearTimeout(thinkingTimer)
  clearDraft()
  exitOpen.value = false
  phase.value = 'finished'
}

function nextTopic() {
  history.value.push({ title: topic.value.title, exchanges: exchanges.value })
  if (topicIndex.value === topics.length - 1) {
    finish()
    return
  }
  topicIndex.value += 1
  exchanges.value = [{ speaker: 'interviewer', text: topic.value.question }]
  clearDraft()
  phase.value = 'answer'
}

function submitAnswer() {
  const text = draft.value.trim()
  if (!text || phase.value === 'thinking' || (isVoice.value && phase.value !== 'review')) return
  const keyword = topic.value.keyword
  const quoted = keyword && text.includes(keyword) ? keyword : undefined
  exchanges.value.push({ speaker: 'candidate', text, keyword: quoted })
  clearDraft()
  phase.value = 'thinking'

  thinkingTimer = window.setTimeout(() => {
    if (topicTurn.value > 1) {
      nextTopic()
      return
    }
    exchanges.value.push({
      speaker: 'interviewer',
      text: keyword && !quoted ? '针对你的回答，你会如何验证刚才提出的判断？' : topic.value.followup,
      keyword: quoted,
    })
    phase.value = 'answer'
  }, 850)
}

function sendOnEnter(event: KeyboardEvent) {
  if (event.isComposing || event.keyCode === 229) return
  event.preventDefault()
  submitAnswer()
}

async function viewTopic(index: number | null) {
  viewedTopic.value = index
  await nextTick()
  dialogueHeading.value?.focus({ preventScroll: true })
  const viewport = dialogueViewport.value
  if (viewport) viewport.scrollTop = index === null ? viewport.scrollHeight : 0
}

watch([() => exchanges.value.length, topicIndex, phase], async () => {
  await nextTick()
  const viewport = dialogueViewport.value
  if (viewport && !previousTopic.value) viewport.scrollTop = viewport.scrollHeight
})

onBeforeUnmount(() => {
  stopRecording()
  window.clearTimeout(thinkingTimer)
})

onMounted(() => roomTitle.value?.focus({ preventScroll: true }))
</script>

<template>
  <Teleport to="body">
    <div class="answer-room" aria-labelledby="answer-room-title">
      <header class="answer-topbar">
        <div class="answer-topbar-inner">
          <div class="answer-session">
            <h1 id="answer-room-title" ref="roomTitle" tabindex="-1">{{ role }}</h1>
            <p>{{ round }} · {{ format }}面试<span v-if="difficulty"> · {{ difficulty }}</span></p>
          </div>
          <el-button v-if="phase === 'finished'" class="answer-exit" plain :icon="ArrowLeft" @click="emit('leave')">返回配置</el-button>
          <el-button v-else class="answer-exit" plain :icon="ArrowLeft" @click="exitOpen = true">退出面试</el-button>
        </div>
      </header>

      <main v-if="phase === 'finished'" class="answer-summary">
        <h2>本场面试已交卷</h2>
        <p>已提交 {{ submittedCount }} 个回答，面试记录和报告正在整理。</p>
        <el-button type="primary" @click="emit('detail')">查看本场详情 <el-icon class="el-icon--right"><ArrowRight /></el-icon></el-button>
        <el-button text @click="emit('leave')">返回配置</el-button>
      </main>

      <main v-else class="answer-workspace">
        <section class="answer-dialogue" aria-labelledby="dialogue-heading">
          <header class="dialogue-header">
            <div>
              <p class="dialogue-context">第 {{ (viewedTopic ?? topicIndex) + 1 }} 题<span v-if="previousTopic"> · 已完成</span><span v-else> · 第 {{ topicTurn }} 轮对话</span></p>
              <h2 id="dialogue-heading" ref="dialogueHeading" tabindex="-1">{{ previousTopic?.title ?? topic.title }}</h2>
            </div>
            <el-button v-if="previousTopic" text :icon="ArrowRight" @click="viewTopic(null)">回到当前题</el-button>
            <p v-else class="dialogue-status" role="status" :class="{ 'status-waiting': phase === 'thinking' }">
              <span aria-hidden="true"></span>{{ sessionStatus }}
            </p>
          </header>

          <div ref="dialogueViewport" class="dialogue-viewport" tabindex="0" aria-label="面试聊天记录">
            <TransitionGroup :key="viewedTopic ?? topicIndex" tag="ol" name="chat" class="exchange-list" aria-live="polite" aria-relevant="additions">
              <li v-for="(entry, index) in displayedExchanges" :key="index" class="exchange"
                :class="{ 'exchange-own': entry.speaker === 'candidate', 'exchange-current': !previousTopic && index === exchanges.length - 1 && entry.speaker === 'interviewer' }">
                <span class="exchange-avatar" aria-hidden="true">
                  <el-icon><User v-if="entry.speaker === 'candidate'" /><ChatDotRound v-else /></el-icon>
                </span>
                <div class="exchange-body">
                  <span class="exchange-role">{{ entry.speaker === 'interviewer' ? '面试官' : '我' }}</span>
                  <p>{{ entry.before }}<mark v-if="entry.highlight">{{ entry.highlight }}</mark>{{ entry.after }}</p>
                </div>
              </li>
            </TransitionGroup>
          </div>

          <footer v-if="previousTopic" class="chat-footer history-footer">
            <p>历史问答</p>
          </footer>
          <footer v-else class="chat-footer">
            <form class="chat-composer" aria-label="回答面试官" @submit.prevent="submitAnswer">
              <div v-if="!isVoice" class="composer-row">
                <label class="visually-hidden" for="written-answer">输入回答</label>
                <textarea id="written-answer" v-model="draft" rows="1" placeholder="输入你的回答"
                  :readonly="phase === 'thinking'" @keydown.enter.exact="sendOnEnter"></textarea>
                <el-button type="primary" native-type="submit" :disabled="!draft.trim() || phase === 'thinking'">发送</el-button>
              </div>

              <div v-else-if="phase === 'review'" class="voice-review">
                <label for="voice-review">核对转写</label>
                <textarea id="voice-review" ref="reviewField" v-model="draft" rows="2"></textarea>
                <div class="review-actions">
                  <el-button text @click="restartAnswer">重新录音</el-button>
                  <el-button type="primary" native-type="submit" :disabled="!draft.trim()">发送</el-button>
                </div>
              </div>

              <button v-else class="record-button" :class="{ 'record-running': recording }" type="button"
                :disabled="phase === 'thinking'" @click="recording ? reviewTranscript() : startRecording()">
                <el-icon aria-hidden="true"><VideoPause v-if="recording" /><Microphone v-else /></el-icon>
                <span>{{ recording ? '说完了' : '点击录音' }}</span>
                <span v-if="recording" class="record-wave" aria-hidden="true">
                  <i v-for="(bar, index) in wave" :key="index" :style="{ '--bar-height': `${bar}px`, '--bar-delay': `${index * -85}ms` }"></i>
                </span>
                <span v-if="recording" class="record-timer" aria-hidden="true">{{ timerLabel }}</span>
                <span v-else class="record-demo">语音演示</span>
              </button>
            </form>
          </footer>
        </section>

        <aside class="answer-sidebar" aria-labelledby="answer-progress-heading">
          <header class="progress-header">
            <h2 id="answer-progress-heading">本场进度</h2>
            <span class="topic-count" aria-hidden="true">{{ history.length }} / {{ topics.length }}</span>
          </header>
          <div class="progress-segments" role="progressbar" aria-label="已完成题数" :aria-valuenow="history.length" :aria-valuemax="topics.length">
            <span v-for="(_, index) in topics" :key="index" :class="{ 'segment-done': index < history.length, 'segment-current': index === topicIndex }"></span>
          </div>
          <ol class="topic-list" aria-label="面试题目">
            <li v-for="(item, index) in topics" :key="item.title">
              <button v-if="index <= topicIndex" class="topic-row" type="button"
                :class="{ 'topic-live': index === topicIndex, 'topic-selected': index === (viewedTopic ?? topicIndex) }"
                :aria-current="index === (viewedTopic ?? topicIndex) ? 'true' : undefined"
                @click="viewTopic(index === topicIndex ? null : index)">
                <span class="topic-step" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="topic-row-label">
                  <strong>{{ history[index]?.title ?? topic.title }}</strong>
                  <small>{{ index === topicIndex ? '作答中' : '已完成' }}</small>
                </span>
                <el-icon aria-hidden="true"><ArrowRight v-if="index === topicIndex || index === viewedTopic" /><Check v-else /></el-icon>
              </button>
              <div v-else class="topic-row topic-pending">
                <span class="topic-step" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="topic-row-label"><strong>第 {{ index + 1 }} 题</strong><small>待开始</small></span>
              </div>
            </li>
          </ol>
          <dl v-if="usesResume !== undefined" class="answer-details">
            <div><dt>简历</dt><dd>{{ usesResume ? '已关联本场' : '未关联' }}</dd></div>
          </dl>
        </aside>
      </main>

      <el-dialog v-model="exitOpen" title="结束本场面试？" width="min(460px, calc(100vw - 32px))">
        <p>交卷后将结束本场面试。</p>
        <template #footer>
          <el-button @click="exitOpen = false">继续面试</el-button>
          <el-button type="danger" @click="finish">结束交卷</el-button>
        </template>
      </el-dialog>
    </div>
  </Teleport>
</template>

<style src="./interview-answer.css"></style>
