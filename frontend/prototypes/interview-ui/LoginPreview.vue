<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ChatDotRound, Collection, Document, Files } from '@element-plus/icons-vue'
import './login-preview.css'

const emit = defineEmits<{ enter: [] }>()
const activeTab = ref<'login' | 'register'>('login')
const previewMode = ref<'resume' | 'interview' | 'review'>('interview')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loginSubmitting = ref(false)
const registerSubmitting = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '' })

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度为 3-32 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 个字符', trigger: 'blur' },
  ],
  nickname: [{ max: 32, message: '昵称不能超过 32 个字符', trigger: 'blur' }],
}

async function submitLogin() {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid || loginSubmitting.value) return

  loginSubmitting.value = true
  window.setTimeout(() => {
    ElMessage.success('演示登录成功')
    emit('enter')
    loginSubmitting.value = false
  }, 320)
}

async function submitRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid || registerSubmitting.value) return

  registerSubmitting.value = true
  window.setTimeout(() => {
    loginForm.username = registerForm.username
    loginForm.password = ''
    registerForm.password = ''
    registerForm.nickname = ''
    activeTab.value = 'login'
    ElMessage.success('注册流程演示完成，请登录')
    registerSubmitting.value = false
  }, 320)
}
</script>

<template>
  <main class="auth-screen">
    <section class="auth-story" aria-labelledby="story-title">
      <div class="story-content">
        <div class="story-heading">
          <h1 id="story-title">面试准备<br>从简历开始。</h1>
          <p>分析经历、选择岗位、进入真实面试节奏，再回到薄弱点继续练习。</p>
        </div>

        <section class="session-window" aria-label="产品流程互动预览">
          <header class="session-window-head">
            <div class="window-identity">
              <span class="window-mark"><el-icon aria-hidden="true"><ChatDotRound /></el-icon></span>
              <span><strong>AI 面试助手</strong><small>产品流程预览</small></span>
            </div>
            <span class="window-live"><i aria-hidden="true"></i>演示场景</span>
          </header>

          <div class="preview-switcher" aria-label="选择预览内容">
            <button type="button" :aria-pressed="previewMode === 'resume'" @click="previewMode = 'resume'">
              <el-icon aria-hidden="true"><Document /></el-icon><span>简历分析</span>
            </button>
            <button type="button" :aria-pressed="previewMode === 'interview'" @click="previewMode = 'interview'">
              <el-icon aria-hidden="true"><ChatDotRound /></el-icon><span>模拟面试</span>
            </button>
            <button type="button" :aria-pressed="previewMode === 'review'" @click="previewMode = 'review'">
              <el-icon aria-hidden="true"><Files /></el-icon><span>面试复盘</span>
            </button>
          </div>

          <Transition name="scene-change" mode="out-in">
            <div :key="previewMode" class="preview-scene">
              <template v-if="previewMode === 'resume'">
                <div class="scene-caption"><span>简历分析</span><span class="scene-state">等待上传</span></div>
                <div class="resume-preview-row">
                  <div class="resume-page-icon"><el-icon aria-hidden="true"><Document /></el-icon></div>
                  <div class="resume-preview-copy"><strong>目标岗位简历</strong><span>经历与岗位要求逐项对照</span></div>
                  <span class="resume-type">PDF</span>
                </div>
                <div class="analysis-lines" aria-label="简历分析维度">
                  <div><span>项目经历</span><i><b style="--line-size: 72%"></b></i></div>
                  <div><span>岗位匹配</span><i><b style="--line-size: 56%"></b></i></div>
                  <div><span>表达结构</span><i><b style="--line-size: 84%"></b></i></div>
                </div>
              </template>

              <template v-else-if="previewMode === 'interview'">
                <div class="scene-caption"><span>Java 后端 · 技术面</span><span class="scene-state">第 2 / 6 题</span></div>
                <div class="interviewer-line">
                  <span class="interviewer-avatar"><el-icon aria-hidden="true"><ChatDotRound /></el-icon></span>
                  <div><small>面试官</small><p>请结合一次项目经历，说说你如何定位并解决性能问题？</p></div>
                </div>
                <div class="candidate-line"><span>我的回答</span><p>我会先确认指标变化，再沿调用链检查日志与资源使用。</p></div>
                <div class="conversation-progress" aria-label="本场进度"><span class="is-done"></span><span class="is-current"></span><span></span><span></span><span></span><span></span></div>
              </template>

              <template v-else>
                <div class="scene-caption"><span>本场复盘</span><span class="scene-state">示例报告</span></div>
                <div class="review-summary"><strong>项目脉络清楚，技术细节还可继续展开。</strong><span>综合评估 · 示例内容</span></div>
                <div class="review-bars" aria-label="示例表现维度">
                  <div><span>项目表达</span><i><b style="--line-size: 82%"></b></i></div>
                  <div><span>知识准确</span><i><b style="--line-size: 57%"></b></i></div>
                  <div><span>回答结构</span><i><b style="--line-size: 69%"></b></i></div>
                </div>
                <div class="review-next"><el-icon aria-hidden="true"><Collection /></el-icon><span>薄弱知识点已整理到复习中心</span></div>
              </template>
            </div>
          </Transition>

          <footer class="session-window-foot">
            <span>简历</span><i></i><span>面试</span><i></i><span>报告</span><i></i><span>复习</span>
          </footer>
        </section>
      </div>
      <footer class="story-footer"><span>AI 面试助手</span><span>个人练习空间</span></footer>
    </section>

    <aside class="auth-account" aria-labelledby="auth-title">
      <a class="auth-brand" href="#login" aria-label="AI 面试助手登录">
        <span class="auth-brand-icon"><el-icon aria-hidden="true"><ChatDotRound /></el-icon></span>
        <strong>AI 面试助手</strong>
      </a>

      <div class="auth-form-wrap">
        <header class="auth-heading">
          <h2 id="auth-title">{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ activeTab === 'login' ? '登录后继续你的面试练习。' : '注册账号，保存简历与面试记录。' }}</p>
        </header>

        <el-tabs v-model="activeTab" class="auth-tabs" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              class="auth-form"
              hide-required-asterisk
              @submit.prevent="submitLogin"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username" autocomplete="username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password" type="password" show-password autocomplete="current-password" placeholder="请输入密码" />
              </el-form-item>
              <el-button class="auth-submit" type="primary" native-type="submit" :loading="loginSubmitting">登录</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              label-position="top"
              class="auth-form"
              hide-required-asterisk
              @submit.prevent="submitRegister"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username" autocomplete="username" placeholder="3-32 个字符" />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" type="password" show-password autocomplete="new-password" placeholder="6-32 个字符" />
              </el-form-item>
              <el-form-item label="昵称（可选）" prop="nickname">
                <el-input v-model="registerForm.nickname" autocomplete="nickname" placeholder="登录后显示的称呼" />
              </el-form-item>
              <el-button class="auth-submit" type="primary" native-type="submit" :loading="registerSubmitting">创建账号</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <p class="auth-demo-note">账号用于保存简历与面试记录</p>
      </div>
      <p class="auth-account-foot">安全登录 · 个人练习档案</p>
    </aside>
  </main>
</template>
