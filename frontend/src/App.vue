<script setup lang="ts">
import { ref } from 'vue'
import http from '@/api/http'
import type { Result } from '@/types/api'

// —— 临时联调代码（D1 晚冒烟），D2 换布局壳时删除 ——
const helloReply = ref('')

async function callHello(): Promise<void> {
  try {
    // 拦截器已验过 code=0，走到这里就是成功；res.data 是 Result，payload 在 .data.data
    const res = await http.get<Result<string>>('/hello')
    helloReply.value = res.data.data ?? ''
  } catch {
    // 失败文案已由拦截器统一弹出，此处不再重复处理
  }
}
</script>

<template>
  <el-button type="primary" @click="callHello">联调 hello</el-button>
  <p v-if="helloReply">{{ helloReply }}</p>
</template>

<style scoped></style>
