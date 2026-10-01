<script setup lang="ts">
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import LifeHabitForm from './LifeHabitForm.vue'
import { isCountHabit } from '@/stores/lifeStore'
import type { HabitSaveParams, HabitVO } from '@/api/life'

/**
 * 生活域 · 习惯管理面板（增删改）。
 *
 * - 顶部为新建表单（极简）；
 * - 下方为习惯清单，每行可「编辑」（就地展开表单）或「删除」（二次确认）；
 * - 删除使用 Element Plus 的 ElMessageBox 确认（已玻璃化）。
 */
const props = defineProps<{
  /** 习惯列表 */
  habits: HabitVO[]
  /** 保存中（新建或编辑提交中） */
  saving: boolean
}>()

const emit = defineEmits<{
  /** 新建 */
  (e: 'create', payload: HabitSaveParams): void
  /** 编辑保存 */
  (e: 'update', id: number, payload: HabitSaveParams): void
  /** 删除 */
  (e: 'remove', id: number): void
}>()

/** 正在编辑的习惯（null = 无） */
const editing = ref<HabitVO | null>(null)

/** 开始编辑 */
function startEdit(habit: HabitVO): void {
  editing.value = habit
}

/** 取消编辑 */
function cancelEdit(): void {
  editing.value = null
}

/** 新建提交 */
function handleCreate(payload: HabitSaveParams): void {
  emit('create', payload)
}

/** 编辑提交 */
function handleUpdate(payload: HabitSaveParams): void {
  if (!editing.value) {
    return
  }
  emit('update', editing.value.id, payload)
  editing.value = null
}

/** 删除（二次确认） */
async function handleRemove(habit: HabitVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除习惯「${habit.name}」吗？`, '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  if (editing.value?.id === habit.id) {
    editing.value = null
  }
  emit('remove', habit.id)
}

/** 类型文案 */
function typeLabel(habit: HabitVO): string {
  return isCountHabit(habit) ? '计数型' : '打卡型'
}

/** 目标文案 */
function targetLabel(habit: HabitVO): string {
  if (!isCountHabit(habit)) {
    return '每日打卡'
  }
  const target = habit.targetValue ?? 1
  const unit = habit.unit ?? ''
  return `目标 ${target}${unit}`
}
</script>

<template>
  <section class="habit-manage" aria-labelledby="life-manage-title">
    <h2 id="life-manage-title" class="habit-manage__title">新建习惯</h2>
    <LifeHabitForm :habit="null" :saving="props.saving" @submit="handleCreate" />

    <h2 class="habit-manage__title habit-manage__title--gap">我的习惯</h2>

    <p v-if="!props.habits.length" class="habit-manage__empty">
      还没有习惯，先在上方新建一个吧
    </p>

    <ul v-else class="habit-manage__list">
      <li v-for="habit in props.habits" :key="habit.id" class="habit-row glass glass--thin">
        <template v-if="editing?.id === habit.id">
          <LifeHabitForm
            :habit="habit"
            :saving="props.saving"
            @submit="handleUpdate"
            @cancel="cancelEdit"
          />
        </template>

        <template v-else>
          <div class="habit-row__info">
            <span class="habit-row__name">{{ habit.name }}</span>
            <span class="habit-row__meta">
              {{ typeLabel(habit) }} · {{ targetLabel(habit) }}
            </span>
          </div>

          <div class="habit-row__actions">
            <button
              type="button"
              class="habit-row__btn"
              :aria-label="`编辑 ${habit.name}`"
              @click="startEdit(habit)"
            >
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M4 20h4L18.4 9.6a2 2 0 0 0 0-2.8l-1.2-1.2a2 2 0 0 0-2.8 0L4 16v4Z" />
                <path d="M13.6 6.4 17.6 10.4" />
              </svg>
            </button>
            <button
              type="button"
              class="habit-row__btn habit-row__btn--danger"
              :aria-label="`删除 ${habit.name}`"
              @click="handleRemove(habit)"
            >
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M5 7h14M10 7V5.4A1.4 1.4 0 0 1 11.4 4h1.2A1.4 1.4 0 0 1 14 5.4V7" />
                <path d="M6.6 7 7.4 19a1.6 1.6 0 0 0 1.6 1.5h6a1.6 1.6 0 0 0 1.6-1.5L17.4 7" />
              </svg>
            </button>
          </div>
        </template>
      </li>
    </ul>
  </section>
</template>

<style scoped lang="scss">
.habit-manage {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.habit-manage__title {
  padding: 0 4px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: var(--text-secondary);
}

.habit-manage__title--gap {
  margin-top: 10px;
}

.habit-manage__empty {
  padding: 24px 0;
  font-size: 14px;
  text-align: center;
  color: var(--text-secondary);
}

.habit-manage__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.habit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--r-md);
}

/* 编辑态：表单占满整行 */
.habit-row:has(.habit-form) {
  display: block;
  padding: 12px;
}

.habit-row__info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.habit-row__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}

.habit-row__meta {
  font-size: 12px;
  color: var(--text-tertiary);
}

.habit-row__actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.habit-row__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring), transform var(--dur-fast) var(--spring);
}

.habit-row__btn--danger {
  color: var(--danger);
}

@media (prefers-reduced-motion: no-preference) {
  .habit-row__btn:active {
    transform: scale(0.94);
  }
}
</style>
