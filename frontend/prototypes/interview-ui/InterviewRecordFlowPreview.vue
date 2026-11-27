<script setup lang="ts">
import { ArrowLeft, ArrowRight, Check, Clock, Document, Files, Refresh } from '@element-plus/icons-vue'

defineProps<{ view: 'records' | 'detail'; reportGenerating?: boolean }>()
const emit = defineEmits<{ open: []; back: []; report: []; repeat: [] }>()
</script>

<template>
  <section v-if="view === 'records'" class="record-list-preview" aria-labelledby="records-heading">
    <header class="record-list-heading">
      <div><h2 id="records-heading">全部面试记录</h2><p>查看每场练习的过程与分析结果。</p></div>
      <span>1 场记录</span>
    </header>
    <button class="record-list-row" type="button" @click="emit('open')">
      <span class="record-type-icon"><el-icon aria-hidden="true"><Document /></el-icon></span>
      <span class="record-list-copy">
        <strong>Java 后端开发实习生</strong>
        <span>技术面 · 标准难度 · 文字面试</span>
      </span>
      <span class="record-list-date">2026年11月26日</span>
      <span class="record-list-score"><b>56</b><small>综合分</small></span>
      <span class="record-list-status"><el-icon aria-hidden="true"><Check /></el-icon>已完成</span>
      <el-icon class="record-list-arrow" aria-hidden="true"><ArrowRight /></el-icon>
    </button>
    <p class="record-list-note">演示记录 · 示例面试数据</p>
  </section>

  <section v-else class="interview-detail-preview" aria-labelledby="interview-detail-heading">
    <button class="record-back-link" type="button" @click="emit('back')"><el-icon aria-hidden="true"><ArrowLeft /></el-icon>返回面试记录</button>
    <section class="record-detail-hero">
      <div class="record-detail-copy">
        <span class="record-detail-state" :class="{ 'state-generating': reportGenerating }"><span></span>{{ reportGenerating ? '报告生成中' : '面试已完成' }}</span>
        <h2 id="interview-detail-heading">Java 后端开发实习生</h2>
        <p>技术面 <i></i> 标准难度 <i></i> 文字面试</p>
        <span class="record-detail-date">2026年11月26日 · 用时 32 分钟</span>
      </div>
      <div class="record-detail-score"><strong>56</strong><span>综合评分</span></div>
    </section>

    <section class="record-detail-stats" aria-label="本场统计">
      <div><span>面试轮次</span><strong>1 轮</strong></div>
      <div><span>完成题目</span><strong>6 道</strong></div>
      <div><span>已评分</span><strong>5 道</strong></div>
      <div><span>面试形式</span><strong>文字问答</strong></div>
    </section>

    <section class="record-detail-agenda" aria-labelledby="agenda-heading">
      <header><div><h3 id="agenda-heading">本场安排</h3><p>围绕岗位能力、基础知识与项目经验展开。</p></div><span>技术面 · 第 1 轮</span></header>
      <div class="agenda-topics">
        <span><el-icon aria-hidden="true"><Files /></el-icon>岗位基础与机制理解</span>
        <span><el-icon aria-hidden="true"><Document /></el-icon>项目经历与个人贡献</span>
        <span><el-icon aria-hidden="true"><Clock /></el-icon>场景分析与故障排查</span>
      </div>
    </section>

    <footer class="record-detail-actions">
      <div><h3>查看本场面试分析</h3><p>包含综合评价、优势与改进建议，以及逐题表现。</p></div>
      <div><el-button class="record-repeat-button" :icon="Refresh" @click="emit('repeat')">再面一次</el-button><el-button type="primary" :icon="ArrowRight" @click="emit('report')">查看面试报告</el-button></div>
    </footer>
    <p class="report-disclaimer">本页内容为交互演示数据，不代表真实面试评分。</p>
  </section>
</template>

<style src="./interview-record-flow.css"></style>
