<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { login, register } from '@/api/auth'
import type { LoginParams, RegisterParams } from '@/types/api'

// —— 页面卡 D2-1：登录/注册合并一页两 tab（接口事实源：01 合同 §2 / 02 合同 §2）——
const router = useRouter()
const activeTab = ref<'login' | 'register'>('login')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loginSubmitting = ref(false)
const registerSubmitting = ref(false)

const loginForm = reactive<LoginParams>({ username: '', password: '' })
const registerForm = reactive<RegisterParams>({ username: '', password: '', nickname: '' })

// 校验规则镜像合同：01 §2.1（3-32 / 6-32 / nickname ≤32）、02 §2.1（仅 @NotBlank）——不发明新规则
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
  if (!valid || registerSubmitting.value) return // F7：防连点
  registerSubmitting.value = true
  try {
    await register(registerForm)
    // 拍板一：注册成功不自动登录——切登录 tab 并预填 username、清空敏感字段
    ElMessage.success('注册成功，请登录')
    loginForm.username = registerForm.username
    loginForm.password = ''
    registerForm.password = ''
    registerForm.nickname = ''
    activeTab.value = 'login'
  } catch {
    // 失败文案由拦截器统一弹出（文案出口唯一），页面只复位状态，不重复弹
  } finally {
    registerSubmitting.value = false
  }
}

async function onLogin(): Promise<void> {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid || loginSubmitting.value) return // F7：防连点
  loginSubmitting.value = true
  try {
    const vo = await login(loginForm) // token 落盘与 401 锁释放都在 auth.login 内完成
    ElMessage.success('欢迎回来，' + vo.nickname)
    router.push('/resumes') // 拍板二：登录第一站=简历列表
  } catch {
    // 同上：G/H 用例防枚举文案一致性由后端保证，前端不做任何区分
  } finally {
    loginSubmitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <template #header>
        <h2 class="auth-title">AI 面试系统</h2>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @submit.prevent
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                @keyup.enter="onLogin"
              />
            </el-form-item>
            <el-button
              type="primary"
              class="auth-submit"
              :loading="loginSubmitting"
              :disabled="!loginForm.username || !loginForm.password"
              @click="onLogin"
            >
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            @submit.prevent
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="3-32 字符" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="6-32 字符"
              />
            </el-form-item>
            <el-form-item label="昵称（可选）" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="最长 32 字符" />
            </el-form-item>
            <el-button
              type="primary"
              class="auth-submit"
              :loading="registerSubmitting"
              :disabled="!registerForm.username || !registerForm.password"
              @click="onRegister"
            >
              注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.auth-card {
  width: 380px;
}
.auth-title {
  margin: 0;
  text-align: center;
  font-size: 20px;
}
.auth-submit {
  width: 100%;
}
</style>
