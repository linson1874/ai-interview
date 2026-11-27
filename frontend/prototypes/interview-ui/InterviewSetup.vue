<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { ArrowDown, ArrowRight, ArrowUp, ChatLineSquare, Check, CircleCheck, Delete, Microphone, Plus } from '@element-plus/icons-vue'
import { interviewRoleGroups } from './interview-roles'
import type { ResumeRecord } from './resume'
import type { AnswerSession } from './answer-examples'

const props = defineProps<{ resumes: readonly ResumeRecord[]; initialResume: ResumeRecord | null; initialRole?: string; initialDifficulty?: '入门' | '标准' | '进阶' }>()
const emit = defineEmits<{ enter: [session: AnswerSession] }>()

const roundTypes = ['岗位能力', '技术基础', '项目深挖', '情境问答', 'HR 面'] as const
type RoundType = typeof roundTypes[number]
type Preset = { id: string; title: string; rounds: readonly RoundType[] }
const roundDescriptions: Record<RoundType, string> = {
  岗位能力: '围绕目标岗位的常见任务，考查知识运用和问题分析。',
  技术基础: '考查岗位相关的基础概念、原理和常见使用场景。',
  项目深挖: '围绕你的实际经历，追问个人职责、方案取舍和结果。',
  情境问答: '给出一个工作场景，了解你的判断、沟通方式和处理步骤。',
  'HR 面': '交流求职动机、职业规划和过往的协作经历。',
}

const presets = {
  goal: [
    { id: 'quick', title: '快速练习', rounds: ['岗位能力'] },
    { id: 'deep', title: '项目深挖', rounds: ['项目深挖', '情境问答'] },
    { id: 'complete', title: '完整模拟', rounds: ['岗位能力', '项目深挖', 'HR 面'] },
  ],
  stage: [
    { id: 'first', title: '技术一面', rounds: ['技术基础'] },
    { id: 'second', title: '项目二面', rounds: ['项目深挖'] },
    { id: 'hr', title: 'HR 面', rounds: ['HR 面'] },
  ],
} satisfies Record<string, Preset[]>

const formats = [
  { value: '文字', label: '文字面试', description: '键盘输入回答', icon: ChatLineSquare },
  { value: '语音', label: '语音面试', description: '录音演示，发送前核对转写', icon: Microphone },
]

const targetRole = ref(props.initialResume?.role ?? props.initialRole ?? '')
const initialRoleGroup = interviewRoleGroups.find((group) => group.options.some((option) => option.title === targetRole.value))
const roleCategory = ref(initialRoleGroup?.id ?? (targetRole.value ? 'custom' : 'software'))
const customRole = ref(initialRoleGroup ? '' : targetRole.value)
const resumeId = ref(props.initialResume?.id ?? 0)
const presetCategory = ref<keyof typeof presets>('goal')
const presetId = ref('quick')
const difficulty = ref(props.initialDifficulty ?? '标准')
const format = ref<AnswerSession['format']>('文字')
const stage = ref<'configure' | 'generating' | 'ready' | 'failed'>('configure')
const roleError = ref(false)
const targetRoleInput = ref<HTMLInputElement>()
const roleHeading = ref<HTMLElement>()
const configurationHeading = ref<HTMLElement>()
const preparationHeading = ref<HTMLElement>()
const elapsedSeconds = ref(0)
const microphone = ref<'idle' | 'checking' | 'available' | 'failed'>('idle')
const microphoneError = ref('')
let microphoneAttempt = 0
let generationTimer: number | undefined
let readyConfiguration = ''
let failNextGeneration = new URLSearchParams(window.location.search).get('prepare') === 'fail-once'
let nextRoundId = 2
const rounds = ref<{ id: number; type: RoundType }[]>([{ id: 1, type: '岗位能力' }])

