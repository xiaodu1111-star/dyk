import http, { get, post } from '@/api/request'
import type {
  PageResult,
  TaskCreateParams,
  TaskQueryParams,
  TaskStatus,
  TaskUpdateParams,
  TaskVO
} from '@/views/work/types'

/**
 * 工作域任务 API 客户端。
 *
 * 说明：`@/api/request` 只导出了 `get` / `post` 与默认 axios 实例 `http`，
 * 未导出 `put` / `delete`，故 PUT / DELETE 直接复用默认实例 `http`
 * （拦截器已挂在实例上，返回的同样是业务 data）。
 */

/** 新建任务，返回新任务 id */
export function createTask(data: TaskCreateParams): Promise<number> {
  return post<number>('/work/tasks', { ...data })
}

/** 分页查询任务（三视图 + 可选状态过滤） */
export function pageTasks(params: TaskQueryParams): Promise<PageResult<TaskVO>> {
  return get<PageResult<TaskVO>>('/work/tasks', { ...params })
}

/** 任务详情 */
export function getTask(id: number): Promise<TaskVO> {
  return get<TaskVO>(`/work/tasks/${id}`)
}

/** 全量更新任务 */
export function updateTask(id: number, data: TaskUpdateParams): Promise<void> {
  return http.put(`/work/tasks/${id}`, { ...data }) as unknown as Promise<void>
}

/** 状态流转 */
export function updateTaskStatus(id: number, status: TaskStatus): Promise<void> {
  return http.put(`/work/tasks/${id}/status`, { status }) as unknown as Promise<void>
}

/** 删除任务（逻辑删） */
export function deleteTask(id: number): Promise<void> {
  return http.delete(`/work/tasks/${id}`) as unknown as Promise<void>
}
