<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useLifeHabitStore } from '@/stores/lifeStore'
import { MSG_PARSE_FAILED, saveRecord, quickRecord } from '@/api/life'
import type { HabitSaveParams, HabitVO, QuickRecordVO } from '@/api/life'
import LifeHabitCard from './components/LifeHabitCard.vue'
import LifeHabitManager from './components/LifeHabitManager.vue'
import LifeQuickRecordBar from './components/LifeQuickRecordBar.vue'
import LifeQuickRecordModal from './components/LifeQuickRecordModal.vue'

/**
 * 生活域主页面壳（router 已注册 `/life`，本组件整体替换 M0 占位）。
 *
 * 三个视图态（组件切换，不新增路由）：
 * 1) 今日打卡（默认）：顶部快捷记录条 + 习惯卡网格
 * 2) 习惯管理：增删改
 * 3) 快捷记录预填确认卡：浮层
 *
 * 注意：`.wallpaper` 已由 DefaultLayout 提供，本页不再放置，仅保证内容 z-index 在其上。
 */

const store = useLifeHabitStore()

/** 当前视图：today 今日打卡 / manage 习惯管理 */
const view = ref<'today' | 'manage'>('today')

/** 快捷记录解析中 */
const quickLoading = ref<boolean>(false)

/** 快捷记录内联错误 */
const quickError = ref<string>('')

/** 预填确认卡开合 + 解析结果 */
const modalOpen = ref<boolean>(false)
const parsed = ref<QuickRecordVO | null>(null)

/** 落库中 */
const recordSaving = ref<boolean>(false)

/** 新建 / 编辑保存中 */
const habitSaving = ref<boolean>(false)

/** 今日进度文案 */
const progressText = computed<string>(() => {
  const total = store.habits.length
  if (total === 0) {
    return ''
  }
  return `今日已完成 ${store.doneCount} / ${total}`
})

/** 习惯列表是否为空 */
const isEmpty = computed<boolean>(() => !store.loading && store.habits.length === 0)

onMounted(() => {
  store.refresh().catch(() => undefined)
})

/** 切换视图 */
function switchView(next: 'today' | 'manage'): void {
  view.value = next
}

/** 打卡型打卡 */
function handleCheckin(habit: HabitVO): void {
  store.checkin(habit).catch(() => undefined)
}

/** 计数型 +1 */
function handleIncrement(habit: HabitVO, amount: number): void {
  store.checkin(habit, amount).catch(() => undefined)
}

/** 新建习惯 */
function handleCreate(payload: HabitSaveParams): void {
  habitSaving.value = true
  store
    .createHabit(payload)
    .then(() => ElMessage.success('已新建习惯'))
    .catch(() => undefined)
    .finally(() => {
      habitSaving.value = false
    })
}

/** 更新习惯 */
function handleUpdate(id: number, payload: HabitSaveParams): void {
  habitSaving.value = true
  store
    .updateHabit(id, payload)
    .then(() => ElMessage.success('已保存'))
    .catch(() => undefined)
    .finally(() => {
      habitSaving.value = false
    })
}

/** 删除习惯 */
function handleRemove(id: number): void {
  store
    .removeHabit(id)
    .then(() => ElMessage.success('已删除'))
    .catch(() => undefined)
}

/** 快捷记录解析失败时由输入条清错 */
function clearQuickError(): void {
  quickError.value = ''
}

/** 提交快捷记录文本 → 解析 → 弹预填确认卡 */
async function handleQuickSubmit(text: string): Promise<void> {
  quickError.value = ''
  quickLoading.value = true
  try {
    // silent：解析失败不弹全局报错，改由输入框下方内联提示
    const res = await quickRecord(text, true)
    parsed.value = res
    modalOpen.value = true
  } catch {
    // 14003 解析失败：silent 呼叫下拿不到 code，统一走输入框下方内联提示降级
    quickError.value = MSG_PARSE_FAILED
  } finally {
    quickLoading.value = false
  }
}

/** 确认落库 */
function handleConfirmRecord(payload: { metricId: number; value: number; date: string }): void {
  recordSaving.value = true
  saveRecord(payload)
    .then(() => {
      modalOpen.value = false
      parsed.value = null
      ElMessage.success('已记录')
      // 记录可能命中某个习惯指标，刷新列表让网格上的「今日值」保持同步
      store.refresh().catch(() => undefined)
    })
    .catch(() => undefined)
    .finally(() => {
      recordSaving.value = false
    })
}
</script>

