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
  <div class="zoo">
    <!-- 海岸线：宽屏自左涌入，窄屏自上而下；陆地与水域的分界由这条 path 独家定义 -->
    <svg
      class="zoo__coast zoo__coast--wide"
      viewBox="0 0 1440 900"
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        class="zoo__land"
        d="M0 0 H520 C470 120 560 180 600 260 C650 350 640 430 620 500 C590 600 680 660 740 710 C780 745 760 850 700 900 H0 Z"
      />
      <circle class="zoo__pond" cx="500" cy="720" r="100" />
    </svg>

    <svg
      class="zoo__coast zoo__coast--narrow"
      viewBox="0 0 390 844"
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <path
        class="zoo__land"
        d="M0 0 H390 V695 C320 705 250 690 180 700 C110 710 50 700 0 692 Z"
      />
    </svg>

    <div class="zoo__grid">
      <section class="zoo__plot" aria-labelledby="zoo-title">
        <h1 id="zoo-title" class="zoo__title">
          <span class="zoo__title-latin">AI</span>
          <span class="zoo__title-cn">面试系统</span>
        </h1>
        <p class="zoo__lede">按简历出题，逐题评分，<br />练过的都留下记录。</p>

        <el-tabs v-model="activeTab" class="zoo__tabs">
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              label-position="top"
              class="zoo__form"
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
                class="zoo__submit"
                native-type="submit"
                :loading="loginSubmitting"
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
              class="zoo__form"
              hide-required-asterisk
              @submit.prevent="onRegister"
            >
              <el-form-item label="用户名" prop="username">
                <el-input
                  v-model="registerForm.username"
                  size="large"
                  autocomplete="username"
                  placeholder="3-32 字符"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  size="large"
                  show-password
                  autocomplete="new-password"
                  placeholder="6-32 字符"
                />
              </el-form-item>
              <el-form-item label="昵称（可选）" prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  size="large"
                  autocomplete="nickname"
                  placeholder="登录后用来称呼你，可留空"
                />
              </el-form-item>
              <el-button
                type="primary"
                size="large"
                class="zoo__submit"
                native-type="submit"
                :loading="registerSubmitting"
              >
                注册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </section>

      <aside class="zoo__water">
        <p class="zoo__water-note">导览图 · 园区里有这些</p>
        <ul class="zoo__legend">
          <li class="zoo__legend-row">
            <span class="zoo__chip zoo__chip--coral">简历</span>
            <span class="zoo__chip-note">填一次，之后都从它出题</span>
          </li>
          <li class="zoo__legend-row">
            <span class="zoo__chip zoo__chip--sky">面试</span>
            <span class="zoo__chip-note">按简历出题，逐题评分</span>
          </li>
        </ul>
        <p class="zoo__foot">账号用于保存简历与面试记录</p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.zoo {
  position: relative;
  min-height: 100dvh;
  overflow: hidden;
  background: var(--zoo-water);
  color: var(--zoo-ink);
  font-family: var(--ai-font-sans);
}

/* 海岸线层：陆地、水洞都由它画，页面其它部分不许再画分界 */
.zoo__coast {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  /* 钉住视口高度：随内容拉伸会让海岸线跑出视口，水域内容就会压到绿地上 */
  height: 100dvh;
  pointer-events: none;
}

.zoo__coast--narrow {
  display: none;
}

.zoo__land {
  fill: var(--zoo-land);
  stroke: var(--zoo-ink);
  stroke-width: var(--zoo-line);
  vector-effect: non-scaling-stroke;
}

.zoo__pond {
  fill: var(--zoo-water);
  stroke: var(--zoo-ink);
  stroke-width: var(--zoo-line);
  vector-effect: non-scaling-stroke;
}

.zoo__grid {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 56%) minmax(0, 44%);
  min-height: 100dvh;
}

/* 陆地一侧 */
.zoo__plot {
  align-self: start;
  width: 100%;
  max-width: 520px;
  padding: 56px 0 56px 72px;
}

.zoo__title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin: 0;
}

/* 两档字标：拉丁字头当"地名"，中文当"区位名"。显示声部用自托管的
   Zoo Display 子集（楷体，只含这几个字形），不是拿系统显示字体顶上去。 */
.zoo__title-latin {
  font-family: 'Zoo Display', var(--ai-font-sans);
  font-size: 46px;
  font-weight: 700;
  line-height: 0.95;
  letter-spacing: -0.01em;
}

.zoo__title-cn {
  font-family: 'Zoo Display', var(--ai-font-sans);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0.3em;
}

.zoo__lede {
  margin: 10px 0 0;
  max-width: 44ch;
  font-size: 15px;
  line-height: 1.6;
}

/* tab 变成两枚胶囊：选中用墨色实心，黄色只留给主操作 */
.zoo__tabs {
  margin-top: 34px;
}

.zoo__tabs :deep(.el-tabs__header) {
  margin: 0 0 22px;
}

.zoo__tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.zoo__tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.zoo__tabs :deep(.el-tabs__item) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 18px !important;
  margin-right: 8px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: transparent;
  color: var(--zoo-ink);
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
}

.zoo__tabs :deep(.el-tabs__item.is-active) {
  background: var(--zoo-ink);
  color: var(--zoo-water);
  font-weight: 600;
}

/* Element Plus 的 hover 色被上面同优先级的墨色规则吃掉，这里补回来 */
.zoo__tabs :deep(.el-tabs__item:not(.is-active):hover) {
  background: rgba(44, 46, 42, 0.1);
}

.zoo__tabs :deep(.el-tabs__item:focus-visible) {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}

.zoo__tabs :deep(.el-tabs__nav) {
  border: 0;
}

