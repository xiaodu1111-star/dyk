<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { LoginParams } from '@/types'

const REMEMBER_KEY = 'personal_os_remember_username'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive<LoginParams>({ username: '', password: '' })
const remember = ref(false)
const loading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')
const usernameInvalid = ref(false)
const passwordInvalid = ref(false)

// 恢复「记住我」
const savedUsername = localStorage.getItem(REMEMBER_KEY)
if (savedUsername) {
  form.username = savedUsername
  remember.value = true
}

const passwordToggleLabel = computed(() => (showPassword.value ? '隐藏密码' : '显示密码'))

function clearError(): void {
  errorMessage.value = ''
  usernameInvalid.value = false
  passwordInvalid.value = false
}

function validate(): boolean {
  clearError()
  if (!form.username.trim()) {
    errorMessage.value = '请输入用户名'
    usernameInvalid.value = true
    return false
  }
  if (!form.password) {
    errorMessage.value = '请输入密码'
    passwordInvalid.value = true
    return false
  }
  return true
}

/** 把异常转成面向用户可读的中文提示 */
function resolveErrorMessage(err: unknown): string {
  const raw = err instanceof Error ? err.message : ''
  if (!raw || raw === 'Network Error' || /timeout/i.test(raw)) {
    return '网络异常，请稍后重试'
  }
  return raw
}

async function handleLogin(): Promise<void> {
  if (loading.value || !validate()) {
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    // silent=true：错误在本卡片内联展示，不弹全局提示
    await userStore.login({ username: form.username.trim(), password: form.password }, true)

    if (remember.value) {
      localStorage.setItem(REMEMBER_KEY, form.username.trim())
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }

    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (err) {
    errorMessage.value = resolveErrorMessage(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-card glass glass--thick">
    <div class="login-head">
      <div class="login-badge glass glass--thin" aria-hidden="true">
        <!-- 个人操作系统意象：中枢 + 四向辐条 -->
        <svg
          viewBox="0 0 24 24"
          width="26"
          height="26"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
        >
          <circle cx="12" cy="12" r="8.2" />
          <circle cx="12" cy="12" r="2.8" fill="currentColor" stroke="none" />
          <path d="M12 3.8v3.1M12 17.1v3.1M3.8 12h3.1M17.1 12h3.1" />
        </svg>
      </div>
      <h1 class="login-title">Personal OS</h1>
      <p class="login-subtitle">工作 · 学习 · 运动 · 理财 · 生活</p>
    </div>

    <Transition name="po-error">
      <div v-if="errorMessage" class="login-error" role="alert" aria-live="assertive">
        <svg
          class="login-error__icon"
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="9" />
          <path d="M12 7.6v5.2" />
          <path d="M12 16.4h.01" />
        </svg>
        <span>{{ errorMessage }}</span>
      </div>
    </Transition>

    <form class="login-form" novalidate @submit.prevent="handleLogin">
      <div class="field">
        <label class="sr-only" for="login-username">用户名</label>
        <div class="field__control glass glass--thin">
          <svg
            class="field__icon"
            viewBox="0 0 24 24"
            width="20"
            height="20"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <circle cx="12" cy="8" r="3.6" />
            <path d="M4.6 20.2a7.4 7.4 0 0 1 14.8 0" />
          </svg>
          <input
            id="login-username"
            v-model="form.username"
            class="field__input"
            type="text"
            name="username"
            placeholder="用户名"
            autocomplete="username"
            autocapitalize="off"
            spellcheck="false"
            :aria-invalid="usernameInvalid"
            @input="clearError"
          />
        </div>
      </div>

      <div class="field">
        <label class="sr-only" for="login-password">密码</label>
        <div class="field__control glass glass--thin">
          <svg
            class="field__icon"
            viewBox="0 0 24 24"
            width="20"
            height="20"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <rect x="4.8" y="10.4" width="14.4" height="9.4" rx="2.6" />
            <path d="M8.2 10.4V7.9a3.8 3.8 0 0 1 7.6 0v2.5" />
          </svg>
          <input
            id="login-password"
            v-model="form.password"
            class="field__input"
            :type="showPassword ? 'text' : 'password'"
            name="password"
            placeholder="密码"
            autocomplete="current-password"
            :aria-invalid="passwordInvalid"
            @input="clearError"
          />
          <button
            type="button"
            class="field__action"
            :aria-label="passwordToggleLabel"
            :title="passwordToggleLabel"
            :aria-pressed="showPassword"
            @click="showPassword = !showPassword"
          >
            <svg
              v-if="!showPassword"
              viewBox="0 0 24 24"
              width="20"
              height="20"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M2.8 12S6.4 6.2 12 6.2 21.2 12 21.2 12 17.6 17.8 12 17.8 2.8 12 2.8 12Z" />
              <circle cx="12" cy="12" r="2.9" />
            </svg>
            <svg
              v-else
              viewBox="0 0 24 24"
              width="20"
              height="20"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="m4 4 16 16" />
              <path d="M9.6 5.4A9.9 9.9 0 0 1 12 5.2c5.6 0 9.2 5.4 9.2 5.4a17.6 17.6 0 0 1-2.7 3.3" />
              <path d="M6.6 7.7A17.3 17.3 0 0 0 4.8 10.6s3.6 5.8 9.2 5.8a9.6 9.6 0 0 0 3.3-.6" />
            </svg>
          </button>
        </div>
      </div>

      <div class="login-remember">
        <span class="login-remember__label">记住我</span>
        <button
          type="button"
          class="switch"
          role="switch"
          aria-label="记住我"
          :aria-checked="remember"
          @click="remember = !remember"
        >
          <span class="switch__knob" />
        </button>
      </div>

      <button type="submit" class="login-submit" :disabled="loading" :aria-busy="loading">
        <span v-if="loading" class="spinner" aria-hidden="true" />
        <span>{{ loading ? '登录中…' : '登录' }}</span>
      </button>
    </form>

    <p class="login-footer">Personal OS · v0.1.0</p>
  </section>
</template>

<style scoped lang="scss">
.login-card {
  width: min(400px, calc(100vw - 32px));
  padding: 40px 32px 28px;
  border-radius: var(--r-xl);
}

@media (prefers-reduced-motion: no-preference) {
  .login-card {
    animation: po-card-enter var(--dur-slow) var(--spring) both;
  }
}

@keyframes po-card-enter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-head {
  margin-bottom: 24px;
  text-align: center;
}

.login-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 14px;
  border-radius: var(--r-pill);
  color: var(--accent);
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--text-primary);
}

.login-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

/* ---- 内联错误条 ---- */
.login-error {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--danger);
  border: 1px solid rgba(255, 59, 48, 0.28);
  border-radius: var(--r-md);
  background: rgba(255, 59, 48, 0.12);
  -webkit-backdrop-filter: blur(12px);
  backdrop-filter: blur(12px);
}

.login-error__icon {
  flex: 0 0 auto;
}

@media (prefers-reduced-motion: no-preference) {
  .po-error-enter-active,
  .po-error-leave-active {
    transition: opacity var(--dur) var(--spring), transform var(--dur) var(--spring);
  }

  .po-error-enter-from,
  .po-error-leave-to {
    opacity: 0;
    transform: translateY(-8px);
  }
}

/* ---- 表单 ---- */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field__control {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 52px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  transition: border-color var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

.field__control:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.18), var(--glass-inner);
}