const roleOptions = computed(() => interviewRoleGroups.find((group) => group.id === roleCategory.value)?.options ?? [])
const selectedRoleFocus = computed(() => interviewRoleGroups.flatMap((group) => group.options).find((role) => role.title === targetRole.value)?.focus)
const selectedResume = computed(() => props.resumes.find((resume) => resume.id === resumeId.value))
const canEnter = computed(() => stage.value === 'ready' && (format.value !== '语音' || microphone.value === 'available'))
const preparationSteps = computed(() => [
  '创建面试会话',
  `准备「${targetRole.value}」面试内容`,
  '生成首轮面试题',
  format.value === '语音' ? '检查设备，准备入场' : '准备入场',
])
const preparationStep = computed(() => {
  if (canEnter.value) return preparationSteps.value.length
  if (stage.value === 'ready') return 3
  if (stage.value === 'failed') return 2
  if (elapsedSeconds.value < 2) return 1
  return elapsedSeconds.value < 4 ? 2 : 3
})
const opening = computed(() => {
  if (selectedResume.value) {
    return `请从简历选一项与你申请的${targetRole.value}岗位相关的经历。你负责哪部分，最后取得了什么结果？`
  }
  return `你认为${targetRole.value}最需要哪项能力？请用一次课程、社团或个人练习举例。`
})

function chooseRole(role: string, isCustom = false) {
  if (!isCustom) customRole.value = ''
  targetRole.value = role
  roleError.value = false
}

function editCustomRole(event: Event) {
  customRole.value = (event.target as HTMLInputElement).value
  chooseRole(customRole.value.trim(), true)
}

function choosePreset(value: string | number | boolean | undefined) {
  const preset = presets[presetCategory.value].find(({ id }) => id === value)
  if (!preset) return
  presetId.value = preset.id
  rounds.value = preset.rounds.map((type) => ({ id: nextRoundId++, type }))
}

function addRound() {
  rounds.value.push({ id: nextRoundId++, type: '岗位能力' })
}

function moveRound(index: number, offset: number) {
  const neighbor = index + offset
  const current = rounds.value[index]
  const adjacent = rounds.value[neighbor]
  if (!current || !adjacent) return
  rounds.value[index] = adjacent
  rounds.value[neighbor] = current
}

function showStage(next: typeof stage.value) {
  const previous = stage.value
  stage.value = next
  if (previous !== 'configure' && next !== 'configure') return
  window.scrollTo(0, 0)
  void nextTick(() => {
    if (stage.value !== next) return
    const heading = next === 'configure' ? configurationHeading.value : preparationHeading.value
    heading?.focus({ preventScroll: true })
  })
}

function stopGeneration() {
  window.clearInterval(generationTimer)
  generationTimer = undefined
}

function configurationKey() {
  return JSON.stringify([targetRole.value, resumeId.value, difficulty.value, format.value, rounds.value.map((round) => round.type)])
}

function generateQuestions() {
  stopGeneration()
  elapsedSeconds.value = 0
  showStage('generating')
  const startedAt = Date.now()
  const configuration = configurationKey()

  generationTimer = window.setInterval(() => {
    const elapsed = Date.now() - startedAt
    elapsedSeconds.value = Math.floor(elapsed / 1000)
    if (elapsed < 5000) return

    stopGeneration()
    if (failNextGeneration) {
      failNextGeneration = false
      showStage('failed')
      return
    }
    readyConfiguration = configuration
    showStage('ready')
  }, 250)
}

function resetMicrophone() {
  microphoneAttempt += 1
  microphone.value = 'idle'
  microphoneError.value = ''
}

async function checkMicrophone() {
  if (microphone.value === 'checking') return
  microphoneError.value = ''
  if (!navigator.mediaDevices?.getUserMedia) {
    microphone.value = 'failed'
    microphoneError.value = '当前浏览器无法访问麦克风。可以检查浏览器设置，或返回选择文字面试。'
    return
  }

  const attempt = ++microphoneAttempt
  microphone.value = 'checking'
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const audioTrack = stream.getAudioTracks()[0]
    const available = audioTrack?.readyState === 'live'
    for (const track of stream.getTracks()) track.stop()
    if (attempt !== microphoneAttempt) return
    microphone.value = available ? 'available' : 'failed'
    if (!available) microphoneError.value = '没有检测到可用的麦克风，请检查设备后重试。'
  } catch (error) {
    if (attempt !== microphoneAttempt) return
    microphone.value = 'failed'
    microphoneError.value = error instanceof DOMException && error.name === 'NotAllowedError'
      ? '麦克风权限未开启。请允许访问麦克风后重试。'
      : '无法使用麦克风。请检查设备连接和输入设置后重试。'
  }
}

