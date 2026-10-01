<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useWorkTaskStore } from '@/stores/workStore'
import type { TaskCreateParams, TaskStatus, TaskUpdateParams, TaskVO } from '@/views/work/types'
import { TASK_STATUS_FILTERS } from '@/views/work/types'
import WorkSegmentedTabs from './components/WorkSegmentedTabs.vue'
import WorkQuickAdd from './components/WorkQuickAdd.vue'
import WorkTaskRow from './components/WorkTaskRow.vue'
import WorkTaskDrawer from './components/WorkTaskDrawer.vue'

/**
 * 工作域页面壳：三 Tab 分段控件 + 快速添加 + 任务列表 + 编辑抽屉。
 */

const store = useWorkTaskStore()

/** 编辑抽屉开合 */
const drawerOpen = ref(false)

/** 当前编辑任务 */
const currentTask = ref<TaskVO | null>(null)

/** 空态文案（按视图区分） */
const emptyText = computed(() => {
  if (store.view === 'overdue') {
    return '没有逾期任务，保持得不错'
  }
  if (store.view === 'today') {
    return '今日没有待办，添加一条开始吧'
  }
  return '还没有任务，在上方输入框回车即可创建'
})

onMounted(() => {
  store.fetchList(true).catch(() => undefined)
})

/** 切换 Tab */
function handleViewChange(value: typeof store.view): void {
  store.setView(value).catch(() => undefined)
}

/** 状态过滤 */
function handleFilterChange(event: Event): void {
  const value = (event.target as HTMLSelectElement).value
  const status = value === '' ? null : (value as TaskStatus)
  store.setFilterStatus(status).catch(() => undefined)
}

/** 快速添加 */
function handleCreate(payload: TaskCreateParams): void {
  store
    .createTask(payload)
    .then(() => ElMessage.success('已添加'))
    .catch(() => undefined)
}

/** 三态图标点击流转 */
async function handleToggle(task: TaskVO): Promise<void> {
  const target: TaskStatus =
    task.status === 'todo' ? 'doing' : task.status === 'doing' ? 'done' : 'todo'

  if (task.status === 'done') {
    try {
      await ElMessageBox.confirm('该任务已完成，确定重新打开吗？', '提示', {
        confirmButtonText: '重新打开',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }

  await store.changeStatus(task.id, target).catch(() => undefined)
}

/** 打开编辑抽屉 */
function handleEdit(task: TaskVO): void {
  currentTask.value = task
  drawerOpen.value = true
}

/** 保存编辑 */
function handleSave(payload: TaskUpdateParams): void {
  if (!currentTask.value) {
    return
  }
  store
    .updateTask(currentTask.value.id, payload)
    .then(() => {
      drawerOpen.value = false
      ElMessage.success('已保存')
    })
    .catch(() => undefined)
}

/** 删除（抽屉内二次确认后触发） */
function handleRemove(): void {
  if (!currentTask.value) {
    return
  }
  store
    .removeTask(currentTask.value.id)
    .then(() => {
      drawerOpen.value = false
      ElMessage.success('已删除')
    })
    .catch(() => undefined)
}

/** 逾期行改期：组装全量 payload（PUT 为全量覆盖） */
function handleReschedule(task: TaskVO, dueAt: string): void {
  // 改期弹层传入的是 datetime-local 的 ISO 'T' 格式，发给后端前统一转成 'yyyy-MM-dd HH:mm:ss'
  const normalizedDueAt = dayjs(dueAt).format('YYYY-MM-DD HH:mm:ss')
  const payload: TaskUpdateParams = {
    title: task.title,
    priority: task.priority,
    dueAt: normalizedDueAt,
    tagIds: task.tags.map((t) => t.id)
  }
  if (task.description) {
    payload.description = task.description
  }
  if (task.estimateMin !== null) {
    payload.estimateMin = task.estimateMin
  }
  store
    .updateTask(task.id, payload)
    .then(() => ElMessage.success('已改期'))
    .catch(() => undefined)
}
</script>

<template>
  <div class="work-page">
    <div class="work-page__inner">
      <header class="work-head">
        <h1 class="work-head__title">工作</h1>
        <div class="work-head__tools">
          <label class="sr-only" for="work-filter">状态过滤</label>
          <select
            id="work-filter"
            class="work-filter glass glass--thin"
            :value="store.filterStatus ?? ''"
            @change="handleFilterChange"
          >
            <option v-for="f in TASK_STATUS_FILTERS" :key="String(f.value)" :value="f.value ?? ''">
              {{ f.label }}
            </option>
          </select>
        </div>
      </header>

      <WorkSegmentedTabs :model-value="store.view" @update:model-value="handleViewChange" />

      <WorkQuickAdd @submit="handleCreate" />

      <section class="task-list" aria-live="polite">
        <!-- 加载态 -->
        <div v-if="store.loading && !store.records.length" class="task-list__state">
          <span class="spinner" aria-hidden="true" />
          <span>加载中…</span>
        </div>

        <!-- 空态 -->
        <p v-else-if="!store.records.length" class="task-list__empty">{{ emptyText }}</p>

        <!-- 列表 -->
        <template v-else>
          <WorkTaskRow
            v-for="task in store.records"
            :key="task.id"
            :task="task"
            :overdue-view="store.view === 'overdue'"
            @toggle="handleToggle(task)"
            @edit="handleEdit(task)"
            @reschedule="handleReschedule(task, $event)"
          />

          <button
            v-if="store.hasMore"
            type="button"
            class="task-list__more glass glass--thin"
            :disabled="store.loading"
            @click="store.loadMore()"
          >
            {{ store.loading ? '加载中…' : '加载更多' }}
          </button>
        </template>
      </section>
    </div>

    <WorkTaskDrawer
      v-model="drawerOpen"
      :task="currentTask"
      @save="handleSave"
      @remove="handleRemove"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

.work-page {
  display: flex;
  justify-content: center;
  padding-bottom: 40px;
}

.work-page__inner {
  display: flex;
  width: 100%;
  max-width: 720px;
  flex-direction: column;
  gap: 16px;
  margin-top: 4vh;
}

.work-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px;
}

.work-head__title {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--text-primary);
}

.work-filter {
  height: 36px;
  padding: 0 14px;
  font-size: 13px;
  color: var(--text-primary);
  border-radius: var(--r-pill);
  cursor: pointer;
}

.work-filter:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-list__state,
.task-list__empty {
  padding: 32px 0;
  font-size: 14px;
  text-align: center;
  color: var(--text-secondary);
}

.task-list__state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--separator);
  border-top-color: var(--accent);
  border-radius: 50%;
}

@media (prefers-reduced-motion: no-preference) {
  .spinner {
    animation: po-work-spin 0.7s linear infinite;
  }
}

@keyframes po-work-spin {
  to {
    transform: rotate(360deg);
  }
}

.task-list__more {
  align-self: center;
  height: 40px;
  margin-top: 6px;
  padding: 0 24px;
  font-size: 14px;
  color: var(--accent);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.task-list__more:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

@media (max-width: 480px) {
  .work-page__inner {
    margin-top: 2vh;
    gap: 12px;
  }

  .work-head__title {
    font-size: 22px;
  }
}
</style>
