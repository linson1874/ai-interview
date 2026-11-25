<script setup lang="ts">
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'

// 版面路由：登录页独立全屏；受保护页面统一套进内部区外壳（左侧导览目录 + 顶栏）。
const route = useRoute()
</script>

<template>
  <AppShell v-if="route.meta.requiresAuth">
    <router-view />
  </AppShell>
  <router-view v-else />
</template>
