<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createResume, getResume, updateResume } from '@/api/resume'
import type { ResumeSaveParams } from '@/types/api'

// —— 页面卡 D2-2 编辑页：新建/编辑共用表单（03 合同 §2.1/§3/D6；拍板三：空可空字段转 null）——
const route = useRoute()
const router = useRouter()

// 路由区分模式：/resumes/new 新建；/resumes/:id/edit 编辑
const editId = computed<number | null>(() => {
  const raw = route.params.id
  return raw ? Number(raw) : null
})
const isEdit = computed(() => editId.value !== null)

const formRef = ref<FormInstance>()
const saveSubmitting = ref(false)
const pageLoading = ref(false)

const form = reactive<ResumeSaveParams>({
  realName: '',
  targetJob: '',
  education: null,
  skills: [],
  selfIntro: null,
})

// skills 动态标签：单条 ≤20 字、总量 ≤10（03 §2），第 11 条直接拒绝录入
const skillInput = ref('')
const SKILL_MAX_COUNT = 10
const SKILL_MAX_LEN = 20

function addSkill(): void {
  const value = skillInput.value.trim() // 契约口径：先 trim 后判空（03 §2.1 实现备注）
  if (!value) return
  if (form.skills.length >= SKILL_MAX_COUNT) {
    ElMessage.warning('最多只能添加10项技能')
    return
  }
  if (value.length > SKILL_MAX_LEN) {
    ElMessage.warning('单个技能名称不能超过20个字')
    return
  }
  if (form.skills.includes(value)) return
  form.skills.push(value)
  skillInput.value = ''
}

function removeSkill(index: number): void {
  form.skills.splice(index, 1)
}

