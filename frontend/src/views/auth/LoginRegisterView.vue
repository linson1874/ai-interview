<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { login, register } from '@/api/auth'
import type { LoginParams, RegisterParams } from '@/types/api'

const router = useRouter()
const activeTab = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loginSubmitting = ref(false)
const registerSubmitting = ref(false)

const loginForm = reactive<LoginParams>({ username: '', password: '' })
const registerForm = reactive<RegisterParams>({ username: '', password: '', nickname: '' })

const loginRules: FormRules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }],
}
const registerRules: FormRules = {
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度3-32', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度6-32', trigger: 'blur' },
  ],
  nickname: [{ max: 32, message: '昵称最长32字符', trigger: 'blur' }],
}

async function onRegister(): Promise<void> {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid || registerSubmitting.value) return
  registerSubmitting.value = true
  try {
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    loginForm.username = registerForm.username
    loginForm.password = ''
    registerForm.password = ''
    registerForm.nickname = ''
    activeTab.value = 'login'
  } catch {
    // The shared HTTP interceptor owns authentication error messages.
  } finally {
    registerSubmitting.value = false
  }
}

async function onLogin(): Promise<void> {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid || loginSubmitting.value) return
  loginSubmitting.value = true
  try {
    const vo = await login(loginForm)
    ElMessage.success('欢迎回来，' + vo.nickname)
    router.push('/resumes')
  } catch {
    // The shared HTTP interceptor owns authentication error messages.
  } finally {
    loginSubmitting.value = false
  }
}
</script>

<template>
  <main class="dossier-auth">
    <section class="dossier-stage" aria-labelledby="dossier-title">
      <header class="dossier-stage__top">
        <div class="dossier-brand">
          <span class="dossier-brand__mark" aria-hidden="true"><i /></span>
          <strong>AI 面试助手</strong>
        </div>
        <span class="dossier-stage__edition">个人练习版</span>
      </header>

      <div class="dossier-stage__body">
        <h1 id="dossier-title" class="dossier-stage__title">
          <span>把简历上的每一句，</span>
          <span>练成说出口的答案。</span>
        </h1>
        <p class="dossier-stage__lede">
          管理简历，选择目标岗位，开始一场模拟面试。每一次练习，都留下可以回看的记录。
        </p>

        <article class="resume-sheet" aria-label="简历批注示例">
          <header class="resume-sheet__head">
            <strong>项目经历</strong>
            <span>简历节选 · 示例</span>
          </header>
          <div class="resume-sheet__skeleton" aria-hidden="true"><i /><i /></div>
          <p class="resume-sheet__excerpt">
            主导了一次 <mark>从 0 到 1 的改版</mark>，协调三个小组完成上线。
          </p>
          <p class="resume-sheet__note">
            <span>评估提示</span>
            继续补充你负责的技术决策与结果指标。
          </p>
          <div class="resume-sheet__question">
            <i aria-hidden="true" />
            <p><small>模拟提问</small>“这次改版里，最难推动的一项决定是什么？”</p>
          </div>
        </article>
      </div>

      <footer class="dossier-stage__foot">
        <span>每一次练习，都有迹可循。</span>
        <span>AI 面试助手 · 个人练习空间</span>
      </footer>
    </section>

    <section class="dossier-account" aria-labelledby="account-title">
      <div class="dossier-account__inner">
        <header class="dossier-account__heading">
          <h2 id="account-title">{{ activeTab === 'login' ? '欢迎回来' : '创建账户' }}</h2>
          <p>
            {{ activeTab === 'login' ? '登录后继续你的面试练习。' : '创建个人账号，保存简历与面试记录。' }}
          </p>
        </header>

        <el-tabs v-model="activeTab" class="dossier-tabs">
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              class="dossier-form"
              hide-required-asterisk
              @submit.prevent="onLogin"
            >
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="loginForm.username"
                  size="large"
                  autocomplete="username"
                  placeholder="请输入用户名"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  size="large"
                  show-password
                  autocomplete="current-password"
                  placeholder="请输入密码"
                />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="dossier-submit"
                native-type="submit"
                :loading="loginSubmitting"
              >
                <span>登录并继续</span>
                <el-icon aria-hidden="true"><ArrowRight /></el-icon>
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="创建账户" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              label-position="top"
              class="dossier-form"
              hide-required-asterisk
              @submit.prevent="onRegister"
            >
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="registerForm.username"
                  size="large"
                  autocomplete="username"
                  placeholder="3-32 个字符"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  size="large"
                  show-password
                  autocomplete="new-password"
                  placeholder="6-32 个字符"
                />
              </el-form-item>
              <el-form-item label="昵称（可选）" prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  size="large"
                  autocomplete="nickname"
                  placeholder="登录后用来称呼你"
                />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="dossier-submit"
                native-type="submit"
                :loading="registerSubmitting"
              >
                <span>创建账户</span>
                <el-icon aria-hidden="true"><ArrowRight /></el-icon>
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <p class="dossier-account__foot">账号用于保存简历与面试记录</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.dossier-auth {
  --el-color-primary: var(--dossier-ink);
  --el-color-primary-dark-2: #10151b;
  --el-color-primary-light-3: #60666e;
  --el-color-primary-light-5: #92979d;
  --el-color-primary-light-7: #c1c4c7;
  --el-color-primary-light-8: #d9dbdc;
  --el-color-primary-light-9: #eceeed;
  --el-color-danger: var(--dossier-brick);
  display: grid;
  grid-template-columns: minmax(0, 56%) minmax(400px, 44%);
  min-height: 100dvh;
  color: var(--dossier-ink);
  background: var(--dossier-paper);
  font-family: var(--ai-font-sans);
}

