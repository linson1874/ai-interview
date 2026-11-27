<script setup lang="ts">
import { ChatDotRound, Collection, Document, Files } from '@element-plus/icons-vue'

type View = 'resumes' | 'resume' | 'interview' | 'records' | 'interview-detail' | 'report' | 'review' | 'review-practice' | 'components'

defineProps<{ view: View }>()
defineEmits<{ change: [view: View] }>()
</script>

<template>
  <div class="workspace-shell">
    <aside class="workspace-sidebar" aria-label="工作区侧栏">
      <a class="workspace-brand" href="#resumes" @click.prevent="$emit('change', 'resumes')">
        <span class="brand-icon"><el-icon aria-hidden="true"><ChatDotRound /></el-icon></span>
        AI 面试助手
      </a>
      <nav class="workspace-nav" aria-label="主导航">
        <a href="#resumes" :aria-current="view === 'resumes' || view === 'resume' ? 'page' : undefined" @click.prevent="$emit('change', 'resumes')">
          <el-icon aria-hidden="true"><Document /></el-icon><span>我的简历</span>
        </a>
        <a href="#interview" :aria-current="view === 'interview' ? 'page' : undefined" @click.prevent="$emit('change', 'interview')">
          <el-icon aria-hidden="true"><ChatDotRound /></el-icon><span>模拟面试</span>
        </a>
        <a href="#records" :aria-current="view === 'records' || view === 'interview-detail' || view === 'report' ? 'page' : undefined" @click.prevent="$emit('change', 'records')">
          <el-icon aria-hidden="true"><Files /></el-icon><span>面试记录</span>
        </a>
        <a href="#review" :aria-current="view === 'review' || view === 'review-practice' ? 'page' : undefined" @click.prevent="$emit('change', 'review')">
          <el-icon aria-hidden="true"><Collection /></el-icon><span>复习</span>
        </a>
      </nav>
    </aside>
    <main class="workspace-main" id="main-content">
      <slot />
    </main>
  </div>
</template>