// 校验规则逐字镜像 03 合同 §2.1 提示语定稿表——不发明新文案
const rules: FormRules = {
  realName: [
    { required: true, message: '真实姓名不能为空', trigger: 'blur' },
    { min: 2, max: 20, message: '真实姓名长度需在2-20个字符之间', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5A-Za-z\s·]+$/, message: '真实姓名仅支持中英文、空格与"·"', trigger: 'blur' },
  ],
  targetJob: [
    { required: true, message: '目标岗位不能为空', trigger: 'blur' },
    { min: 2, max: 50, message: '目标岗位名称过短或过长', trigger: 'blur' },
  ],
  education: [{ max: 64, message: '学历描述不能超过64个字符', trigger: 'blur' }],
  skills: [
    {
      required: true,
      // value 是 ElementPlus 校验器传入的 any：any 家法注释位——形状由本 rules 定义
      // （string[]），在回调内自行收窄，类型系统无法从 FormRules 签名推断
      validator: (_rule, value: string[], callback) => {
        if (!value || value.length === 0) return callback(new Error('请至少填写一项技能'))
        if (value.length > SKILL_MAX_COUNT) return callback(new Error('最多只能添加10项技能'))
        if (value.some((s) => s.length > SKILL_MAX_LEN)) {
          return callback(new Error('单个技能名称不能超过20个字'))
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  selfIntro: [
    {
      // value 为 any，理由同上（此处形状是 string | null）
      validator: (_rule, value: string | null, callback) => {
        if (!value) return callback() // 选填语义：null/空 合法（03 §2：选填≠空字符串歧义已封死）
        if (value.length < 10) return callback(new Error('自我介绍不能少于10个字'))
        if (value.length > 1000) return callback(new Error('自我介绍不能超过1000个字'))
        callback()
      },
      trigger: 'blur',
    },
  ],
}

onMounted(async () => {
  if (!isEdit.value) return
  pageLoading.value = true
  try {
    const detail = await getResume(editId.value!)
    form.realName = detail.realName
    form.targetJob = detail.targetJob
    form.education = detail.education
    form.skills = [...detail.skills]
    form.selfIntro = detail.selfIntro
  } catch {
    // 404（不存在/非本人/已删）文案由拦截器统一弹出；死页停留无意义，回列表
    router.push('/resumes')
  } finally {
    pageLoading.value = false
  }
})

async function onSave(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || saveSubmitting.value) return // F7：防连点
  saveSubmitting.value = true
  try {
    // 拍板三：可空字段 trim 后为空 → 提交 null（空串会吃 code=2"少于10个字"，null 才是选填语义）
    const params: ResumeSaveParams = {
      realName: form.realName.trim(),
      targetJob: form.targetJob.trim(),
      education: form.education?.trim() || null,
      skills: form.skills,
      selfIntro: form.selfIntro?.trim() || null,
    }
    if (isEdit.value) {
      await updateResume(editId.value!, params) // D6 全量替换：形状完整，删空即清空
    } else {
      await createResume(params)
    }
    ElMessage.success('保存成功') // 文案权在前端（03 §2.1：后端成功 msg 统一"ok"）
    router.push('/resumes')
  } catch {
    // 失败文案由拦截器统一弹出（文案出口唯一），页面只复位状态
  } finally {
    saveSubmitting.value = false
  }
}
</script>

<template>
  <div v-loading="pageLoading" class="resume-edit">
    <header class="resume-edit__bar">
      <h1 class="resume-edit__title">{{ isEdit ? '编辑简历' : '新建简历' }}</h1>
      <el-button @click="router.push('/resumes')">返回列表</el-button>
    </header>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="resume-edit__form"
      @submit.prevent="onSave"
    >
      <el-form-item label="真实姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="2-20 字，中英文、空格与·" />
      </el-form-item>
      <el-form-item label="目标岗位" prop="targetJob">
        <el-input v-model="form.targetJob" placeholder="2-50 字" />
      </el-form-item>
      <el-form-item label="学历描述（选填）" prop="education">
        <el-input v-model="form.education" placeholder="最长 64 字；留空保存 = 清空" />
      </el-form-item>
      <el-form-item label="技能" prop="skills">
        <div class="resume-edit__skills">
          <el-tag
            v-for="(skill, i) in form.skills"
            :key="skill"
            closable
            class="resume-edit__skill-tag"
            @close="removeSkill(i)"
          >
            {{ skill }}
          </el-tag>
          <div class="resume-edit__skill-input">
            <el-input
              v-model="skillInput"
              :disabled="form.skills.length >= SKILL_MAX_COUNT"
              placeholder="单条最长 20 字，回车或点添加"
              @keyup.enter="addSkill"
            />
            <el-button :disabled="form.skills.length >= SKILL_MAX_COUNT" @click="addSkill">
              添加
            </el-button>
          </div>
          <p v-if="form.skills.length >= SKILL_MAX_COUNT" class="resume-edit__skill-hint">
            最多只能添加10项技能
          </p>
        </div>
      </el-form-item>
      <el-form-item label="自我介绍（选填）" prop="selfIntro">
        <el-input
          v-model="form.selfIntro"
          type="textarea"
          :rows="6"
          maxlength="1000"
          show-word-limit
          placeholder="10-1000 字；留空保存 = 清空"
        />
      </el-form-item>
      <el-button type="primary" :loading="saveSubmitting" native-type="submit">保存</el-button>
    </el-form>
  </div>
</template>

<style scoped>
/* 骨架期朴素排版：设计官 UI 稿到位后整体接管（完成环第 5 步） */
.resume-edit {
  max-width: 640px;
  margin: 0 auto;
  padding: 32px 24px;
}
.resume-edit__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.resume-edit__title {
  margin: 0;
  font-size: 20px;
  color: var(--zoo-ink);
}
.resume-edit__form {
  max-width: 520px;
}
.resume-edit__skills {
  width: 100%;
}
.resume-edit__skill-tag {
  margin: 0 6px 6px 0;
}
.resume-edit__skill-input {
  display: flex;
  gap: 8px;
  max-width: 360px;
}
.resume-edit__skill-hint {
  margin: 6px 0 0;
  color: var(--zoo-alert);
  font-size: 12px;
}
</style>
