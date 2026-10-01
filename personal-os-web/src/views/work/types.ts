/**
 * 工作域本地 TS 类型与常量。
 *
 * 说明：`src/types/index.ts` 属全局装配（不在 M1 白名单），因此工作域类型就地定义于此。
 */

/** 任务状态 */
export type TaskStatus = 'todo' | 'doing' | 'done' | 'abandoned'

/** 三视图 */
export type TaskView = 'today' | 'all' | 'overdue'

/** 优先级：1 高 / 2 中 / 3 低 */
export type TaskPriority = 1 | 2 | 3

/** 任务标签精简 VO */
export interface TaskTagVO {
  id: number
  name: string
  color: string
}

/** 任务出参 */
export interface TaskVO {
  id: number
  userId: number
  title: string
  description: string | null
  status: TaskStatus
  priority: number
  planStart: string | null
  dueAt: string | null
  estimateMin: number | null
  actualMin: number | null
  doneAt: string | null
  sortNo: number | null
  overdue: boolean
  tags: TaskTagVO[]
  createTime: string | null
  updateTime: string | null
}

/** 创建入参（仅 title 必填） */
export interface TaskCreateParams {
  title: string
  description?: string
  priority?: number
  dueAt?: string
  estimateMin?: number
  tagIds?: number[]
}

/** 更新入参（全量覆盖，语义同创建） */
export type TaskUpdateParams = TaskCreateParams

/** 分页查询入参 */
export interface TaskQueryParams {
  view?: TaskView
  status?: TaskStatus
  page?: number
  size?: number
}

/** 统一分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 标签创建入参（供 WorkTagPicker 新建标签） */
export interface TagCreateParams {
  name: string
  color?: string
  scope?: string
}

/** 三视图选项 */
export const TASK_VIEWS: ReadonlyArray<{ value: TaskView; label: string }> = [
  { value: 'today', label: '今日' },
  { value: 'all', label: '全部' },
  { value: 'overdue', label: '逾期' }
]

/** 状态显示文案 */
export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  todo: '待办',
  doing: '进行中',
  done: '已完成',
  abandoned: '已放弃'
}

/** 优先级显示文案 */
export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  1: '高',
  2: '中',
  3: '低'
}

/** 三态图标点击时的流转目标：todo → doing → done → todo */
export const STATUS_CYCLE: Record<TaskStatus, TaskStatus> = {
  todo: 'doing',
  doing: 'done',
  done: 'todo',
  abandoned: 'todo'
}

/** 三态图标点击时的无障碍文案（目标状态） */
export const STATUS_ACTION_LABELS: Record<TaskStatus, string> = {
  todo: '标记为进行中',
  doing: '标记为已完成',
  done: '重新打开任务',
  abandoned: '任务已放弃'
}

/** 状态过滤选项（全部 / 待办 / 进行中 / 已完成 / 已放弃） */
export const TASK_STATUS_FILTERS: ReadonlyArray<{ value: TaskStatus | null; label: string }> = [
  { value: null, label: '全部状态' },
  { value: 'todo', label: '待办' },
  { value: 'doing', label: '进行中' },
  { value: 'done', label: '已完成' },
  { value: 'abandoned', label: '已放弃' }
]
