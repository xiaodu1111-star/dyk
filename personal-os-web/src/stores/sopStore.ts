import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  createSop,
  deleteSop,
  getSopDetail,
  listSopRuns,
  listSops,
  startSopRun,
  updateSop,
  finishSopRun
} from '@/api/sop'
import type {
  SopDetailVO,
  SopListQuery,
  SopRunFinishPayload,
  SopRunVO,
  SopSavePayload,
  SopVO
} from '@/api/sop'

/**
 * SOP 库状态：列表 + 当前详情缓存，避免视图切换时重复请求。
 * store id 带模块前缀（sopLibrary），与其它模块互不冲突。
 */
export const useSopStore = defineStore('sopLibrary', () => {
  // ---- 列表状态 ----
  const list = ref<SopVO[]>([])
  const total = ref(0)
  const page = ref(1)
  const size = ref(10)
  const keyword = ref('')
  const category = ref('')
  const listLoading = ref(false)

  // ---- 详情状态 ----
  const detail = ref<SopDetailVO | null>(null)
  const detailLoading = ref(false)

  // ---- 执行历史 ----
  const runs = ref<SopRunVO[]>([])
  const runsTotal = ref(0)
  const runsLoading = ref(false)

  const isEmpty = computed(() => !listLoading.value && list.value.length === 0)

  /** 从当前列表推断分类选项（去重，忽略空值） */
  const categories = computed(() => {
    const set = new Set<string>()
    list.value.forEach((item) => {
      if (item.category) {
        set.add(item.category)
      }
    })
    return Array.from(set)
  })

  /** 拉取列表（默认沿用当前筛选条件） */
  async function fetchList(overrides: SopListQuery = {}): Promise<void> {
    listLoading.value = true
    const query: SopListQuery = {
      keyword: overrides.keyword ?? keyword.value,
      category: overrides.category ?? category.value,
      page: overrides.page ?? page.value,
      size: overrides.size ?? size.value
    }
    try {
      const res = await listSops(query)
      list.value = res.records ?? []
      total.value = res.total ?? 0
      page.value = res.page ?? query.page ?? 1
      size.value = res.size ?? query.size ?? 10
      keyword.value = query.keyword ?? ''
      category.value = query.category ?? ''
    } finally {
      listLoading.value = false
    }
  }

  /** 拉取详情；force=true 时忽略缓存 */
  async function fetchDetail(id: number, force = false): Promise<SopDetailVO | null> {
    if (!force && detail.value && detail.value.id === id) {
      return detail.value
    }
    detailLoading.value = true
    try {
      const res = await getSopDetail(id)
      detail.value = res
      return res
    } finally {
      detailLoading.value = false
    }
  }

  /** 拉取执行历史 */
  async function fetchRuns(id: number, pageNo = 1, pageSize = 10): Promise<void> {
    runsLoading.value = true
    try {
      const res = await listSopRuns(id, pageNo, pageSize)
      runs.value = res.records ?? []
      runsTotal.value = res.total ?? 0
    } finally {
      runsLoading.value = false
    }
  }

  /** 创建并刷新列表 */
  async function createAndRefresh(payload: SopSavePayload): Promise<number> {
    const res = await createSop(payload)
    await fetchList({ page: 1 })
    return res.id
  }

  /** 编辑：更新后清掉详情缓存并刷新列表 */
  async function updateAndRefresh(id: number, payload: SopSavePayload): Promise<void> {
    await updateSop(id, payload)
    detail.value = null
    await fetchList()
  }

  /** 删除：清掉详情缓存并刷新列表 */
  async function removeAndRefresh(id: number): Promise<void> {
    await deleteSop(id)
    if (detail.value?.id === id) {
      detail.value = null
    }
    await fetchList()
  }

  /** 发起执行 */
  async function startRun(id: number): Promise<number> {
    const res = await startSopRun(id)
    return res.runId
  }

  /** 结束执行：刷新列表（使用次数）与详情统计 */
  async function finishRun(runId: number, payload: SopRunFinishPayload): Promise<void> {
    await finishSopRun(runId, payload)
    const currentId = detail.value?.id
    if (currentId != null) {
      detail.value = null
      await fetchDetail(currentId, true)
      await fetchRuns(currentId)
    }
    await fetchList()
  }

  return {
    // 列表
    list,
    total,
    page,
    size,
    keyword,
    category,
    listLoading,
    isEmpty,
    categories,
    fetchList,
    createAndRefresh,
    updateAndRefresh,
    removeAndRefresh,
    // 详情
    detail,
    detailLoading,
    fetchDetail,
    // 执行
    runs,
    runsTotal,
    runsLoading,
    fetchRuns,
    startRun,
    finishRun
  }
})