.field__icon {
  flex: 0 0 auto;
  color: var(--text-tertiary);
}

.field__input {
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
  padding: 0;
  font-size: 16px;
  color: var(--text-primary);
  background: transparent;
  border: 0;
}

.field__input::placeholder {
  color: var(--text-tertiary);
}

/* 焦点环由外层 .field__control:focus-within 提供，这里避免双环 */
.field__input:focus-visible {
  outline: none;
}

.field__action {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--text-tertiary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring);
}

.field__action:hover {
  color: var(--text-secondary);
}

/* ---- 记住我：iOS 风格胶囊开关 ---- */
.login-remember {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px;
}

.login-remember__label {
  font-size: 14px;
  color: var(--text-secondary);
}

.switch {
  position: relative;
  width: 44px;
  height: 26px;
  padding: 0;
  background: var(--separator);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: background-color var(--dur) var(--spring), transform var(--dur-fast) var(--spring);
}

.switch[aria-checked='true'] {
  background: var(--accent);
  border-color: transparent;
}

.switch__knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.28);
  transition: transform var(--dur) var(--spring);
}

.switch[aria-checked='true'] .switch__knob {
  transform: translateX(18px);
}

@media (prefers-reduced-motion: no-preference) {
  .switch:active {
    transform: scale(0.97);
  }
}

/* ---- 主按钮 ---- */
.login-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 52px;
  margin-top: 4px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: #fff;
  background: linear-gradient(
    180deg,
    #3d9bff 0%,
    var(--accent) 55%,
    var(--accent-press) 100%
  );
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.42);
  transition: transform var(--dur-fast) var(--spring), box-shadow var(--dur) var(--spring),
    opacity var(--dur) var(--spring);
}

.login-submit:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

@media (prefers-reduced-motion: no-preference) {
  .login-submit:not(:disabled):active {
    transform: scale(0.97);
  }
}

/* ---- 纯 CSS spinner ---- */
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.45);
  border-top-color: #fff;
  border-radius: 50%;
}

@media (prefers-reduced-motion: no-preference) {
  .spinner {
    animation: po-spin 0.7s linear infinite;
  }
}

@keyframes po-spin {
  to {
    transform: rotate(360deg);
  }
}

.login-footer {
  margin-top: 20px;
  font-size: 12px;
  text-align: center;
  color: var(--text-tertiary);
}

@media (max-width: 480px) {
  .login-card {
    width: calc(100vw - 24px);
    padding: 28px 24px 22px;
  }

  .login-title {
    font-size: 24px;
  }
}
</style>