.dossier-stage {
  position: relative;
  display: flex;
  min-width: 0;
  min-height: 100dvh;
  flex-direction: column;
  overflow: hidden;
  padding: 42px clamp(40px, 6vw, 88px) 38px;
  background: var(--dossier-ink);
  color: var(--dossier-paper);
}

.dossier-stage::after {
  position: absolute;
  right: clamp(40px, 6vw, 88px);
  bottom: 32px;
  left: clamp(40px, 6vw, 88px);
  height: 1px;
  background: rgba(245, 245, 241, 0.17);
  content: '';
}

.dossier-stage__top,
.dossier-stage__foot {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.dossier-brand {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  font-size: 13px;
}

.dossier-brand__mark {
  display: grid;
  width: 24px;
  height: 24px;
  flex: none;
  place-items: center;
  border: 1px solid var(--dossier-brass);
}

.dossier-brand__mark i {
  width: 10px;
  height: 2px;
  transform: translateY(3px);
  background: var(--dossier-brass);
}

.dossier-stage__edition {
  color: #8c96a2;
  font-size: 12px;
}

.dossier-stage__body {
  width: min(100%, 620px);
  margin: auto 0;
  padding: 54px 0 62px;
}

.dossier-stage__title {
  display: grid;
  gap: 2px;
  margin: 0;
  font-family: var(--dossier-serif);
  font-size: 52px;
  font-weight: 600;
  line-height: 1.3;
}

.dossier-stage__title span {
  display: block;
}

.dossier-stage__lede {
  max-width: 48ch;
  margin: 18px 0 0;
  color: #aab2bc;
  font-size: 14px;
  line-height: 1.9;
}

.resume-sheet {
  width: min(100%, 478px);
  margin-top: 42px;
  padding: 25px 30px 23px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 3px;
  background: var(--dossier-paper);
  color: var(--dossier-ink);
  box-shadow: 0 26px 60px rgba(0, 0, 0, 0.34);
  animation: sheet-arrive 760ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.resume-sheet__head {
  display: flex;
  align-items: baseline;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(21, 26, 33, 0.14);
}

.resume-sheet__head strong {
  font-family: var(--dossier-serif);
  font-size: 15px;
  font-weight: 600;
}

.resume-sheet__head span {
  color: var(--dossier-slate);
  font-size: 11px;
}

.resume-sheet__skeleton {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.resume-sheet__skeleton i {
  display: block;
  height: 7px;
  border-radius: 2px;
  background: rgba(21, 26, 33, 0.11);
}

.resume-sheet__skeleton i:first-child { width: 88%; }
.resume-sheet__skeleton i:last-child { width: 71%; }

.resume-sheet__excerpt {
  margin: 18px 0 0;
  color: #40474f;
  font-family: var(--dossier-serif);
  font-size: 14px;
  line-height: 1.9;
}

.resume-sheet__excerpt mark {
  color: var(--dossier-ink);
  background: linear-gradient(transparent 59%, rgba(176, 138, 74, 0.36) 59% 92%, transparent 92%);
  background-size: 100% 100%;
  animation: annotation-reveal 900ms 380ms both;
}

.resume-sheet__note {
  margin: 10px 0 0 14px;
  padding-left: 12px;
  border-left: 2px solid var(--dossier-brass);
  color: #5e6670;
  font-size: 12px;
  line-height: 1.7;
}

.resume-sheet__note span {
  display: block;
  margin-bottom: 2px;
  color: #8a6b36;
  font-size: 10px;
}

.resume-sheet__question {
  display: flex;
  align-items: stretch;
  gap: 12px;
  margin-top: 18px;
  padding-top: 15px;
  border-top: 1px solid rgba(21, 26, 33, 0.14);
}

.resume-sheet__question > i {
  width: 2px;
  flex: 0 0 auto;
  background: var(--dossier-brass);
}

.resume-sheet__question p {
  margin: 0;
  color: #4a525b;
  font-size: 12px;
  line-height: 1.8;
}

.resume-sheet__question small {
  display: block;
  margin-bottom: 3px;
  color: var(--dossier-slate);
  font-size: 10px;
}

.dossier-stage__foot {
  z-index: 1;
  padding-bottom: 2px;
  color: #89929b;
  font-size: 11px;
}

.dossier-account {
  display: flex;
  min-width: 0;
  min-height: 100dvh;
  flex-direction: column;
  padding: 44px clamp(36px, 6vw, 92px) 36px;
  border-left: 1px solid var(--dossier-rule);
  background: var(--dossier-sheet);
}

.dossier-account__inner {
  width: min(100%, 388px);
  margin: auto 0;
  padding: 38px 0;
}

.dossier-account__heading h2 {
  margin: 0;
  font-family: var(--dossier-serif);
  font-size: 30px;
  font-weight: 600;
  line-height: 1.35;
}

.dossier-account__heading p {
  margin: 8px 0 0;
  color: var(--dossier-slate);
  font-size: 13px;
  line-height: 1.7;
}

.dossier-tabs {
  width: 100%;
  margin-top: 28px;
}

.dossier-tabs :deep(.el-tabs__header) {
  margin: 0 0 24px;
}

.dossier-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--dossier-rule);
}

.dossier-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  background: var(--dossier-brass);
}

