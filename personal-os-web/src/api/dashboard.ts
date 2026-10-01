import { get } from '@/api/request'

/**
 * 首页聚合 API 客户端（P1 集成，RIce）。
 *
 * 契约来源：`docs/modules/m0-infra.md` §3 `GET /api/dashboard/home`。
 */

/** 今日信息 */
export interface DashboardToday {
  date: string
  week: string
}

/** 工作域概况（M1 接线） */
export interface DashboardWork {
  todayTotal: number
  todayDone: number
  overdue: number
}

/** 习惯条目 */
export interface DashboardHabit {
  id: number
  name: string
  done: boolean
}

/** 生活域概况（M3 接线） */
export interface DashboardLife {
  checkinDone: number
  checkinTotal: number
  habits: DashboardHabit[]
}

/** 高频 SOP 条目 */
export interface DashboardSopItem {
  id: number
  title: string
  useCount: number
}

/** SOP 域概况（M2 接线） */
export interface DashboardSop {
  top: DashboardSopItem[]
}

/** SOP 提示：重复劳动，建议沉淀成 SOP */
export interface DashboardSopHint {
  taskTitle: string
  count: number
}

/** 首页聚合数据 */
export interface DashboardData {
  today: DashboardToday
  work: DashboardWork
  life: DashboardLife
  sop: DashboardSop
  streakDays: number
  sopHints: DashboardSopHint[]
}

/** 拉取首页聚合数据 */
export function getDashboardHome(): Promise<DashboardData> {
  return get<DashboardData>('/dashboard/home')
}
