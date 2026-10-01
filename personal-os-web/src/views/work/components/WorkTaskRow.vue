<script setup lang="ts">
import { computed } from 'vue'
import dayjs from 'dayjs'
import type { TaskVO } from '@/views/work/types'
import WorkStatusIcon from './WorkStatusIcon.vue'
import WorkReschedulePopover from './WorkReschedulePopover.vue'

/**
 * 任务行。
 *
 * 行首三态图标 / 逾期红徽标 / 标题 / 优先级点 / 标签 chips / 截止日。
 * 点击标题或行 → emit edit 打开抽屉；点击图标 → emit toggle 流转。
 */

const props = defineProps<{
  task: TaskVO
  /** 当前 Tab 是否为逾期视图（决定是否显示逾期徽标 + 改期按钮） */
  overdueView: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle'): void
  (e: 'edit'): void
  (e: 'reschedule', dueAt: string): void
}>()

/** 标题是否加删除线 */
const done = computed(() => props.task.status === 'done')

/** 截止日展示文本 */
const dueText = computed(() => {
  if (!props.task.dueAt) {
    return ''
  }
  const due = dayjs(props.task.dueAt)
  const today = dayjs().startOf('day')
  if (due.isBefore(today)) {
    return `${due.format('M月D日')} · 逾期`
  }
  if (due.isSame(today)) {
    return due.format('今天 HH:mm')
  }
  return due.format('M月D日 HH:mm')
})

/** 截止日是否过期（红标） */
const dueIsDanger = computed(() => props.task.overdue)

/** 优先级点的颜色类 */
const priorityClass = computed(() => `priority-dot--${props.task.priority}`)

function onEdit(): void {
  emit('edit')
}
</script>

<template>
  <div class="task-row glass glass--thin" :class="{ 'task-row--done': done }">
    <!-- 逾期徽标（仅逾期视图） -->
    <span v-if="overdueView && task.overdue" class="overdue-badge">逾期</span>

    <WorkStatusIcon :status="task.status" @toggle="emit('toggle')" />

    <button type="button" class="task-row__main" @click="onEdit">
      <span class="task-row__title" :class="{ 'task-row__title--done': done }">
        {{ task.title }}
      </span>

      <span class="task-row__meta">
        <span class="priority-dot" :class="priorityClass" :title="`优先级 ${task.priority}`" />

        <span
          v-for="tag in task.tags"
          :key="tag.id"
          class="row-tag"
          :style="{ backgroundColor: tag.color + '22', color: 'var(--text-primary)' }"
        >
          <span class="row-tag__dot" :style="{ backgroundColor: tag.color }" aria-hidden="true" />
          {{ tag.name }}
        </span>

        <span v-if="dueText" class="row-due" :class="{ 'row-due--danger': dueIsDanger }">
          {{ dueText }}
        </span>
      </span>
    </button>

    <!-- 改期（仅逾期行） -->
    <WorkReschedulePopover
      v-if="overdueView && task.overdue"
      :current-due="task.dueAt"
      @reschedule="emit('reschedule', $event)"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.task-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: var(--r-lg);
  transition: background-color var(--dur) var(--spring);
}

.task-row:hover {
  background: var(--glass-bg-thick);
}

.task-row--done {
  opacity: 0.82;
}

.overdue-badge {
  flex: 0 0 auto;
  height: 22px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 500;
  line-height: 22px;
  color: var(--danger);
  background: rgba(255, 59, 48, 0.12);
  border-radius: var(--r-pill);
}

.task-row__main {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  padding: 2px 0;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.task-row__main:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
  border-radius: var(--r-sm);
}

.task-row__title {
  overflow: hidden;
  font-size: 15px;
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-row__title--done {
  color: var(--text-tertiary);
  text-decoration: line-through;
}

.task-row__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.priority-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.priority-dot--1 {
  background: var(--danger);
}

.priority-dot--2 {
  background: #ff9f0a;
}

.priority-dot--3 {
  background: var(--text-tertiary);
}

.row-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 20px;
  padding: 0 8px;
  font-size: 12px;
  border-radius: var(--r-pill);
}

.row-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.row-due {
  font-size: 12px;
  color: var(--text-secondary);
}

.row-due--danger {
  color: var(--danger);
}

@media (max-width: 480px) {
  .task-row {
    padding: 10px 10px;
  }
}
</style>
