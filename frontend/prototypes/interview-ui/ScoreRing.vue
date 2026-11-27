<script setup lang="ts">
defineProps<{ score: number }>()
</script>

<template>
  <div class="score-ring" aria-hidden="true">
    <el-progress type="circle" :percentage="score" :width="116" :stroke-width="8" color="var(--score-ring-fill, var(--accent))">
      <div class="score-value"><strong>{{ score }}</strong><span> / 100</span></div>
    </el-progress>
  </div>
  <span class="visually-hidden">{{ score }} 分，满分 100 分</span>
</template>
