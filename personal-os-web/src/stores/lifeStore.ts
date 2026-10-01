import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  MSG_ALREADY_CHECKED,
  checkinHabit,
  createHabit as createHabitApi,
  deleteHabit as deleteHabitApi,
  getHabitStreak,
  listHabits,
  updateHabit as updateHabitApi
} from '@/api/life'
import type { HabitSaveParams, HabitType, HabitVO } from '@/api/life'

/**
 * 生活域习惯状态（Pinia id 带模块前缀，避免与其他窗口撞名）。
 *
 * 设计要点：
 * - 列表以「习惯 + 其 streak」为展示单元，故内部维护 `streaks` 映射，
 *   在拉列表后并发补齐每个习惯的连续天数（展示用，缺失时降级为 0）。
 * - 打卡 / 增删改后统一重拉列表 + streak，保证 done/value/streak 口径一致。
 */

/** 习惯 + 连续天数（页面展示单元） */
export interface HabitWithStreak {
  habit: HabitVO
  streak: number
}

/** 打卡结果（用于区分「成功」与「今日已打卡」幂等） */
export interface CheckinResult {
  /** 是否为有效打卡（false 表示今日已打卡，UI 应视作已完成） */
  checked: boolean
  /** 打卡后该习惯今日的累计值 */
  value: number
}

export const useLifeHabitStore = defineStore('lifeHabitStore', () => {
  /** 习惯列表（原始 VO） */
  const habits = ref<HabitVO[]>([])

  /** 习惯 id → 连续天数 */
  const streaks = ref<Record<number, number>>({})

  /** 列表加载中 */
  const loading = ref<boolean>(false)

  /** 正在打卡的习惯 id 集合（用于单卡 spinner 与防重复点击） */
  const checkingIds = ref<number[]>([])

  /** 列表 + streak 的展示单元 */
  const habitList = computed<HabitWithStreak[]>(() =>
    habits.value.map((habit) => ({
      habit,
      streak: streaks.value[habit.id] ?? 0
    }))
  )

  /** 今日已完成数 */
  const doneCount = computed<number>(() => habits.value.filter((h) => h.done).length)

  /** 是否正在为某习惯打卡 */
  function isChecking(id: number): boolean {
    return checkingIds.value.includes(id)
  }

  /** 拉取习惯列表 */
  async function fetchHabits(): Promise<void> {
    loading.value = true
    try {
      habits.value = await listHabits()
    } finally {
      loading.value = false
    }
  }

  /** 为已加载的习惯并发补齐 streak（静默失败，缺失降级 0） */
  async function fetchStreaks(): Promise<void> {
    const results = await Promise.all(
      habits.value.map(async (habit) => {
        try {
          const res = await getHabitStreak(habit.id)
          return { id: habit.id, streak: res.streak }
        } catch {
          return { id: habit.id, streak: 0 }
        }
      })
    )
    const next: Record<number, number> = {}
    for (const item of results) {
      next[item.id] = item.streak
    }
    streaks.value = next
  }

  /** 全量刷新：列表 + streak */
  async function refresh(): Promise<void> {
    await fetchHabits()
    await fetchStreaks()
  }

  /**
   * 打卡（打卡型 / 计数型通用）。
   * - silent=true 呼叫后端：14002「今日已打卡」不弹全局报错。
   * - 命中 14002 时归一化为「已完成」：本地把该习惯置 done 并返回 checked=false。
   * @param habit 目标习惯
   * @param increment 计数型增量（缺省 1）
   */
  async function checkin(habit: HabitVO, increment = 1): Promise<CheckinResult> {
    if (checkingIds.value.includes(habit.id)) {
      return { checked: false, value: habit.value }
    }
    checkingIds.value = [...checkingIds.value, habit.id]
    try {
      await checkinHabit(habit.id, increment, true)
      // 打卡型成功 → value=1；计数型 → 后端返回最新单条 record.value（增量），
      // 故本地累加一次并按目标判断完成态，随后 refresh 校正。
      const nextValue = habit.type === 'checkin' ? 1 : habit.value + increment
      markDone(habit.id, nextValue)
      void refresh().catch(() => undefined)
      return { checked: true, value: nextValue }
    } catch (err) {
      if (err instanceof Error && err.message === MSG_ALREADY_CHECKED) {
        // 幂等：今日已打卡 → UI 表现为「已完成」，不报错
        markDone(habit.id, habit.type === 'checkin' ? 1 : habit.value)
        return { checked: false, value: habit.value }
      }
      throw err
    } finally {
      checkingIds.value = checkingIds.value.filter((id) => id !== habit.id)
    }
  }

  /** 本地把某习惯标记为已完成（用于幂等 / 即时反馈） */
  function markDone(id: number, value: number): void {
    habits.value = habits.value.map((h) =>
      h.id === id ? { ...h, done: true, value } : h
    )
  }

  /** 新建习惯 */
  async function createHabit(payload: HabitSaveParams): Promise<HabitVO> {
    const created = await createHabitApi(payload)
    await refresh()
    return created
  }

  /** 更新习惯 */
  async function updateHabit(id: number, payload: HabitSaveParams): Promise<HabitVO> {
    const updated = await updateHabitApi(id, payload)
    await refresh()
    return updated
  }

  /** 删除习惯 */
  async function removeHabit(id: number): Promise<void> {
    await deleteHabitApi(id)
    await refresh()
  }

  return {
    habits,
    streaks,
    loading,
    checkingIds,
    habitList,
    doneCount,
    isChecking,
    fetchHabits,
    fetchStreaks,
    refresh,
    checkin,
    createHabit,
    updateHabit,
    removeHabit
  }
})

/** 便捷：类型守卫（供组件内做类型收敛用） */
export function isCountHabit(habit: HabitVO): habit is HabitVO & { type: 'count' } {
  return habit.type === 'count'
}

/** 便捷：判断类型字面量（避免组件里重复写魔法字符串） */
export function isCheckinType(type: HabitType): boolean {
  return type === 'checkin'
}
