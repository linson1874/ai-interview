<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'
import type { ResumeRecord } from './resume'

defineProps<{ resumes: readonly ResumeRecord[]; analyzing: boolean }>()
defineEmits<{ edit: [resume: ResumeRecord]; analyze: [resume: ResumeRecord] }>()
</script>

<template>
  <div class="resume-list" aria-label="简历列表">
    <div v-if="resumes.length" class="resume-list-heading" aria-hidden="true">
      <span>文件名称</span><span>分析状态</span><span>操作</span>
    </div>
    <ul v-if="resumes.length" class="resume-rows">
      <li v-for="resume in resumes" :key="resume.id" class="resume-row">
        <div class="file-details">
          <el-icon class="file-icon" aria-hidden="true"><Document /></el-icon>
          <div class="file-name"><strong>{{ resume.name }}</strong><span>{{ resume.role || '未设置目标岗位' }}</span></div>
        </div>
        <span class="analysis-label" :class="{ 'is-ready': resume.analyzed }">{{ resume.analyzed ? '已有分析' : '待分析' }}</span>
        <div class="resume-row-actions">
          <el-button link @click="$emit('edit', resume)">编辑</el-button>
          <el-button link type="primary" :disabled="analyzing" @click="$emit('analyze', resume)">{{ resume.analyzed ? '查看分析' : 'AI 分析' }}</el-button>
        </div>
      </li>
    </ul>
    <p v-else class="analysis-empty">上传一份 PDF 后，可以管理简历和查看 AI 分析。</p>
  </div>
</template>
