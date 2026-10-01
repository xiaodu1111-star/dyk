import type { AxiosRequestConfig } from 'axios'
import http, { get, post } from './request'

// =============================================================
// M2 · SOP 库 · API 客户端 + 领域类型
// 说明：
//  - request.ts 只导出了 get / post，这里用默认导出的 http 实例自行封装 put / del，
//    复用同一套拦截器（token 注入 / 401 跳登录 / 成功拆包 R）。
//  - SOP 相关 TS 类型统一在本文件导出（不写进 types/index.ts）。
// =============================================================

/** 后端统一分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** SOP 列表卡片（列表页直接露出复用数据：使用次数 / 平均耗时） */
export interface SopVO {
  id: number
  title: string
  category: string | null
  triggerScene: string | null
  goal: string | null
  version: string
  useCount: number
  avgMinutes: number
  lastUsedAt: string | null
  status: string
  /** 后端返回 Integer 0/1，使用时显式转布尔 */
  pinned: number
  stepCount: number
  createTime: string
  updateTime: string
}

/** SOP 步骤（后端 SopStepVO：无 id / sopId，步骤以 stepNo 为稳定标识） */
export interface SopStep {
  stepNo: number
  title: string
  detail: string | null
  tip: string | null
  estimateMin: number | null
}

/** 详情聚合统计 */
export interface SopStats {
  runCount: number
  avgMinutes: number
  topStuckStep: number | null
  lastUsedAt: string | null
}

/** 版本时间线摘要（后端 SopVersionVO：不含 sopId / content_json） */
export interface SopVersionSummary {
  id: number
  version: string
  changeNote: string | null
  createTime: string
}

/** SOP 详情 */
export interface SopDetailVO {
  id: number
  title: string
  category: string | null
  triggerScene: string | null
  goal: string | null
  version: string
  useCount: number
  avgMinutes: number
  lastUsedAt: string | null
  status: string
  /** 后端返回 Integer 0/1，使用时显式转布尔 */
  pinned: number
  sourceTaskId: number | null
  createTime: string
  updateTime: string
  steps: SopStep[]
  versions: SopVersionSummary[]
  stats: SopStats
}

/**
 * 版本完整快照（回看）。
 * 后端 SopVersionDetailVO 已把 contentJson 解析成结构化 `steps` 返回，
 * 前端直接消费 `steps`，不再手工 JSON.parse（快照结构由后端兜底）。
 */
export interface SopVersionDetail {
  id: number
  sopId: number
  version: string
  changeNote: string | null
  createTime: string
  title: string | null
  category: string | null
  triggerScene: string | null
  goal: string | null
  status: string | null
  steps: SopStep[]
  contentJson: string | null
}

/** 版本快照反序列化后的结构 */
export interface SopSnapshot {
  title?: string
  category?: string | null
  triggerScene?: string | null
  goal?: string | null
  version?: string
  steps?: SopSnapshotStep[]
}

/** 版本快照内的步骤 */
export interface SopSnapshotStep {
  stepNo?: number
  title?: string
  detail?: string | null
  tip?: string | null
  estimateMin?: number | null
}

/** 执行记录 */
export interface SopRunVO {
  id: number
  sopId: number
  startedAt: string
  finishedAt: string | null
  costMin: number | null
  stuckStep: number | null
  deviation: string | null
}

/** 创建 / 编辑入参中的步骤 */
export interface SopStepInput {
  title: string
  detail?: string | null
  tip?: string | null
  estimateMin?: number | null
}

/** 创建 / 编辑入参 */
export interface SopSavePayload {
  title: string
  category?: string | null
  triggerScene?: string | null
  goal?: string | null
  sourceTaskId?: number | null
  changeNote?: string | null
  steps: SopStepInput[]
}

/** 列表查询入参 */
export interface SopListQuery {
  keyword?: string
  category?: string
  page?: number
  size?: number
}

/** 结束执行入参 */
export interface SopRunFinishPayload {
  finished: boolean
  costMin?: number | null
  stuckStep?: number | null
  deviation?: string | null
}

/** PUT 封装：复用 http 实例的拦截器语义 */
function put<T>(url: string, data?: Record<string, unknown>): Promise<T> {
  const config = {} as AxiosRequestConfig
  return http.put(url, data, config) as unknown as Promise<T>
}

/** DELETE 封装：复用 http 实例的拦截器语义 */
function del<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const config = { params } as AxiosRequestConfig
  return http.delete(url, config) as unknown as Promise<T>
}

/** 列表查询（支持 keyword / category / 分页） */
export function listSops(query: SopListQuery = {}): Promise<PageResult<SopVO>> {
  return get<PageResult<SopVO>>('/sop', { ...query })
}

/** 详情（含步骤 / 版本摘要 / 统计） */
export function getSopDetail(id: number): Promise<SopDetailVO> {
  return get<SopDetailVO>(`/sop/${id}`)
}

/** 创建（可含步骤） */
export function createSop(payload: SopSavePayload): Promise<{ id: number }> {
  return post<{ id: number }>('/sop', { ...payload })
}

/** 编辑（若已有执行记录，后端自动留版本快照） */
export function updateSop(id: number, payload: SopSavePayload): Promise<void> {
  return put<void>(`/sop/${id}`, { ...payload })
}

/** 逻辑删除 */
export function deleteSop(id: number): Promise<void> {
  return del<void>(`/sop/${id}`)
}

/** 版本完整快照（回看） */
export function getSopVersion(id: number, versionId: number): Promise<SopVersionDetail> {
  return get<SopVersionDetail>(`/sop/${id}/versions/${versionId}`)
}

/** 发起执行，返回 runId */
export function startSopRun(id: number): Promise<{ runId: number }> {
  return post<{ runId: number }>(`/sop/${id}/runs`)
}

/** 结束执行（回写耗时 / 卡点 / 偏差，并刷新聚合字段） */
export function finishSopRun(
  runId: number,
  payload: SopRunFinishPayload
): Promise<void> {
  return put<void>(`/sop/runs/${runId}`, { ...payload })
}

/** 执行历史 */
export function listSopRuns(
  id: number,
  page = 1,
  size = 10
): Promise<PageResult<SopRunVO>> {
  return get<PageResult<SopRunVO>>(`/sop/${id}/runs`, { page, size })
}
