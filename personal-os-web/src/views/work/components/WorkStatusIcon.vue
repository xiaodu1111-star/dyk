<script setup lang="ts">
import { computed } from 'vue'
import type { TaskStatus } from '@/views/work/types'
import { STATUS_ACTION_LABELS } from '@/views/work/types'

/**
 * 任务三态图标（行首）。
 *
 * todo 空心圆 / doing 半填充圆 / done 实心圆 + 白对勾 / abandoned 灰化虚线圆 + 斜线。
 * 点击即流转：todo → doing → done → todo（abandoned 不流转）。
 */

const props = defineProps<{
  status: TaskStatus
}>()

const emit = defineEmits<{
  (e: 'toggle'): void
}>()

/** 无障碍文案（目标状态） */
const label = computed(() => STATUS_ACTION_LABELS[props.status])

/** abandoned 不可点 */
const disabled = computed(() => props.status === 'abandoned')

function handleClick(): void {
  if (disabled.value) {
    return
  }
  emit('toggle')
}
</script>

<template>
  <button
    type="button"
    class="status-icon"
    :class="`status-icon--${status}`"
    :aria-label="label"
    :aria-disabled="disabled"
    :disabled="disabled"
    @click.stop="handleClick"
  >
    <!-- todo：空心圆 -->
    <svg v-if="status === 'todo'" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <circle cx="12" cy="12" r="9.2" fill="none" stroke="currentColor" stroke-width="1.6" />
    </svg>

    <!-- doing：半填充圆（左半实心 + 进度感描边） -->
    <svg v-else-if="status === 'doing'" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <path
        d="M12 2.8 A9.2 9.2 0 0 0 12 21.2 Z"
        fill="currentColor"
        opacity="0.9"
      />
      <circle cx="12" cy="12" r="9.2" fill="none" stroke="currentColor" stroke-width="1.6" />
    </svg>

    <!-- done：实心圆 + 白对勾 -->
    <svg v-else-if="status === 'done'" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <circle cx="12" cy="12" r="9.6" fill="currentColor" />
      <circle cx="12" cy="12" r="9.6" fill="none" stroke="rgba(255,255,255,.35)" stroke-width="1" />
      <path
        d="M7.8 12.4l2.8 2.8 5.6-5.8"
        fill="none"
        stroke="#fff"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>

    <!-- abandoned：灰化虚线圆 + 斜线 -->
    <svg v-else viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
      <circle
        cx="12"
        cy="12"
        r="9.2"
        fill="none"
        stroke="currentColor"
        stroke-width="1.4"
        stroke-dasharray="3 3"
      />
      <path d="M6.4 17.6 17.6 6.4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
    </svg>
  </button>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.status-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  background: transparent;
  border: 0;
  color: var(--text-tertiary);
  cursor: pointer;
  @include pressable;
}

.status-icon:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
  border-radius: var(--r-pill);
}

.status-icon--todo {
  color: var(--separator);
}

.status-icon--doing {
  color: var(--accent);
}

.status-icon--done {
  color: var(--success);
}

.status-icon--abandoned {
  color: var(--text-tertiary);
  cursor: not-allowed;
}
</style>
