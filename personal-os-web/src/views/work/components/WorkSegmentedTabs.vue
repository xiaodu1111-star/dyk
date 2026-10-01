<script setup lang="ts">
import { computed } from 'vue'
import type { TaskView } from '@/views/work/types'
import { TASK_VIEWS } from '@/views/work/types'

/**
 * 三 Tab 玻璃胶囊分段控件（今日 / 全部 / 逾期）+ 滑动指示条。
 * role=tablist / role=tab / aria-selected，支持键盘左右切换。
 */

const props = defineProps<{
  modelValue: TaskView
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: TaskView): void
}>()

/** 当前选中下标 */
const activeIndex = computed(() => TASK_VIEWS.findIndex((v) => v.value === props.modelValue))

/** 指示条位移百分比 */
const indicatorStyle = computed(() => ({
  transform: `translateX(${Math.max(activeIndex.value, 0) * 100}%)`,
  width: `${100 / TASK_VIEWS.length}%`
}))

function select(value: TaskView): void {
  if (value !== props.modelValue) {
    emit('update:modelValue', value)
  }
}

function onKeydown(event: KeyboardEvent, index: number): void {
  const count = TASK_VIEWS.length
  let next = index
  if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
    next = (index + 1) % count
  } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
    next = (index - 1 + count) % count
  } else {
    return
  }
  event.preventDefault()
  select(TASK_VIEWS[next].value)
}
</script>

<template>
  <div class="segmented glass glass--thin" role="tablist" aria-label="任务视图">
    <span class="segmented__indicator" :style="indicatorStyle" aria-hidden="true" />
    <button
      v-for="(item, index) in TASK_VIEWS"
      :key="item.value"
      type="button"
      class="segmented__tab"
      role="tab"
      :aria-selected="modelValue === item.value"
      :tabindex="modelValue === item.value ? 0 : -1"
      @click="select(item.value)"
      @keydown="onKeydown($event, index)"
    >
      {{ item.label }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.segmented {
  position: relative;
  display: inline-flex;
  padding: 4px;
  border-radius: var(--r-pill);
}

.segmented__indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  bottom: 4px;
  z-index: 0;
  background: var(--glass-bg-thick);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-pill);
  box-shadow: var(--glass-shadow);
}

@media (prefers-reduced-motion: no-preference) {
  .segmented__indicator {
    transition: transform var(--dur) var(--spring);
  }
}

.segmented__tab {
  position: relative;
  z-index: 1;
  min-width: 72px;
  height: 34px;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring);
}

.segmented__tab[aria-selected='true'] {
  color: var(--text-primary);
}

.segmented__tab:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

@media (max-width: 480px) {
  .segmented {
    width: 100%;
  }

  .segmented__tab {
    flex: 1 1 0;
    min-width: 0;
    padding: 0 8px;
  }
}
</style>
