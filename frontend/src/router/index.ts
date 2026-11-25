import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/http'

// 路由表（合同11 §4：未登录可达面仅 /login，其余全守卫）
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/resumes' }, // 登录第一站=简历列表（页面卡 D2-1 拍板二）
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginRegisterView.vue'),
    },
    {
      path: '/resumes',
      name: 'resumes',
      component: () => import('@/views/resume/ResumeListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resumes/new',
      name: 'resume-new',
      component: () => import('@/views/resume/ResumeEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/resumes/:id/edit',
      name: 'resume-edit',
      component: () => import('@/views/resume/ResumeEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/interviews/new',
      name: 'interview-new',
      component: () => import('@/views/interview/InterviewCreateView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/interviews/:id/answer',
      name: 'interview-answer',
      component: () => import('@/views/interview/InterviewAnswerView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

// 全局守卫（§4 拍板：beforeEach + meta.requiresAuth）。
// getToken 在回调内调用而非模块顶层——http.ts 与本文件存在双向引用（http 的 401 处理
// 要 push /login），但引用都发生在运行期回调里，ESM 循环依赖无害。
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !getToken()) {
    return { name: 'login' } // 未登录访问受保护页 → 弹回登录页（断言 6 的落点）
  }
  if (to.name === 'login' && getToken()) {
    return { name: 'resumes' } // 已登录访问登录页 → 直接送回简历列表
  }
})

export default router