/* 表单：字段也是胶囊，2px 墨边，白底；宽度按方向契约钉在 380px */
.zoo__form {
  max-width: 380px;
}

.zoo__form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.zoo__form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  color: var(--zoo-ink);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}

.zoo__form :deep(.el-input__wrapper) {
  height: 46px;
  padding: 0 18px;
  background: #ffffff;
  border-radius: var(--zoo-cap);
  box-shadow: inset 0 0 0 2px var(--zoo-ink);
}

.zoo__form :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 3px var(--zoo-ink);
}

.zoo__form :deep(.el-input__wrapper:focus-within) {
  box-shadow: inset 0 0 0 2px var(--zoo-ink);
  outline: 2px solid var(--zoo-ink);
  outline-offset: 2px;
}

.zoo__form :deep(.el-input__inner) {
  color: var(--zoo-ink);
  font-size: 14px;
}

.zoo__form :deep(.el-input__inner::placeholder) {
  color: var(--zoo-ink-soft);
}

.zoo__form :deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: inset 0 0 0 2px var(--zoo-ink);
  outline: 2px solid var(--zoo-alert);
  outline-offset: 2px;
}

/* 错误文字坐在奶油色胶囊里：红字压在绿地上只有 3.04:1，换底才够读（奶油 4.81:1）；
   同时配一枚红点，语义不只靠颜色传达。 */
.zoo__form :deep(.el-form-item__error) {
  position: static;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 3px 10px;
  background: var(--zoo-water);
  border-radius: var(--zoo-cap);
  color: var(--zoo-alert);
  font-size: 12px;
  line-height: 1.4;
}

.zoo__form :deep(.el-form-item__error)::before {
  content: '';
  display: block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--zoo-alert);
}

.zoo__submit {
  width: 100%;
  height: 46px;
  margin-top: 6px;
  border: 1px solid var(--zoo-ink);
  border-radius: var(--zoo-cap);
  background: var(--zoo-sun);
  color: var(--zoo-ink);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  /* 钉住 Element Plus 的按钮变量：hover/active 都留在黄与墨里，不漂到浅蓝阶 */
  --el-button-bg-color: var(--zoo-sun);
  --el-button-border-color: var(--zoo-ink);
  --el-button-text-color: var(--zoo-ink);
  --el-button-hover-bg-color: var(--zoo-sun-deep);
  --el-button-hover-border-color: var(--zoo-ink);
  --el-button-hover-text-color: var(--zoo-ink);
  --el-button-active-bg-color: var(--zoo-sun-press);
  --el-button-active-border-color: var(--zoo-ink);
  --el-button-active-text-color: var(--zoo-ink);
}

.zoo__submit:hover {
  background: var(--zoo-sun-deep);
}

.zoo__submit:active {
  background: var(--zoo-sun-press);
}

.zoo__submit:focus-visible {
  outline: 2px solid var(--zoo-ink);
  outline-offset: 3px;
}

/* 水域一侧：图例胶囊 + 页脚 */
.zoo__water {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 22px;
  padding: 56px 72px 96px 32px;
  text-align: right;
}

.zoo__water-note {
  margin: 0;
  color: var(--zoo-ink-soft);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.zoo__legend {
  display: grid;
  gap: 16px;
  width: 292px;
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 两列定宽：胶囊列对齐、说明列紧随其后——整块右靠，但 chip 与 note 必须成对 */
.zoo__legend-row {
  display: grid;
  grid-template-columns: 72px 1fr;
  align-items: center;
  gap: 12px;
  justify-items: start;
}

.zoo__chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 30px;
  padding: 0;
  border-radius: var(--zoo-cap);
  color: var(--zoo-ink);
  font-size: 13px;
  font-weight: 600;
}

.zoo__chip--coral {
  background: var(--zoo-coral);
}

.zoo__chip--sky {
  background: var(--zoo-sky);
}

.zoo__chip-note {
  color: var(--zoo-ink-soft);
  font-size: 12px;
  text-align: left;
}

.zoo__foot {
  position: absolute;
  right: 72px;
  bottom: 32px;
  margin: 0;
  color: var(--zoo-ink-soft);
  font-size: 11px;
}

/* 窄屏：海岸线改为自上而下，陆地在上一半，水域与图例沉到下方 */
@media (max-width: 899px) {
  /* 矮视口下让页面可滚，而不是把水域内容裁掉 */
  .zoo {
    overflow: visible;
  }

  .zoo__coast--wide {
    display: none;
  }

  .zoo__coast--narrow {
    display: block;
  }

  .zoo__grid {
    display: flex;
    flex-direction: column;
    min-height: 100dvh;
  }

  .zoo__plot {
    flex: 0 0 auto;
    max-width: none;
    padding: 36px 24px 32px;
  }

  /* 水域内容钉在岸线以下：84.1% 是窄屏 path 陆地底边 ~710/844 的位置；
     下限 620px 保证矮视口时不压到表单。 */
  .zoo__water {
    position: absolute;
    top: max(84.1dvh, 620px);
    left: 0;
    right: 0;
    gap: 8px;
    align-items: flex-start;
    padding: 6px 24px 12px;
    text-align: left;
  }

  .zoo__legend {
    width: 100%;
    gap: 8px;
  }

  .zoo__chip {
    height: 24px;
  }

  .zoo__foot {
    position: static;
    right: auto;
    margin: 0;
    font-size: 11px;
    line-height: 16px;
  }

  .zoo__legend {
    width: 100%;
  }

  .zoo__foot {
    position: static;
    right: auto;
    margin: 4px 0 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .zoo__form :deep(.el-input__wrapper) {
    transition: none;
  }
}
</style>
