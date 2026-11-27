<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ArrowRight, Document, Link, Upload } from '@element-plus/icons-vue'
import AssessmentDetails from './AssessmentDetails.vue'
import InterviewAnswerPreview from './InterviewAnswerPreview.vue'
import InterviewRecordFlowPreview from './InterviewRecordFlowPreview.vue'
import InterviewReportPreview from './InterviewReportPreview.vue'
import InterviewSetup from './InterviewSetup.vue'
import LoginPreview from './LoginPreview.vue'
import ResumeList from './ResumeList.vue'
import ReviewCenterPreview from './ReviewCenterPreview.vue'
import ReviewPracticePreview from './ReviewPracticePreview.vue'
import ScoreRing from './ScoreRing.vue'
import WorkspaceShell from './WorkspaceShell.vue'
import { dimensions, exampleResumes, improvements, overview } from './resume'
import type { ResumeRecord } from './resume'
import type { AnswerSession } from './answer-examples'
import type { ReviewOutcome } from './review-data'

const viewTitles = { login: '登录', resumes: '我的简历', resume: '简历分析', interview: '模拟面试', records: '面试记录', 'interview-detail': '面试详情', report: '面试报告', review: '复习中心', 'review-practice': '单题复习', components: '组件示例' }
type View = keyof typeof viewTitles

const view = ref<View>('login')
const heading = ref<HTMLElement>()
const resumes = ref(exampleResumes.map((resume) => ({ ...resume })))
const activeResumeId = ref(1)
const activeResume = computed(() => resumes.value.find((resume) => resume.id === activeResumeId.value))
const interviewResume = ref<ResumeRecord | null>(null)
const repeatSession = ref(false)
const answerSession = ref<AnswerSession | null>(null)
const reportGenerating = ref(false)
const reportReturnView = ref<'records' | 'interview-detail'>('interview-detail')
const reviewTaskId = ref('mysql-index')
const completedReviewTasks = ref<Record<string, ReviewOutcome>>({})
const editingResume = ref<ResumeRecord>()
const editorOpen = ref(false)
let nextResumeId = 3
const fileInput = ref<HTMLInputElement>()
const selectedFile = ref('')
const fileError = ref('')
const reportState = ref<'processing' | 'ready'>('ready')
const reportHeading = ref<HTMLElement>()
const progress = ref(0)
const targetRole = ref('Java 后端开发实习生')
const difficulty = ref('标准')
const analysisState = ref('有数据')
const totalScore = Math.round(dimensions.reduce((sum, row) => sum + row.score, 0) / dimensions.length)
const progressStage = computed(() => {
  if (progress.value < 30) return '准备示例数据'
  if (progress.value < 75) return '生成示例评分'
  if (progress.value < 100) return '整理示例报告'
  return '示例分析完成'
})
let progressTimer: number | undefined

function syncView() {
  const route = window.location.hash.slice(1)
  const query = new URLSearchParams(window.location.search)

  if (route === 'answer') {
    view.value = 'interview'
    answerSession.value = window.history.state?.answerSession ?? null
  } else if (query.get('answer') === 'example') {
    view.value = 'interview'
    answerSession.value = { role: '前端工程师', round: '技术面', format: '语音', seeded: true }
  } else if (Object.hasOwn(viewTitles, route)) {
    view.value = route as View
    answerSession.value = null
  } else {
    view.value = 'login'
    reportGenerating.value = false
    answerSession.value = null
  }
  document.title = `${answerSession.value ? '面试作答' : viewTitles[view.value]} · AI 面试助手`
}

async function changeView(nextView: View) {
  if (view.value === nextView) return
  window.location.hash = nextView
  syncView()
  await nextTick()
  window.scrollTo(0, 0)
  heading.value?.focus({ preventScroll: true })
}

function openFilePicker() {
  if (reportState.value !== 'processing') fileInput.value?.click()
}

