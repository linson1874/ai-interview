<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { answerQuestion, finishInterview, followup, getInterview } from '@/api/interview'
import type { AnswerScoreVO, FollowupVO, QuestionItemVO } from '@/types/api'

// —— 页面卡 D3-2：答题页（07 J1/J2/S13/S15 + 10 §A/B/D/X4/X5；拍板七/八/九）——
// 已知形状现实：QuestionItemVO 无 score 字段（后端冻结），刷新后每题从"未评分"
// 视角开始，F1 门控只认本会话返回的判分响应；重答=正常重评流程（P4 覆盖，无害）
const route = useRoute()
const router = useRouter()
const interviewId = Number(route.params.id)

const pageLoading = ref(true)
const closed = ref(false) // status=2 已交卷（作答端点 CAS 弹死的场景前端先挡）

/** 追问树节点：主题行为根，追问行运行期追加（拍板七：detail 只给主题行，刷新即消失） */
interface TreeNode {
  id: number
  category?: string // 追问行继承父题 category/difficulty（#62 仅展示用；FollowupVO 不带，主题行才有）
  difficulty?: number
  question: string
  isFollowup: boolean
  strategy?: FollowupVO['strategy']
  depth?: number
  source?: FollowupVO['source']
  score: number | null // F1 门控与判分区数据源：null = 本会话未评分
  comment: string | null
  refAnswer: string | null
  showRef: boolean // 拍板八：参考答案折叠面板默认收起
  answering: boolean // 该节点评分中（按钮 loading + F7）
  following: boolean // 该节点追问生成中
  children: TreeNode[]
}

const tree = ref<TreeNode[]>([])
const currentId = ref<number | null>(null)
const draft = ref('') // 当前题作答草稿（骨架期不按题持久化，切题即清——迭代项）

const current = computed<TreeNode | null>(() => {
  const walk = (nodes: TreeNode[]): TreeNode | null => {
    for (const n of nodes) {
      if (n.id === currentId.value) return n
      const hit = walk(n.children)
      if (hit) return hit
    }
    return null
  }
  return walk(tree.value)
})

const finishing = ref(false)

function buildRoots(questions: QuestionItemVO[]): TreeNode[] {
  return questions.map((q) => ({
    id: q.id,
    category: q.category,
    difficulty: q.difficulty,
    question: q.question,
    isFollowup: false,
    score: null,
    comment: null,
    refAnswer: null,
    showRef: false,
    answering: false,
    following: false,
    children: [],
  }))
}

function selectNode(node: TreeNode): void {
  currentId.value = node.id
  draft.value = '' // 骨架期草稿不按题保留（迭代项：按节点缓存草稿）
}

onMounted(async () => {
  try {
    const detail = await getInterview(interviewId)
    if (detail.status === 2) {
      closed.value = true // 已交卷：状态门（作答端点 CAS 弹死的场景前端先挡）
    } else if (detail.status === 1 && detail.questions) {
      tree.value = buildRoots(detail.questions)
      currentId.value = tree.value[0]?.id ?? null
    } else {
      // status 0/4/3：题目窗口未开（R1），回创建页
      ElMessage.info('本场题目尚未就绪')
      router.push('/interviews/new')
    }
  } catch {
    // 404 归属边等文案已由拦截器统一弹
    router.push('/interviews/new')
  } finally {
    pageLoading.value = false
  }
})

async function onSubmitAnswer(): Promise<void> {
  const node = current.value
  if (!node) return
  const text = draft.value.trim()
  if (!text) return // P2 前端先拦：空作答不发请求（后端 @NotBlank 是保险丝）
  if (node.answering) return // F7 防连点
  node.answering = true
  try {
    const vo = await answerQuestion(interviewId, node.id, { answer: text })
    node.score = vo.score
    node.comment = vo.comment
    node.refAnswer = vo.refAnswer
    node.showRef = false // 拍板八：先看点评再对答案，默认收起
    ElMessage.success(`本题评分：${vo.score} 分`)
  } catch {
    // S13 降级"可重新提交重评"/S15 stale——拦截器统一弹；答案已落库（J1），draft 保留供重交
  } finally {
    node.answering = false
  }
}

async function onFollowup(node: TreeNode): Promise<void> {
  // F1 门控（#67 判分窗口案）：判分响应未返回（score=null）绝不 enable
  if (node.score === null || node.following) return
  node.following = true
  try {
    const vo = await followup(interviewId, node.id)
    const childNode: TreeNode = {
      id: vo.questionId,
      question: vo.question,
      isFollowup: true,
      strategy: vo.strategy,
      depth: vo.depth,
      source: vo.source,
      score: null,
      comment: null,
      refAnswer: null,
      showRef: false,
      answering: false,
      following: false,
      children: [],
    }
    node.children.push(childNode)
    selectNode(childNode) // 跳到追问节点作答——用刚 push 的对象自身，不留下标访问的 undefined 口子
    ElMessage.success(`追问已生成（第 ${vo.depth} 层）`)
  } catch {
    // X4 深度顶/X5 场次超限等熔断 code=1 文案——拦截器统一弹
  } finally {
    node.following = false
  }
}

async function onFinish(): Promise<void> {
  // 交卷烧报告流程，防手滑加确认（未评分场景后端 P10/P11 已兜底，前端不设硬门）
  const action = await ElMessageBox.confirm('交卷后将开始生成报告，确定交卷吗？', '交卷确认', {
    confirmButtonText: '交卷',
    cancelButtonText: '再检查一下',
    type: 'warning',
  }).catch(() => null)
  if (action !== 'confirm') return
  finishing.value = true
  try {
    await finishInterview(interviewId)
    ElMessage.success('已交卷，报告生成中')
    router.push(`/interviews/${interviewId}/report`)
  } catch {
    // 状态语言 code=1（如已交卷重复交）由拦截器统一弹
  } finally {
    finishing.value = false
  }
}
</script>