.dossier-tabs :deep(.el-tabs__item) {
  height: 40px;
  padding: 0 14px;
  color: var(--dossier-slate);
  font-size: 13px;
}

.dossier-tabs :deep(.el-tabs__item.is-active) {
  color: var(--dossier-ink);
  font-weight: 600;
}

.dossier-tabs :deep(.el-tabs__item:focus-visible) {
  outline: 2px solid var(--dossier-brass);
  outline-offset: 2px;
}

.dossier-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.dossier-form :deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: var(--dossier-ink);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.dossier-form :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #d2d5d2;
  border-radius: 3px;
  background: #f6f6f3;
  box-shadow: none;
  transition: border-color 180ms ease, background-color 180ms ease, box-shadow 180ms ease;
}

.dossier-form :deep(.el-input__wrapper:hover) {
  border-color: #a7aaa6;
  background: #ffffff;
  box-shadow: none;
}

.dossier-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--dossier-brass);
  outline: 2px solid rgba(176, 138, 74, 0.18);
  outline-offset: 1px;
  background: #ffffff;
  box-shadow: none;
}

.dossier-form :deep(.el-input__inner) {
  color: var(--dossier-ink);
  font-size: 14px;
}

.dossier-form :deep(.el-input__inner::placeholder) {
  color: var(--dossier-slate);
}

.dossier-form :deep(.el-form-item.is-error .el-input__wrapper) {
  border-color: var(--dossier-brick);
  outline: 2px solid rgba(169, 70, 57, 0.12);
  box-shadow: none;
}