async function selectLocalFile(event: Event) {
  const input = event.currentTarget as HTMLInputElement
  const file = input.files?.item(0)
  input.value = ''
  if (!file) return

  if (!file.name.toLowerCase().endsWith('.pdf') || file.size === 0) {
    fileError.value = '请选择一份有效的 PDF 简历。'
    return
  }
  const resume: ResumeRecord = { id: nextResumeId++, name: file.name, role: '', analyzed: false }
  resumes.value.unshift(resume)
  await openAnalysis(resume)
}

async function openAnalysis(resume: ResumeRecord) {
  if (reportState.value === 'processing') return
  activeResumeId.value = resume.id
  fileError.value = ''
  if (!resume.analyzed) startExampleAnalysis(resume)
  await changeView('resume')
}

function navigate(nextView: View) {
  if (nextView === 'interview' && view.value !== 'interview') interviewResume.value = null
  return changeView(nextView)
}

function startInterview(resume: ResumeRecord | null = null) {
  repeatSession.value = false
  interviewResume.value = resume
  return changeView('interview')
}

function repeatInterview() {
  repeatSession.value = true
  interviewResume.value = null
  return changeView('interview')
}

function enterAnswer(session: AnswerSession) {
  window.history.replaceState({ answerSession: session }, '', '#answer')
  answerSession.value = session
  document.title = '面试作答 · AI 面试助手'
}

function openNewInterviewDetail() {
  reportGenerating.value = true
  answerSession.value = null
  window.history.replaceState(null, '', '#interview-detail')
  view.value = 'interview-detail'
  document.title = '面试详情 · AI 面试助手'
  window.scrollTo(0, 0)
}

function openExistingInterviewReport() {
  reportGenerating.value = false
  return openReport('records')
}

function openReport(returnView: 'records' | 'interview-detail' = 'interview-detail') {
  reportReturnView.value = returnView
  answerSession.value = null
  window.history.replaceState(null, '', '#report')
  view.value = 'report'
  document.title = '面试报告 · AI 面试助手'
  window.scrollTo(0, 0)
}

async function leaveAnswer() {
  const url = new URL(window.location.href)
  url.searchParams.delete('answer')
  url.hash = 'interview'
  window.history.replaceState(null, '', url)
  syncView()
  await nextTick()
  heading.value?.focus({ preventScroll: true })
}

function openReviewCenter() {
  reportGenerating.value = false
  changeView('review')
}

function startReview(taskId: string) {
  reviewTaskId.value = taskId
  changeView('review-practice')
}

function finishReview(taskId: string, outcome: ReviewOutcome) {
  completedReviewTasks.value = { ...completedReviewTasks.value, [taskId]: outcome }
  changeView('review')
}

function editResume(resume: ResumeRecord) {
  editingResume.value = { ...resume }
  editorOpen.value = true
}

function saveResume() {
  const draft = editingResume.value
  if (!draft?.name.trim()) return
  const original = resumes.value.find((resume) => resume.id === draft.id)
  if (!original) return
  original.name = draft.name.trim()
  if (original.role !== draft.role.trim()) original.analyzed = false
  original.role = draft.role.trim()
  editorOpen.value = false
}

function stopProgress() {
  window.clearInterval(progressTimer)
  progressTimer = undefined
}

function startExampleAnalysis(resume: ResumeRecord) {
  selectedFile.value = resume.name
  reportState.value = 'processing'
  progress.value = 0
  progressTimer = window.setInterval(async () => {
    if (progress.value < 100) {
      progress.value = Math.min(100, progress.value + 5)
      return
    }

    stopProgress()
    const record = resumes.value.find(({ id }) => id === resume.id)
    if (record) record.analyzed = true
    reportState.value = 'ready'
    await nextTick()
    reportHeading.value?.focus({ preventScroll: true })
  }, 145)
}

