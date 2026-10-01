<script setup lang="ts">
import { computed } from 'vue'
import { useTheme } from '@/composables/useTheme'

const { mode, cycle } = useTheme()

/** 无障碍标签：说明当前模式与点击后的结果 */
const label = computed(() => {
  switch (mode.value) {
    case 'light':
      return '当前浅色模式，点击切换为深色'
    case 'dark':
      return '当前深色模式，点击切换为跟随系统'
    default:
      return '当前跟随系统，点击切换为浅色'
  }
})
</script>

<template>
  <button
    type="button"
    class="theme-toggle glass glass--thin"
    :aria-label="label"
    :title="label"
    @click="cycle"
  >
    <!-- 跟随系统：半明半暗 -->
    <svg
      v-if="mode === 'auto'"
      viewBox="0 0 24 24"
      width="19"
      height="19"
      aria-hidden="true"
      fill="none"
      stroke="currentColor"
      stroke-width="1.7"
    >
      <circle cx="12" cy="12" r="8.2" />
      <path d="M12 3.8a8.2 8.2 0 0 1 0 16.4z" fill="currentColor" stroke="none" />
    </svg>

    <!-- 浅色：太阳 -->
    <svg
      v-else-if="mode === 'light'"
      viewBox="0 0 24 24"
      width="19"
      height="19"
      aria-hidden="true"
      fill="none"
      stroke="currentColor"
      stroke-width="1.7"
      stroke-linecap="round"
    >
      <circle cx="12" cy="12" r="4.2" />
      <path
        d="M12 2.9v2.3M12 18.8v2.3M2.9 12h2.3M18.8 12h2.3M5.6 5.6l1.6 1.6M16.8 16.8l1.6 1.6M18.4 5.6l-1.6 1.6M7.2 16.8l-1.6 1.6"
      />
    </svg>

    <!-- 深色：月亮 -->
    <svg
      v-else
      viewBox="0 0 24 24"
      width="19"
      height="19"
      aria-hidden="true"
      fill="none"
      stroke="currentColor"
      stroke-width="1.7"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <path d="M20.2 14.4A8.6 8.6 0 0 1 9.6 3.8 8.6 8.6 0 1 0 20.2 14.4Z" />
    </svg>
  </button>
</template>

<style scoped lang="scss">
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border-radius: var(--r-pill);
  /* 图标保持 --text-primary：深玻璃叠深背景时它是最清晰的一档，
     用 secondary/tertiary 反而会降低可读性（与本轮"加强可见性"目标相悖） */
  color: var(--text-primary);
  cursor: pointer;
  transition: transform var(--dur-fast) var(--spring), color var(--dur) var(--spring),
    border-color var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

/* 深色下加强边缘 rim 与顶部高光，避免按钮在深玻璃上"消失" */
html[data-theme='dark'] .theme-toggle {
  border-color: rgba(255, 255, 255, 0.24);
  box-shadow: var(--glass-shadow), var(--glass-inner),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

@media (prefers-reduced-motion: no-preference) {
  .theme-toggle:active {
    transform: scale(0.97);
  }
}
</style>