.dossier-form :deep(.el-form-item__error) {
  padding-top: 5px;
  color: var(--dossier-brick);
  font-size: 12px;
}

.dossier-submit {
  display: flex;
  width: 100%;
  min-height: 48px;
  justify-content: center;
  gap: 9px;
  margin-top: 4px;
  border: 1px solid var(--dossier-ink);
  border-radius: 3px;
  background: var(--dossier-ink);
  color: var(--dossier-sheet);
  font-size: 14px;
  font-weight: 600;
  transition: transform 180ms ease, background-color 180ms ease, box-shadow 180ms ease;
  --el-button-bg-color: var(--dossier-ink);
  --el-button-border-color: var(--dossier-ink);
  --el-button-text-color: var(--dossier-sheet);
  --el-button-hover-bg-color: var(--dossier-ink-2);
  --el-button-hover-border-color: var(--dossier-ink-2);
  --el-button-hover-text-color: var(--dossier-sheet);
  --el-button-active-bg-color: #10151b;
  --el-button-active-border-color: #10151b;
  --el-button-active-text-color: var(--dossier-sheet);
}

.dossier-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(21, 26, 33, 0.16);
}

.dossier-submit:active {
  transform: translateY(0);
}

.dossier-submit .el-icon {
  transition: transform 180ms ease;
}

.dossier-submit:hover .el-icon {
  transform: translateX(3px);
}

.dossier-submit:focus-visible {
  outline: 2px solid var(--dossier-brass);
  outline-offset: 3px;
}

.dossier-account__foot {
  margin: 23px 0 0;
  color: var(--dossier-slate);
  font-size: 11px;
  line-height: 1.6;
}

@keyframes sheet-arrive {
  from { opacity: 0; transform: translateY(18px) rotate(-0.45deg); }
  to { opacity: 1; transform: translateY(0) rotate(0); }
}

@keyframes annotation-reveal {
  from { background-size: 0 100%; }
  to { background-size: 100% 100%; }
}

@media (max-width: 1120px) and (min-width: 901px) {
  .dossier-auth { grid-template-columns: minmax(0, 54fr) minmax(400px, 46fr); }
  .dossier-stage { padding-inline: 42px; }
  .dossier-stage::after { right: 42px; left: 42px; }
  .dossier-stage__title { font-size: 44px; }
  .dossier-account { padding-inline: 42px; }
}

@media (max-width: 900px) {
  .dossier-auth { display: flex; flex-direction: column; }
  .dossier-account {
    order: -1;
    min-height: 660px;
    padding: 36px 28px 30px;
    border-right: 0;
    border-bottom: 1px solid var(--dossier-rule);
    border-left: 0;
  }
  .dossier-account__inner { margin: auto; }
  .dossier-stage { min-height: 620px; padding: 30px 34px 36px; }
  .dossier-stage::after { right: 34px; bottom: 24px; left: 34px; }
  .dossier-stage__body { margin: auto; padding: 42px 0 50px; }
  .dossier-stage__title { font-size: 42px; }
  .resume-sheet { margin-top: 30px; }
}

@media (max-width: 560px) {
  .dossier-account { min-height: 620px; padding: 28px 22px; }
  .dossier-account__inner { padding: 24px 0; }
  .dossier-account__heading h2 { font-size: 27px; }
  .dossier-tabs { margin-top: 24px; }
  .dossier-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }
  .dossier-form :deep(.el-form-item) { margin-bottom: 15px; }
  .dossier-stage { min-height: 600px; padding: 24px 22px 32px; }
  .dossier-stage::after { right: 22px; left: 22px; }
  .dossier-stage__body { padding: 36px 0 44px; }
  .dossier-stage__title { font-size: 34px; line-height: 1.36; }
  .dossier-stage__lede { font-size: 13px; }
  .resume-sheet { margin-top: 24px; padding: 21px 20px 19px; }
  .dossier-stage__foot { align-items: flex-start; flex-direction: column; gap: 4px; }
}

@media (prefers-reduced-motion: reduce) {
  .resume-sheet,
  .resume-sheet__excerpt mark { animation: none; }
  .dossier-submit,
  .dossier-submit .el-icon,
  .dossier-form :deep(.el-input__wrapper) { transition: none; }
}
</style>