<template>
  <div class="life-page">
    <div class="life-page__inner">
      <!-- 页头：标题 + 视图切换 -->
      <header class="life-head">
        <div class="life-head__text">
          <h1 class="life-head__title">生活</h1>
          <p v-if="view === 'today' && progressText" class="life-head__sub">
            {{ progressText }}
          </p>
        </div>

        <div class="life-tabs glass glass--thin" role="tablist" aria-label="生活视图">
          <button
            type="button"
            class="life-tabs__item"
            role="tab"
            :aria-selected="view === 'today'"
            :class="{ 'life-tabs__item--on': view === 'today' }"
            @click="switchView('today')"
          >
            今日打卡
          </button>
          <button
            type="button"
            class="life-tabs__item"
            role="tab"
            :aria-selected="view === 'manage'"
            :class="{ 'life-tabs__item--on': view === 'manage' }"
            @click="switchView('manage')"
          >
            习惯管理
          </button>
        </div>
      </header>

      <!-- ============ 今日打卡 ============ -->
      <template v-if="view === 'today'">
        <LifeQuickRecordBar
          :loading="quickLoading"
          :error-message="quickError"
          @submit="handleQuickSubmit"
          @clear-error="clearQuickError"
        />

        <div v-if="store.loading && !store.habits.length" class="life-state">
          <span class="spinner" aria-hidden="true" />
          <span>加载中…</span>
        </div>

        <p v-else-if="isEmpty" class="life-state life-state--empty">
          还没有习惯，去「习惯管理」新建一个吧
        </p>

        <div v-else class="habit-grid">
          <LifeHabitCard
            v-for="item in store.habitList"
            :key="item.habit.id"
            :habit="item.habit"
            :streak="item.streak"
            :checking="store.isChecking(item.habit.id)"
            @checkin="handleCheckin"
            @increment="handleIncrement"
          />
        </div>
      </template>

      <!-- ============ 习惯管理 ============ -->
      <LifeHabitManager
        v-else
        :habits="store.habits"
        :saving="habitSaving"
        @create="handleCreate"
        @update="handleUpdate"
        @remove="handleRemove"
      />
    </div>

    <!-- ============ 快捷记录预填确认卡 ============ -->
    <LifeQuickRecordModal
      v-model="modalOpen"
      :parsed="parsed"
      :saving="recordSaving"
      @confirm="handleConfirmRecord"
    />
  </div>
</template>

<style lang="scss">
/* 模块级暖色 token（连续打卡火焰的明暗两套变量），见 life-theme.scss */
@use './life-theme';
</style>

<style scoped lang="scss">
.life-page {
  display: flex;
  justify-content: center;
  padding-bottom: 40px;
}

.life-page__inner {
  display: flex;
  width: 100%;
  max-width: 720px;
  flex-direction: column;
  gap: 16px;
  margin-top: 3vh;
}

/* ---------- 页头 ---------- */
.life-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px;
}

.life-head__text {
  min-width: 0;
}

.life-head__title {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--text-primary);
}

.life-head__sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.life-tabs {
  display: flex;
  flex: 0 0 auto;
  gap: 4px;
  padding: 4px;
  border-radius: var(--r-pill);
}

.life-tabs__item {
  height: 30px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--dur) var(--spring), background-color var(--dur) var(--spring);
}

.life-tabs__item--on {
  color: var(--accent);
  background: var(--glass-bg-thick);
}

.life-tabs__item:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* ---------- 习惯网格（横排自适应；窄屏单列） ---------- */
.habit-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

/* ---------- 状态 ---------- */
.life-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 0;
  font-size: 14px;
  text-align: center;
  color: var(--text-secondary);
}

.life-state--empty {
  color: var(--text-secondary);
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
    animation: po-life-page-spin 0.7s linear infinite;
  }
}

@keyframes po-life-page-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ---------- 390px 移动端 ---------- */
@media (max-width: 480px) {
  .life-page__inner {
    gap: 12px;
    margin-top: 1vh;
  }

  .life-head {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .life-head__title {
    font-size: 22px;
  }

  .life-tabs {
    align-self: flex-start;
  }

  /* 单列 */
  .habit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
