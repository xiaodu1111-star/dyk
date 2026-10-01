<script setup lang="ts">
/**
 * 生活域 · 连续天数角标（自绘 SVG 火焰 + 数字，严禁 emoji）。
 *
 * - 火焰用 currentColor 上色：未点亮取 `--text-tertiary`，点亮取暖色 `--streak-flame`。
 * - 纯装饰图形 `aria-hidden`，数字通过 `aria-label` 对外播报。
 */
const props = withDefaults(
  defineProps<{
    /** 连续天数 */
    streak: number
    /** 是否点亮（打卡后变暖色） */
    active?: boolean
  }>(),
  {
    active: false
  }
)

/** 对外无障碍文案 */
const label = `连续 ${props.streak} 天`
</script>

<template>
  <span
    class="life-flame"
    :class="{ 'life-flame--active': props.active && props.streak > 0 }"
    role="img"
    :aria-label="label"
    :title="label"
  >
    <svg
      class="life-flame__icon"
      viewBox="0 0 24 24"
      width="16"
      height="16"
      aria-hidden="true"
      focusable="false"
    >
      <!-- 外焰 -->
      <path
        d="M12 2.6c.9 2.6-.6 4.1-1.9 5.5-1.5 1.6-3.1 3.2-3.1 6.1A6.9 6.9 0 0 0 12 21.4a6.9 6.9 0 0 0 6.9-7.2c0-3.4-1.9-5.2-3.3-6.9-.9-1.1-1.5-2-1.6-3.3-1.1.6-1.6 1.6-1.4 2.8-1.2-.9-1.5-2.6-.6-4.2Z"
        fill="currentColor"
        opacity="0.9"
      />
      <!-- 内焰（提亮） -->
      <path
        d="M12 11.4c.5 1.3-.4 2.1-.9 2.9-.5.8-.9 1.6-.9 2.6 0 1.9 1.4 3.2 3.2 3.2 1.7 0 3-1.4 3-3.3 0-1.6-.9-2.4-1.6-3.2-.6-.7-.9-1.3-1-2.2-.6.4-.8 1-.7 1.7-.7-.6-.9-1.4-.3-2.4Z"
        fill="var(--life-flame-core)"
        opacity="0.85"
      />
    </svg>
    <span class="life-flame__num">{{ props.streak }}</span>
  </span>
</template>

<style scoped lang="scss">
.life-flame {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  height: 24px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--text-tertiary);
  border-radius: var(--r-pill);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  transition: color var(--dur) var(--spring), border-color var(--dur) var(--spring),
    background-color var(--dur) var(--spring);
}

.life-flame__icon {
  flex: 0 0 auto;
}

.life-flame__num {
  line-height: 1;
}

/* 点亮：暖色火焰 + 微弱暖底 */
.life-flame--active {
  color: var(--life-flame);
  border-color: rgba(255, 149, 0, 0.4);
  background: rgba(255, 149, 0, 0.14);
}
</style>
