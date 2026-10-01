<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ThemeToggle from '@/components/ThemeToggle.vue'

const router = useRouter()
const userStore = useUserStore()

/**
 * 头像取字（中文习惯）：
 * - 昵称以「小/老/阿」开头且长度为 2-3 时，取第二个字（如「小杜」→「杜」）；
 * - 其余情况取首字（如「admin」→「a」）。
 */
const initial = computed(() => {
  const name = userStore.displayName
  if (!name) return '·'
  const prefixes = ['小', '老', '阿']
  if (name.length >= 2 && name.length <= 3 && prefixes.includes(name.slice(0, 1))) {
    return name.slice(1, 2)
  }
  return name.slice(0, 1)
})

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    // 用户取消
    return
  }

  await userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<template>
  <div class="layout-default">
    <div class="wallpaper" aria-hidden="true"></div>

    <!-- 悬浮玻璃导航条（iOS 26 浮动工具栏） -->
    <header class="topbar glass glass--thick">
      <div class="topbar__left">
        <span class="avatar" aria-hidden="true">{{ initial }}</span>
      </div>

      <div class="topbar__right">
        <span class="topbar__name">{{ userStore.displayName }}</span>
        <ThemeToggle />
        <button
          type="button"
          class="icon-btn glass glass--thin"
          aria-label="退出登录"
          title="退出登录"
          @click="handleLogout"
        >
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            aria-hidden="true"
            fill="none"
            stroke="currentColor"
            stroke-width="1.7"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M14.4 4.8H7.6A2.6 2.6 0 0 0 5 7.4v9.2a2.6 2.6 0 0 0 2.6 2.6h6.8" />
            <path d="M15.4 8.6 18.8 12l-3.4 3.4" />
            <path d="M18.8 12h-8.4" />
          </svg>
        </button>
      </div>
    </header>

    <main class="layout-default__content">
      <router-view />
    </main>
  </div>
</template>

<style scoped lang="scss">
.layout-default {
  position: relative;
  min-height: 100vh;
}

.topbar {
  position: sticky;
  top: 16px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 64px;
  margin: 16px;
  padding: 0 14px;
  border-radius: var(--r-lg);
}

.topbar__left,
.topbar__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  border-radius: var(--r-pill);
  background: linear-gradient(135deg, #4aa3ff 0%, var(--accent) 60%, #0062cc 100%);
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.45);
}

.topbar__name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: var(--r-pill);
  color: var(--text-primary);
  cursor: pointer;
  transition: transform var(--dur-fast) var(--spring),
    color var(--dur) var(--spring);
}

@media (prefers-reduced-motion: no-preference) {
  .icon-btn:active {
    transform: scale(0.97);
  }
}

.layout-default__content {
  position: relative;
  z-index: 1;
  padding: 8px 16px 32px;
}

@media (max-width: 480px) {
  .topbar {
    top: 12px;
    margin: 12px;
    padding: 0 10px;
  }

  .layout-default__content {
    padding: 4px 12px 24px;
  }
}
</style>
