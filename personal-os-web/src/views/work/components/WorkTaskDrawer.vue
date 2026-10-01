<script setup lang="ts">
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessageBox } from 'element-plus'
import type { TaskUpdateParams, TaskVO } from '@/views/work/types'
import WorkTagPicker from './WorkTagPicker.vue'

/**
 * 任务编辑抽屉（玻璃厚层）。
 * 字段：标题（必填）/ 描述 / 优先级 / 截止时间 / 预估工时 / 标签。
 */

const props = defineProps<{
  modelValue: boolean
  task: TaskVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', payload: TaskUpdateParams): void
  (e: 'remove'): void
}>()

/** 表单字段 */
const title = ref('')
const description = ref('')
const priority = ref(2)
const dueAt = ref('')
const estimateMin = ref<number | null>(null)
const tagIds = ref<number[]>([])

/** 标题校验错误 */
const titleError = ref('')

/** 用当前任务初始化表单 */
watch(
  () => props.task,
  (task) => {
    if (!task) {
      return
    }
    title.value = task.title
    description.value = task.description ?? ''
    priority.value = task.priority
    dueAt.value = task.dueAt ? dayjs(task.dueAt).format('YYYY-MM-DDTHH:mm') : ''
    estimateMin.value = task.estimateMin
    tagIds.value = task.tags.map((t) => t.id)
    titleError.value = ''
  },
  { immediate: true }
)

function close(): void {
  emit('update:modelValue', false)
}

function handleSave(): void {
  const value = title.value.trim()
  if (!value) {
    titleError.value = '任务标题不能为空'
    return
  }
  titleError.value = ''
  const payload: TaskUpdateParams = { title: value, priority: priority.value }
  if (description.value) {
    payload.description = description.value
  }
  if (dueAt.value) {
    // datetime-local 控件的值是 ISO 'T' 格式，发给后端前统一转成 'yyyy-MM-dd HH:mm:ss'
    payload.dueAt = dayjs(dueAt.value).format('YYYY-MM-DD HH:mm:ss')
  }
  if (estimateMin.value !== null && estimateMin.value >= 0) {
    payload.estimateMin = estimateMin.value
  }
  payload.tagIds = [...tagIds.value]
  emit('save', payload)
}

async function handleRemove(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该任务吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  emit('remove')
}
</script>

<template>
  <Transition name="drawer">
    <div v-if="modelValue" class="drawer-overlay" @click.self="close">
      <aside class="drawer glass glass--thick" role="dialog" aria-label="编辑任务" aria-modal="true">
        <header class="drawer__head">
          <h2 class="drawer__title">编辑任务</h2>
          <button type="button" class="drawer__close" aria-label="关闭" @click="close">×</button>
        </header>

        <div class="drawer__body">
          <div class="field">
            <label class="field__label" for="drawer-title">标题</label>
            <input
              id="drawer-title"
              v-model="title"
              class="field__input glass glass--thin"
              type="text"
              :aria-invalid="!!titleError"
              @input="titleError = ''"
            />
            <p v-if="titleError" class="field__error" role="alert">{{ titleError }}</p>
          </div>

          <div class="field">
            <label class="field__label" for="drawer-desc">描述</label>
            <textarea
              id="drawer-desc"
              v-model="description"
              class="field__input field__input--area glass glass--thin"
              rows="4"
            />
          </div>

          <div class="field">
            <span class="field__label">优先级</span>
            <div class="seg glass glass--thin" role="radiogroup" aria-label="优先级">
              <button
                v-for="p in [1, 2, 3]"
                :key="p"
                type="button"
                class="seg__item"
                role="radio"
                :aria-checked="priority === p"
                @click="priority = p"
              >
                {{ p === 1 ? '高' : p === 2 ? '中' : '低' }}
              </button>
            </div>
          </div>

          <div class="field">
            <label class="field__label" for="drawer-due">截止时间</label>
            <input
              id="drawer-due"
              v-model="dueAt"
              class="field__input glass glass--thin"
              type="datetime-local"
            />
          </div>

          <div class="field">
            <label class="field__label" for="drawer-estimate">预估工时（分钟）</label>
            <input
              id="drawer-estimate"
              v-model.number="estimateMin"
              class="field__input glass glass--thin"
              type="number"
              min="0"
            />
          </div>

          <div class="field">
            <span class="field__label">标签</span>
            <WorkTagPicker v-model="tagIds" />
          </div>
        </div>

        <footer class="drawer__foot">
          <button type="button" class="drawer__btn drawer__btn--danger" @click="handleRemove">
            删除
          </button>
          <div class="drawer__foot-right">
            <button type="button" class="drawer__btn" @click="close">取消</button>
            <button type="button" class="drawer__btn drawer__btn--primary" @click="handleSave">
              保存
            </button>
          </div>
        </footer>
      </aside>
    </div>
  </Transition>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  justify-content: flex-end;
  background: rgba(0, 0, 0, 0.28);
}

.drawer {
  display: flex;
  width: min(440px, 100vw);
  height: 100%;
  flex-direction: column;
  border-radius: var(--r-xl) 0 0 var(--r-xl);
}

.drawer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px 12px;
}

.drawer__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.drawer__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  font-size: 22px;
  line-height: 1;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
}

.drawer__body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  padding: 8px 22px 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.field__input {
  height: 44px;
  padding: 0 14px;
  font-size: 14px;
  color: var(--text-primary);
  border-radius: var(--r-sm);
}

.field__input--area {
  height: auto;
  padding: 10px 14px;
  line-height: 1.5;
  resize: vertical;
}

.field__input:focus-visible {
  outline: none;
  border-color: var(--accent);
}

.field__error {
  font-size: 12px;
  color: var(--danger);
}

.seg {
  display: flex;
  padding: 3px;
  border-radius: var(--r-pill);
}

.seg__item {
  flex: 1 1 0;
  height: 34px;
  font-size: 13px;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring), background-color var(--dur) var(--spring);
}

.seg__item[aria-checked='true'] {
  color: #fff;
  background: var(--accent);
}

.drawer__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 22px 20px;
  border-top: 1px solid var(--separator);
}

.drawer__foot-right {
  display: flex;
  gap: 10px;
}

.drawer__btn {
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.drawer__btn--primary {
  font-weight: 600;
  color: #fff;
  background: var(--accent);
  border-color: transparent;
}

.drawer__btn--danger {
  color: var(--danger);
  border-color: rgba(255, 59, 48, 0.35);
}

@media (prefers-reduced-motion: no-preference) {
  .drawer-enter-active,
  .drawer-leave-active {
    transition: opacity var(--dur) var(--spring);
  }

  .drawer-enter-active .drawer,
  .drawer-leave-active .drawer {
    transition: transform var(--dur) var(--spring);
  }

  .drawer-enter-from,
  .drawer-leave-to {
    opacity: 0;
  }

  .drawer-enter-from .drawer,
  .drawer-leave-to .drawer {
    transform: translateX(24px);
  }
}

@media (max-width: 480px) {
  .drawer {
    width: 100vw;
    border-radius: 0;
  }
}
</style>