onMounted(() => {
  syncView()
  window.addEventListener('hashchange', syncView)
})
onUnmounted(() => {
  stopProgress()
  window.removeEventListener('hashchange', syncView)
})
</script>

<template>
  <WorkspaceShell v-if="view !== 'login'" v-show="!answerSession" :view="view" @change="navigate">
    <div class="workspace-content">
      <header v-if="view !== 'report' && view !== 'review-practice'" class="page-header">
        <h1 ref="heading" tabindex="-1">{{ viewTitles[view] }}</h1>
        <div v-if="view === 'resumes'" class="page-actions">
          <el-button type="primary" :icon="Upload" :disabled="reportState === 'processing'" @click="openFilePicker">上传简历</el-button>
        </div>
        <div v-else-if="view === 'resume'" class="page-actions">
          <el-button @click="changeView('resumes')">返回简历列表</el-button>
          <el-button v-if="reportState === 'ready' && activeResume" type="primary" @click="startInterview(activeResume)">去模拟面试<el-icon class="el-icon--right" aria-hidden="true"><ArrowRight /></el-icon></el-button>
        </div>
        <el-button v-else-if="view === 'components'" @click="changeView('resumes')">返回我的简历</el-button>
      </header>

      <template v-if="view === 'resumes'">
        <p v-if="fileError" class="file-error" role="alert">{{ fileError }}</p>
        <p v-if="reportState === 'processing'" class="resume-list-notice">示例分析正在生成。<el-button link @click="changeView('resume')">查看进度</el-button></p>
        <ResumeList :resumes="resumes" :analyzing="reportState === 'processing'" @edit="editResume" @analyze="openAnalysis" />
      </template>

      <template v-else-if="view === 'resume'">
        <section v-if="reportState === 'processing'" class="upload-state progress-state" aria-labelledby="progress-heading">
          <div class="upload-icon" aria-hidden="true"><el-icon><Document /></el-icon></div>
          <h2 id="progress-heading">正在生成示例报告</h2>
          <p v-if="selectedFile" class="progress-file">{{ selectedFile }}</p>
          <div class="progress-meter">
            <div class="progress-caption"><span role="status">{{ progressStage }}</span><strong>{{ progress }}%</strong></div>
            <el-progress :percentage="progress" :show-text="false" :stroke-width="8" color="var(--accent)" aria-label="示例分析进度" />
          </div>
        </section>

        <template v-else>
          <div class="resume-context">
            <div class="file-details">
              <el-icon class="file-icon" aria-hidden="true"><Document /></el-icon>
              <div class="file-name"><strong>{{ activeResume?.name }}</strong><span>{{ activeResume?.role || '未设置目标岗位' }}</span></div>
              <span class="sample-label">示例分析报告 · Java 后端样例</span>
            </div>
          </div>

          <section class="report-overview" aria-labelledby="overview-heading">
            <div class="overview-copy">
              <h2 id="overview-heading" ref="reportHeading" tabindex="-1">核心评价</h2>
              <p class="overview-verdict">{{ overview.verdict }}</p>
              <p>{{ overview.summary }}</p>
            </div>
            <div class="overview-score">
              <ScoreRing :score="totalScore" />
              <span>综合评分</span>
            </div>
          </section>

          <AssessmentDetails :rows="dimensions" heading-id="analysis-details" />

          <section class="report-issues" aria-labelledby="issues-heading">
            <h2 id="issues-heading">按优先级查看问题</h2>
            <section v-for="group in improvements" :key="group.id" class="issue-group" :class="`issue-${group.id}`" :aria-labelledby="`issue-${group.id}`">
              <h3 :id="`issue-${group.id}`">{{ group.title }}</h3>
              <ul class="issue-list">
                <li v-for="item in group.items" :key="item.title"><h4>{{ item.title }}</h4><p>{{ item.reason }}</p></li>
              </ul>
            </section>
          </section>
        </template>
      </template>

      <InterviewSetup v-else-if="view === 'interview'" :resumes="resumes" :initial-resume="interviewResume" :initial-role="repeatSession ? 'Java 后端开发实习生' : undefined" :initial-difficulty="repeatSession ? '标准' : undefined" @enter="enterAnswer" />

      <InterviewRecordFlowPreview v-else-if="view === 'records'" view="records" @open="openExistingInterviewReport" />

      <InterviewRecordFlowPreview v-else-if="view === 'interview-detail'" view="detail" :report-generating="reportGenerating" @back="changeView('records')" @report="openReport('interview-detail')" @repeat="repeatInterview" />

      <InterviewReportPreview v-else-if="view === 'report'" :initially-generating="reportGenerating" :back-label="reportReturnView === 'records' ? '面试记录' : '面试详情'" @back="changeView(reportReturnView)" @repeat="repeatInterview" @review="openReviewCenter" @ready="reportGenerating = false" />

      <ReviewCenterPreview v-else-if="view === 'review'" :completed="completedReviewTasks" @practice="startReview" @interview="startInterview" />

      <ReviewPracticePreview v-else-if="view === 'review-practice'" :task-id="reviewTaskId" @back="changeView('review')" @complete="finishReview" />

      <div v-else class="component-examples">
        <section aria-labelledby="actions-heading">
          <h2 id="actions-heading">按钮与表单</h2>
          <div class="example-actions">
            <el-button type="primary" @click="startInterview()">去模拟面试</el-button>
            <el-button :icon="Upload" :disabled="reportState === 'processing'" @click="changeView('resumes')">管理简历</el-button>
            <el-button :icon="Link" link @click="changeView('resume')">查看分析</el-button>
            <el-button disabled>等待分析</el-button>
          </div>
          <el-form label-position="top">
            <el-form-item label="目标岗位">
              <el-input v-model="targetRole" placeholder="如 Java 后端开发实习生" />
            </el-form-item>
            <el-form-item label="练习难度">
              <el-radio-group v-model="difficulty">
                <el-radio-button value="入门">入门</el-radio-button>
                <el-radio-button value="标准">标准</el-radio-button>
                <el-radio-button value="进阶">进阶</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </section>
        <section aria-labelledby="charts-heading">
          <div class="section-heading">
            <h2 id="charts-heading">评分组件</h2>
            <el-radio-group v-model="analysisState" size="small" aria-label="分项评分状态">
              <el-radio-button value="有数据">有数据</el-radio-button>
              <el-radio-button value="暂无数据">暂无数据</el-radio-button>
            </el-radio-group>
          </div>
          <div class="example-ring"><ScoreRing :score="totalScore" /></div>
          <AssessmentDetails :rows="analysisState === '有数据' ? dimensions : []" heading-id="example-details" />
        </section>
      </div>
    </div>
  </WorkspaceShell>
  <LoginPreview v-if="view === 'login'" @enter="changeView('resumes')" />
  <InterviewAnswerPreview v-if="answerSession" v-bind="answerSession" @leave="leaveAnswer" @detail="openNewInterviewDetail" />

  <input ref="fileInput" type="file" accept=".pdf,application/pdf" hidden @change="selectLocalFile">
  <el-dialog v-model="editorOpen" title="编辑简历信息" width="min(440px, calc(100vw - 32px))">
    <el-form v-if="editingResume" label-position="top" @submit.prevent="saveResume">
      <el-form-item label="简历名称"><el-input v-model="editingResume.name" maxlength="100" /></el-form-item>
      <el-form-item label="目标岗位"><el-input v-model="editingResume.role" maxlength="50" /></el-form-item>
      <div class="editor-actions">
        <el-button @click="editorOpen = false">取消</el-button>
        <el-button type="primary" native-type="submit" :disabled="!editingResume.name.trim()">保存</el-button>
      </div>
    </el-form>
  </el-dialog>
</template>