function backToSetup() {
  if (stage.value === 'generating' || stage.value === 'failed') readyConfiguration = ''
  stopGeneration()
  resetMicrophone()
  showStage('configure')
}

function enterInterview() {
  if (!canEnter.value) return
  readyConfiguration = ''
  emit('enter', {
    role: targetRole.value,
    round: rounds.value[0]?.type ?? '岗位能力',
    format: format.value,
    difficulty: difficulty.value,
    usesResume: Boolean(selectedResume.value),
    initialQuestion: opening.value,
  })
  resetMicrophone()
  stage.value = 'configure'
}

function confirm() {
  targetRole.value = targetRole.value.trim()
  roleError.value = !targetRole.value
  if (roleError.value) {
    (targetRoleInput.value ?? roleHeading.value)?.focus()
    return
  }
  if (readyConfiguration === configurationKey()) showStage('ready')
  else generateQuestions()
}

onUnmounted(() => {
  stopGeneration()
  resetMicrophone()
})
</script>

<template>
  <form v-if="stage === 'configure'" class="interview-form" @submit.prevent="confirm">
    <section class="setup-section" aria-labelledby="interview-format">
      <h2 id="interview-format" ref="configurationHeading" tabindex="-1">面试形式</h2>
      <div class="format-options" role="radiogroup" aria-labelledby="interview-format">
        <label v-for="option in formats" :key="option.value" class="setup-choice format-choice">
          <input v-model="format" class="visually-hidden" type="radio" name="interview-format" :value="option.value">
          <span class="choice-icon" aria-hidden="true"><el-icon><component :is="option.icon" /></el-icon></span>
          <span class="choice-copy"><strong>{{ option.label }}</strong><small>{{ option.description }}</small></span>
          <el-icon class="choice-check" aria-hidden="true"><Check /></el-icon>
        </label>
      </div>
    </section>

    <section class="setup-section" aria-labelledby="interview-target">
      <h2 id="interview-target" ref="roleHeading" tabindex="-1">目标岗位</h2>
      <el-radio-group v-model="roleCategory" class="role-categories" aria-label="岗位分类">
        <el-radio-button v-for="group in interviewRoleGroups" :key="group.id" :value="group.id">{{ group.label }}</el-radio-button>
        <el-radio-button value="custom">自定义岗位</el-radio-button>
      </el-radio-group>

      <div v-if="roleCategory === 'custom'" class="setup-field custom-role">
        <label for="custom-role">岗位名称</label>
        <input id="custom-role" ref="targetRoleInput" type="text" :value="customRole" placeholder="例如：建筑设计助理"
          :aria-invalid="roleError || undefined" :aria-describedby="roleError ? 'role-error' : undefined" @input="editCustomRole">
      </div>
      <div v-else class="role-options" role="radiogroup" aria-labelledby="interview-target" :aria-describedby="roleError ? 'role-error' : undefined">
        <label v-for="role in roleOptions" :key="role.title" class="setup-choice role-choice">
          <input class="visually-hidden" type="radio" name="interview-role" :value="role.title" :checked="targetRole === role.title" @change="chooseRole(role.title)">
          <span class="choice-icon" aria-hidden="true"><el-icon><component :is="role.icon" /></el-icon></span>
          <span class="choice-copy">
            <strong>{{ role.title }}</strong>
            <span class="role-tags"><small v-for="tag in role.focus" :key="tag">{{ tag }}</small></span>
          </span>
          <el-icon class="choice-check" aria-hidden="true"><Check /></el-icon>
        </label>
      </div>
      <p v-if="roleError" id="role-error" class="field-error" role="alert">选择或输入一个目标岗位。</p>
    </section>

    <section class="setup-section" aria-labelledby="interview-preset">
      <div class="setup-section-line">
        <h2 id="interview-preset">面试预设</h2>
          <el-radio-group v-model="presetCategory" class="preset-categories" aria-label="预设分类" size="small">
          <el-radio-button value="goal">按练习目标</el-radio-button>
          <el-radio-button value="stage">按招聘轮次</el-radio-button>
        </el-radio-group>
      </div>
      <el-radio-group :model-value="presetId" class="preset-list" aria-label="面试预设" @change="choosePreset">
        <el-radio v-for="preset in presets[presetCategory]" :key="preset.id" :value="preset.id" class="preset-option">
          <strong>{{ preset.title }}</strong><span class="preset-content">{{ preset.rounds.join(' · ') }}</span><span class="preset-count">{{ preset.rounds.length }} 轮</span>
        </el-radio>
      </el-radio-group>
    </section>

    <section class="setup-section rounds-section" aria-labelledby="interview-rounds">
      <div class="setup-section-line">
        <h2 id="interview-rounds">本场轮次 <span>{{ rounds.length }} 轮</span></h2>
        <el-button link :icon="Plus" @click="addRound">添加一轮</el-button>
      </div>
      <ol class="round-list">
        <li v-for="(round, index) in rounds" :key="round.id" class="round-row">
          <label :for="`round-${round.id}`">第 {{ index + 1 }} 轮</label>
          <el-select :id="`round-${round.id}`" v-model="round.type">
            <el-option v-for="type in roundTypes" :key="type" :label="type" :value="type" />
          </el-select>
          <div class="round-actions">
            <el-button text :icon="ArrowUp" :aria-label="`上移第 ${index + 1} 轮`" :disabled="index === 0" @click="moveRound(index, -1)" />
            <el-button text :icon="ArrowDown" :aria-label="`下移第 ${index + 1} 轮`" :disabled="index === rounds.length - 1" @click="moveRound(index, 1)" />
            <el-button text :icon="Delete" :aria-label="`删除第 ${index + 1} 轮`" :disabled="rounds.length < 2" @click="rounds.splice(index, 1)" />
          </div>
        </li>
      </ol>

      <div class="setup-fields round-settings" role="group" aria-label="场次设置">
        <fieldset class="setup-field">
          <legend>难度</legend>
          <el-radio-group v-model="difficulty" aria-label="难度">
            <el-radio-button value="入门">入门</el-radio-button>
            <el-radio-button value="标准">标准</el-radio-button>
            <el-radio-button value="进阶">进阶</el-radio-button>
          </el-radio-group>
        </fieldset>
        <div class="setup-field">
          <label for="interview-resume">使用简历</label>
          <el-select id="interview-resume" v-model="resumeId">
            <el-option label="不带简历" :value="0" />
            <el-option v-for="resume in resumes" :key="resume.id" :label="resume.name" :value="resume.id" />
          </el-select>
        </div>
      </div>

      <footer class="setup-footer">
        <p>{{ targetRole || '待选岗位' }}<span>{{ rounds.length }} 轮 · {{ difficulty }} · {{ format }}</span></p>
        <el-button type="primary" native-type="submit">确认面试安排<el-icon class="el-icon--right" aria-hidden="true"><ArrowRight /></el-icon></el-button>
      </footer>
    </section>
  </form>

  <section v-else class="interview-ready" aria-labelledby="ready-heading">
    <h2 id="ready-heading" ref="preparationHeading" tabindex="-1">面试准备</h2>
    <div class="prepare-workspace">
      <div class="prepare-status" :class="{ 'prepare-failed': stage === 'failed' }">
        <div class="prepare-status-line">
          <h3 v-if="stage === 'generating'" role="status">题目正在生成</h3>
          <h3 v-else-if="stage === 'ready'" role="status">题目已就绪</h3>
          <h3 v-else role="alert">题目生成失败</h3>
          <span v-if="stage === 'generating'">已等待 {{ elapsedSeconds }} 秒</span>
        </div>
        <div v-if="stage === 'generating'" class="prepare-progress" role="progressbar" aria-label="题目生成进度">
          <el-progress :percentage="44" :indeterminate="true" :show-text="false" :stroke-width="7" color="var(--accent)" aria-hidden="true" />
        </div>
        <p v-else-if="stage === 'ready'" class="prepare-hint">{{ canEnter ? '准备好后，点击「开始面试」入场。' : '完成麦克风检查后即可开始。' }}</p>
        <p v-else class="prepare-hint">可以按本场安排重试。</p>
      </div>

      <div class="prepare-layout">
        <section class="interview-pass" aria-labelledby="pass-heading">
          <div class="pass-heading">
            <h3 id="pass-heading">面试工牌</h3>
            <p>{{ targetRole }}</p>
          </div>
          <dl class="ready-details">
            <div><dt>面试形式</dt><dd>{{ format }}</dd></div>
            <div><dt>难度</dt><dd>{{ difficulty }}</dd></div>
            <div v-if="selectedRoleFocus" class="pass-resume"><dt>考察重点</dt><dd>{{ selectedRoleFocus.join('、') }}</dd></div>
            <div class="pass-resume"><dt>使用简历</dt><dd>{{ selectedResume?.name ?? '不带简历' }}</dd></div>
          </dl>
        </section>

        <section class="prepare-panel" aria-labelledby="prepare-steps-heading">
          <h3 id="prepare-steps-heading">入场进度</h3>
          <ol class="prepare-steps" aria-label="示例准备步骤">
            <li v-for="(step, index) in preparationSteps" :key="step"
              :class="{ 'step-complete': index < preparationStep, 'step-current': index === preparationStep, 'step-failed': index === preparationStep && stage === 'failed' }"
              :aria-current="index === preparationStep ? 'step' : undefined">
              <span class="step-marker" aria-hidden="true"><el-icon v-if="index < preparationStep"><CircleCheck /></el-icon></span>
              <span class="step-label">{{ step }}</span>
              <span class="step-state">{{ index < preparationStep ? '已完成' : index > preparationStep ? '待开始' : stage === 'failed' ? '失败' : stage === 'ready' ? '待检查' : '进行中' }}</span>
            </li>
          </ol>
        </section>
      </div>

      <section class="prepare-rounds" aria-labelledby="prepare-rounds-heading">
        <h3 id="prepare-rounds-heading">本场流程 <span>{{ rounds.length }} 轮</span></h3>
        <ol class="ready-rounds">
          <li v-for="(round, index) in rounds" :key="round.id">
            <span>第 {{ index + 1 }} 轮</span>
            <strong>{{ round.type }}</strong>
            <p>{{ roundDescriptions[round.type] }}</p>
          </li>
        </ol>
      </section>

      <section v-if="format === '语音'" class="microphone-check" aria-labelledby="microphone-heading">
        <div>
          <h3 id="microphone-heading"><el-icon aria-hidden="true"><Microphone /></el-icon> 麦克风检查</h3>
          <p v-if="microphone === 'available'" role="status">麦克风可用</p>
          <p v-else-if="microphone === 'checking'" role="status">等待浏览器授权或设备响应</p>
          <p v-else-if="microphone === 'failed'" role="alert">{{ microphoneError }}</p>
        </div>
        <el-button :loading="microphone === 'checking'" @click="checkMicrophone">
          {{ microphone === 'failed' ? '重新检查' : '检查麦克风' }}
        </el-button>
      </section>

      <div class="ready-actions">
        <el-button @click="backToSetup">返回修改</el-button>
        <el-button v-if="stage === 'failed'" type="primary" @click="generateQuestions">重试生成</el-button>
        <el-button v-else type="primary" :disabled="!canEnter" @click="enterInterview">开始面试</el-button>
      </div>
    </div>
  </section>

</template>
