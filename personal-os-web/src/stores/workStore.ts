import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  createTask as createTaskApi,
  deleteTask as deleteTaskApi,
  pageTasks,
  updateTask as updateTaskApi,
  updateTaskStatus
} from '@/api/work'
import type {
  TaskCreateParams,
  TaskStatus,
  TaskUpdateParams,
  TaskView,
  TaskVO
} from '@/views/work/types'

/**
 * 工作域任务状态。
 *
 * 状态流转 / 增删改后**统一重拉当前列表**，避免破坏三视图口径。
 */
export const useWorkTaskStore = defineStore('workTaskStore', () => {
  /** 当前 Tab */
  const view = ref<TaskView>('today')

  /** 当前页记录 */
  const records = ref<TaskVO[]>([])

  /** 总条数 */
  const total = ref<number>(0)

  /** 当前页码 */
  const page = ref<number>(1)

  /** 每页条数 */
  const size = ref<number>(20)

  /** 加载中 */
  const loading = ref<boolean>(false)

  /** 状态过滤（null = 全部） */
  const filterStatus = ref<TaskStatus | null>(null)

  /** 是否还有更多（用于「加载更多」） */
  const hasMore = ref<boolean>(false)

  /** 按当前 view + filterStatus 拉分页；reset=true 回到第 1 页并替换列表 */
  async function fetchList(reset = true): Promise<void> {
    loading.value = true
    try {
      if (reset) {
        page.value = 1
      }
      const res = await pageTasks({
        view: view.value,
        status: filterStatus.value ?? undefined,
        page: page.value,
        size: size.value
      })
      records.value = reset ? res.records : [...records.value, ...res.records]
      total.value = res.total
      page.value = res.page
      size.value = res.size
      hasMore.value = records.value.length < res.total
    } finally {
      loading.value = false
    }
  }

  /** 切换 Tab：重置到第 1 页并重拉 */
  async function setView(v: TaskView): Promise<void> {
    if (view.value === v) {
      return
    }
    view.value = v
    await fetchList(true)
  }

  /** 设置状态过滤并重拉 */
  async function setFilterStatus(status: TaskStatus | null): Promise<void> {
    filterStatus.value = status
    await fetchList(true)
  }

  /** 加载下一页（追加） */
  async function loadMore(): Promise<void> {
    if (loading.value || !hasMore.value) {
      return
    }
    page.value += 1
    await fetchList(false)
  }

  /** 新建任务后重拉 */
  async function createTask(payload: TaskCreateParams): Promise<number> {
    const id = await createTaskApi(payload)
    await fetchList(true)
    return id
  }

  /** 状态流转后重拉（保持一致的三视图口径） */
  async function changeStatus(id: number, status: TaskStatus): Promise<void> {
    await updateTaskStatus(id, status)
    await fetchList(true)
  }

  /** 更新任务后重拉 */
  async function updateTask(id: number, payload: TaskUpdateParams): Promise<void> {
    await updateTaskApi(id, payload)
    await fetchList(true)
  }

  /** 删除任务后重拉 */
  async function removeTask(id: number): Promise<void> {
    await deleteTaskApi(id)
    await fetchList(true)
  }

  return {
    view,
    records,
    total,
    page,
    size,
    loading,
    filterStatus,
    hasMore,
    fetchList,
    setView,
    setFilterStatus,
    loadMore,
    createTask,
    changeStatus,
    updateTask,
    removeTask
  }
})