<template>
  <div v-loading="pageLoading" class="answer-page">
    <!-- 状态门：已交卷 -->
    <el-empty v-if="closed" description="本场已交卷">
      <el-button type="primary" @click="router.push(`/interviews/${interviewId}/report`)">
        去看报告
      </el-button>
    </el-empty>

    <template v-else>
      <header class="answer-page__bar">
        <h1 class="answer-page__title">面试进行中</h1>
        <el-button type="danger" plain :loading="finishing" @click="onFinish">交卷</el-button>
      </header>

      <div class="answer-page__layout">
        <!-- 左：题目导航（主题 + 追问树，缩进即深度——F8 骨架版，拍板九） -->
        <nav class="answer-page__nav">
          <template v-for="root in tree" :key="root.id">
            <button
              class="nav-node"
              :class="{ 'nav-node--active': currentId === root.id }"
              @click="selectNode(root)"
            >
              <span class="nav-node__badges">
                <el-tag size="small">{{ root.category }}</el-tag>
                <el-tag size="small" type="info">难度{{ root.difficulty }}</el-tag>
              </span>
              <span class="nav-node__text">{{ root.question }}</span>
              <span class="nav-node__state">
                {{ root.score === null ? '未评分' : `${root.score}分` }}
              </span>
            </button>
            <!-- 追问行：缩进 + 策略徽章（骨架期文字化，配色归设计官） -->
            <template v-for="child in root.children" :key="child.id">
              <button
                class="nav-node nav-node--followup"
                :class="{ 'nav-node--active': currentId === child.id }"
                @click="selectNode(child)"
              >
                <span class="nav-node__badges">
                  <el-tag size="small" type="warning">追问·{{ child.strategy }}</el-tag>
                  <el-tag size="small" type="info">第{{ child.depth }}层</el-tag>
                  <el-tag v-if="child.source" size="small" type="info">{{ child.source }}</el-tag>
                </span>
                <span class="nav-node__text">{{ child.question }}</span>
                <span class="nav-node__state">
                  {{ child.score === null ? '未评分' : `${child.score}分` }}
                </span>
              </button>
            </template>
          </template>
        </nav>

        <!-- 右：当前题主区 -->
        <section v-if="current" class="answer-page__main">
          <div class="question-head">
            <el-tag v-if="current.isFollowup" type="warning">
              追问·{{ current.strategy }}·第{{ current.depth }}层
            </el-tag>
            <template v-else>
              <el-tag>{{ current.category }}</el-tag>
              <el-tag type="info">难度{{ current.difficulty }}</el-tag>
            </template>
          </div>

          <!-- F3 渲染纪律：题干文本插值，禁 v-html -->
          <p class="question-text">{{ current.question }}</p>

          <el-input
            v-model="draft"
            type="textarea"
            :rows="8"
            maxlength="1000"
            show-word-limit
            placeholder="写下你的作答（10-1000 字更佳），提交后同步评分"
          />

          <div class="question-actions">
            <el-button
              type="primary"
              :loading="current.answering"
              :disabled="!draft.trim()"
              @click="onSubmitAnswer"
            >
              提交作答
            </el-button>
            <!-- F1 门控：score 非 null 才可追问（#67 判分窗口案） -->
            <el-button
              :loading="current.following"
              :disabled="current.score === null"
              @click="onFollowup(current)"
            >
              追问
            </el-button>
          </div>

          <!-- 判分区：评完出现 -->
          <div v-if="current.score !== null" class="score-panel">
            <p class="score-panel__score">得分：{{ current.score }} / 5</p>
            <p class="score-panel__comment">{{ current.comment }}</p>
            <!-- 拍板八：参考答案默认收起 -->
            <el-button link type="primary" @click="current.showRef = !current.showRef">
              {{ current.showRef ? '收起参考答案' : '查看参考答案' }}
            </el-button>
            <p v-if="current.showRef" class="score-panel__ref">{{ current.refAnswer }}</p>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 骨架期朴素排版：设计官接管时重排（完成环第 5 步） */
.answer-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 24px;
}
.answer-page__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.answer-page__title {
  margin: 0;
  font-size: 20px;
  color: var(--zoo-ink);
}
.answer-page__layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: 24px;
  align-items: start;
}
.answer-page__nav {
  display: grid;
  gap: 8px;
}
.nav-node {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--ai-radius-md);
  background: #ffffff;
  color: var(--zoo-ink);
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}
.nav-node--followup {
  margin-left: 24px; /* F8：parent 链缩进即深度（骨架版） */
  border-style: dashed;
}
.nav-node--active {
  background: var(--zoo-water);
  font-weight: 600;
}
.nav-node__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.nav-node__text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.nav-node__state {
  color: var(--zoo-ink-soft);
  font-size: 12px;
}
.question-head {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.question-text {
  margin: 0 0 16px;
  font-size: 16px;
  line-height: 1.7;
  color: var(--zoo-ink);
}
.question-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
.score-panel {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--ai-radius-md);
  background: var(--zoo-water);
}
.score-panel__score {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--zoo-ink);
}
.score-panel__comment {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--zoo-ink);
}
.score-panel__ref {
  margin: 8px 0 0;
  padding-top: 8px;
  border-top: 1px dashed var(--zoo-ink-soft);
  font-size: 13px;
  line-height: 1.6;
  color: var(--zoo-ink-soft);
  white-space: pre-wrap;
}
</style>
