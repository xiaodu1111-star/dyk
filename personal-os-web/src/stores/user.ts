import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getMe, login as loginApi, logout as logoutApi } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'
import type { LoginParams, UserInfo } from '@/types'

/**
 * 用户状态：token 持久化到 localStorage，用户信息缓存在内存。
 */
export const useUserStore = defineStore('user', () => {
  /** 访问令牌，初始从 localStorage 恢复 */
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')

  /** 当前用户信息 */
  const userInfo = ref<UserInfo | null>(null)

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)

  /** 昵称（兜底显示用户名） */
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.username || ''
  )

  function setToken(value: string): void {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  function clearToken(): void {
    token.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  /**
   * 登录：拿到 token 后立即拉取当前用户信息。
   * @param params 用户名 / 密码
   * @param silent 为 true 时不弹全局提示（登录页用内联错误条）
   */
  async function login(params: LoginParams, silent = false): Promise<void> {
    const res = await loginApi(params, silent)
    setToken(res.token)
    await fetchMe()
  }

  /** 拉取当前用户信息 */
  async function fetchMe(): Promise<UserInfo | null> {
    const info = await getMe()
    userInfo.value = info
    return info
  }

  /** 登出：先通知后端，再清本地状态（后端失败不阻断前端） */
  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 忽略：本地清理优先
    } finally {
      clearToken()
      userInfo.value = null
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    displayName,
    setToken,
    clearToken,
    login,
    fetchMe,
    logout
  }
})
