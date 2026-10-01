import { computed, ref } from 'vue'

/** 主题模式：跟随系统 / 强制浅色 / 强制深色 */
export type ThemeMode = 'auto' | 'light' | 'dark'

/** 实际生效主题 */
export type EffectiveTheme = 'light' | 'dark'

/** localStorage 存储键（与 index.html 首屏内联脚本保持一致） */
export const THEME_STORAGE_KEY = 'theme'

// ---- 模块级单例状态 ----
const mode = ref<ThemeMode>('auto')
const systemPrefersDark = ref(false)

let mediaQuery: MediaQueryList | null = null
let initialized = false

/** 计算当前实际生效的主题。 */
function resolveEffective(): EffectiveTheme {
  if (mode.value === 'auto') {
    return systemPrefersDark.value ? 'dark' : 'light'
  }
  return mode.value
}

/** 把生效主题写到 <html data-theme>。 */
function applyToDocument(): void {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.setAttribute('data-theme', resolveEffective())
}

/** 读取本地保存的模式。 */
function readStoredMode(): ThemeMode {
  try {
    const saved = localStorage.getItem(THEME_STORAGE_KEY)
    if (saved === 'light' || saved === 'dark' || saved === 'auto') {
      return saved
    }
  } catch {
    // 隐私模式等场景下 localStorage 可能抛错，忽略
  }
  return 'auto'
}

/**
 * 初始化主题：同步读取 localStorage 与系统偏好并立即写入 data-theme。
 * 必须在 app.mount 之前调用，避免闪白。
 */
export function initTheme(): void {
  if (initialized) {
    return
  }
  initialized = true

  mode.value = readStoredMode()

  if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    systemPrefersDark.value = mediaQuery.matches
    // 系统主题变化时，仅 auto 模式需要跟随
    mediaQuery.addEventListener('change', (event: MediaQueryListEvent) => {
      systemPrefersDark.value = event.matches
      if (mode.value === 'auto') {
        applyToDocument()
      }
    })
  } else {
    systemPrefersDark.value = false
  }

  applyToDocument()
}

/** 主题状态与切换能力（供主题按钮使用）。 */
export function useTheme() {
  const effective = computed<EffectiveTheme>(() => resolveEffective())

  function setMode(next: ThemeMode): void {
    mode.value = next
    try {
      localStorage.setItem(THEME_STORAGE_KEY, next)
    } catch {
      // 忽略写入失败
    }
    applyToDocument()
  }

  /** 循环：auto → light → dark → auto */
  function cycle(): void {
    const order: ThemeMode[] = ['auto', 'light', 'dark']
    const next = order[(order.indexOf(mode.value) + 1) % order.length]
    setMode(next)
  }

  return { mode, effective, setMode, cycle }
}
