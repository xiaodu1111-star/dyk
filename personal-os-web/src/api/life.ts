import http, { get, post } from '@/api/request'

/**
 * 生活域（习惯打卡 + 快捷记录）API 客户端。
 *
 * 说明：`@/api/request` 只导出了 `get` / `post` 与默认 axios 实例 `http`，
 * 未导出 `put` / `del`，故 PUT / DELETE 直接复用默认实例 `http`
 * （拦截器已挂在实例上，返回的同样是业务 data，无需再解一层 `res.data`）。
 *
 * 契约来源：`docs/modules/m3-life-domain.md` §4（后端已完成并冻结）。
 */

/** 习惯类型：打卡型 / 计数型 */
export type HabitType = 'checkin' | 'count'

/** 习惯 VO（后端 HabitVO） */
export interface HabitVO {
  /** 习惯 id（= 指标 def id） */
  id: number
  /** 指标 code（后端自动生成） */
  code: string
  /** 习惯名称 */
  name: string
  /** 类型：打卡型 checkin / 计数型 count */
  type: HabitType
  /** 计数型单位，打卡型为 null */
  unit: string | null
  /** 目标值，BigDecimal 序列化为 number，打卡型为 1 */
  targetValue: number | null
  /** 今日是否已完成 */
  done: boolean
  /** 今日累计值：打卡型 0/1；计数型当日 SUM */
  value: number
  /** 归属日 'yyyy-MM-dd' */
  date: string
}

/** 连续打卡天数 VO（后端 StreakVO） */
export interface StreakVO {
  /** 习惯 id */
  habitId: number
  /** 习惯名称 */
  name: string
  /** 连续天数 */
  streak: number
}

/** 快捷记录解析结果 VO（后端 QuickRecordVO，只解析不落库） */
export interface QuickRecordVO {
  /** 命中的指标 id */
  metricId: number
  /** 命中的指标名 */
  metricName: string
  /** 解析出的数值 */
  value: number
  /** 归属日 'yyyy-MM-dd' */
  date: string
}

/** 指标记录 VO（后端 RecordVO） */
export interface RecordVO {
  id: number
  metricId: number
  /** 指标名 */
  metricName: string
  /** 归属日 'yyyy-MM-dd' */
  date: string
  /** 记录时刻 */
  recordTime: string
  /** 记录值 */
  value: number
}

/** 新建 / 更新习惯入参 */
export interface HabitSaveParams {
  name: string
  type: HabitType
  /** 计数型单位（可选） */
  unit?: string
  /** 目标值（计数型；打卡型后端固定 1） */
  targetValue?: number
}

/** 快捷记录解析入参 */
export interface QuickRecordParams {
  text: string
}

/** 记录落库入参 */
export interface RecordSaveParams {
  metricId: number
  value?: number
  date?: string
}

/** 生活域后端错误码（与后端 LifeErrorCode 对齐） */
export const LIFE_ERROR = {
  /** 习惯不存在 */
  HABIT_NOT_FOUND: 14001,
  /** 今日已打卡 */
  ALREADY_CHECKED: 14002,
  /** 快捷记录解析失败 */
  PARSE_FAILED: 14003,
  /** 参数非法 */
  PARAM_INVALID: 14004
} as const

/**
 * 「今日已打卡」后端提示文案（14002）。
 * 用于 silent 呼叫下在 catch 中做文本判定，把预期的业务幂等
 * 转成 UI 上的「已完成」状态而非报错弹窗。
 */
export const MSG_ALREADY_CHECKED = '今日已打卡，无需重复'

/**
 * 「快捷记录解析失败」后端提示文案（14003）。
 * 用于 silent 呼叫下在 catch 中做文本判定，转成输入框下方的友好内联提示。
 */
export const MSG_PARSE_FAILED = '未能识别习惯，请换种说法，如「跑步 5」'

/** 新建习惯 */
export function createHabit(data: HabitSaveParams): Promise<HabitVO> {
  return post<HabitVO>('/life/habits', { ...data })
}

/** 习惯列表（含今日完成状态） */
export function listHabits(): Promise<HabitVO[]> {
  return get<HabitVO[]>('/life/habits')
}

/** 更新习惯（改名 / 目标 / 单位 / 类型） */
export function updateHabit(id: number, data: HabitSaveParams): Promise<HabitVO> {
  return http.put(`/life/habits/${id}`, { ...data }) as unknown as Promise<HabitVO>
}

/** 删除习惯（逻辑删） */
export function deleteHabit(id: number): Promise<void> {
  return http.delete(`/life/habits/${id}`) as unknown as Promise<void>
}

/**
 * 打卡。
 * @param id 习惯 id
 * @param value 计数型增量（可选，缺省后端按 1 计）
 * @param silent 为 true 时不弹全局 ElMessage（用于把 14002 幂等转成「已完成」）
 */
export function checkinHabit(
  id: number,
  value?: number,
  silent = false
): Promise<RecordVO> {
  const body: Record<string, unknown> = {}
  if (typeof value === 'number') {
    body.value = value
  }
  return post<RecordVO>(`/life/habits/${id}/checkin`, body, { silent })
}

/** 查询习惯连续天数 */
export function getHabitStreak(id: number): Promise<StreakVO> {
  return get<StreakVO>(`/life/habits/${id}/streak`)
}

/**
 * 快捷记录解析（只解析不落库）。
 * @param text 自然语言片段，如「跑步 5」
 * @param silent 为 true 时由调用方内联展示解析失败，不弹全局提示
 */
export function quickRecord(text: string, silent = true): Promise<QuickRecordVO> {
  return post<QuickRecordVO>('/life/quick-record', { text }, { silent })
}

/** 记录落库（快捷记录确认 / 手动统一入口） */
export function saveRecord(data: RecordSaveParams): Promise<RecordVO> {
  return post<RecordVO>('/life/records', { ...data })
}
