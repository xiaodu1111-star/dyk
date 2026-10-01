<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSopStore } from '@/stores/sopStore'
import type { SopVO } from '@/api/sop'
import {
  CATEGORY_SUGGESTIONS,
  cardSummary,
  categoryHue,
  formatAvgMinutes,
  formatRelative,
  statusLabel
} from './utils'

const emit = defineEmits<{
  (e: 'create'): void
  (e: 'open', id: number): void
  (e: 'edit', id: number): void
}>()

const store = useSopStore()

const searchInput = ref('')

/** 分类筛选选项：既有 SOP 的分类 ∪ 建议分类 */
const categoryOptions = computed(() => {
  const set = new Set<string>([...store.categories, ...CATEGORY_SUGGESTIONS])
  return Array.from(set)
})

const hasFilter = computed(() => !!store.keyword || !!store.category)

onMounted(() => {
  searchInput.value = store.keyword
  store.fetchList({ page: 1 }).catch(() => undefined)
})

async function applySearch(): Promise<void> {
  store.keyword = searchInput.value.trim()
  store.page = 1
  await store.fetchList({ page: 1 }).catch(() => undefined)
}

async function clearFilters(): Promise<void> {
  searchInput.value = ''
  store.keyword = ''
  store.category = ''
  store.page = 1
  await store.fetchList({ page: 1 }).catch(() => undefined)
}

async function selectCategory(category: string): Promise<void> {
  store.category = store.category === category ? '' : category
  store.page = 1
  await store.fetchList({ page: 1 }).catch(() => undefined)
}

async function changePage(next: number): Promise<void> {
  if (next < 1) return
  store.page = next
  await store.fetchList({ page: next }).catch(() => undefined)
}

const totalPages = computed(() =>
  Math.max(1, Math.ceil(store.total / (store.size || 10)))
)

/** 删除（卡片上的快捷操作，带二次确认） */
async function handleDelete(sop: SopVO, event: Event): Promise<void> {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm(`确定删除《${sop.title}》吗？删除后不可恢复。`, '删除 SOP', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await store.removeAndRefresh(sop.id)
    ElMessage.success('已删除')
  } catch {
    // 全局拦截器已提示
  }
}
</script>

<template>
  <div class="sop-list">
    <!-- 页头 -->
    <header class="sop-head glass glass--thick">
      <div class="sop-head__text">
        <h1 class="sop-head__title">SOP 库</h1>
        <p class="sop-head__subtitle">沉淀工作流程 —— 任务是消耗，SOP 是复利</p>
      </div>
      <button type="button" class="sop-head__new" @click="emit('create')">
        <svg
          viewBox="0 0 24 24"
          width="18"
          height="18"
          aria-hidden="true"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
        >
          <path d="M12 5.4v13.2M5.4 12h13.2" />
        </svg>
        <span>新建 SOP</span>
      </button>
    </header>

    <!-- 工具栏：搜索 + 分类筛选 -->
    <section class="sop-toolbar glass glass--thin" aria-label="筛选与搜索">
      <label class="sr-only" for="sop-search">搜索 SOP</label>
      <div class="search glass glass--thin">
        <svg
          class="search__icon"
          viewBox="0 0 24 24"
          width="18"
          height="18"
          aria-hidden="true"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
        >
          <circle cx="10.6" cy="10.6" r="6.4" />
          <path d="m15.4 15.4 4.2 4.2" />
        </svg>
        <input
          id="sop-search"
          v-model="searchInput"
          class="search__input"
          type="search"
          placeholder="搜索标题 / 场景"
          autocomplete="off"
          @keyup.enter="applySearch"
        />
        <button
          v-if="searchInput"
          type="button"
          class="search__clear"
          aria-label="清空搜索"
          @click="clearFilters"
        >
          <svg
            viewBox="0 0 24 24"
            width="15"
            height="15"
            aria-hidden="true"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          >
            <path d="M6.6 6.6l10.8 10.8M17.4 6.6 6.6 17.4" />
          </svg>
        </button>
      </div>

      <button type="button" class="toolbar__search-btn" @click="applySearch">搜索</button>
    </section>

    <div v-if="categoryOptions.length" class="chips" role="group" aria-label="按分类筛选">
      <button
        type="button"
        class="chip glass glass--thin"
        :class="{ 'chip--on': !store.category }"
        :aria-pressed="!store.category"
        @click="clearFilters"
      >
        全部
      </button>
      <button
        v-for="cat in categoryOptions"
        :key="cat"
        type="button"
        class="chip glass glass--thin"
        :class="{ 'chip--on': store.category === cat }"
        :aria-pressed="store.category === cat"
        @click="selectCategory(cat)"
      >
        <span
          class="chip__dot"
          aria-hidden="true"
          :style="{ background: `hsl(${categoryHue(cat)} 78% 52%)` }"
        />
        {{ cat }}
      </button>
    </div>

    <!-- 加载骨架 -->
    <div v-if="store.listLoading && store.list.length === 0" class="cards" aria-busy="true">
      <div v-for="n in 3" :key="n" class="sop-card sop-card--skeleton glass" />
    </div>

    <!-- 空态 -->
    <section v-else-if="store.isEmpty" class="empty glass glass--thick">
      <div class="empty__badge glass glass--thin" aria-hidden="true">
        <svg
          viewBox="0 0 24 24"
          width="26"
          height="26"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M5.5 4.5h9l5 5v10a1.5 1.5 0 0 1-1.5 1.5h-12.5a1.5 1.5 0 0 1-1.5-1.5v-13.5a1.5 1.5 0 0 1 1.5-1.5Z" />
          <path d="M14 4.5v5h5" />
          <path d="M8.5 13.6h7M8.5 16.6h4.4" />
        </svg>
      </div>
      <h2 class="empty__title">{{ hasFilter ? '没有匹配的 SOP' : '还没有 SOP' }}</h2>
      <p class="empty__desc">
        {{
          hasFilter
            ? '换个关键词或清空筛选试试。'
            : '把常做的工作流程写下来，下次照着一步步走。'
        }}
      </p>
      <button v-if="hasFilter" type="button" class="empty__btn" @click="clearFilters">
        清空筛选
      </button>
      <button v-else type="button" class="empty__btn" @click="emit('create')">
        新建第一个 SOP
      </button>
    </section>

    <!-- 卡片流 -->
    <div v-else class="cards">
      <article
        v-for="(sop, index) in store.list"
        :key="sop.id"
        class="sop-card glass"
        :class="{ 'sop-card--pinned': !!sop.pinned }"
        :style="{ '--card-delay': `${Math.min(index, 8) * 40}ms` }"
        role="button"
        tabindex="0"
        @click="emit('open', sop.id)"
        @keydown.enter="emit('open', sop.id)"
        @keydown.space.prevent="emit('open', sop.id)"
      >
        <!-- 顶部：分类色点 + 分类名 + 置顶钉 -->
        <div class="sop-card__top">
          <span class="sop-card__cat">
            <span
              class="sop-card__dot"
              aria-hidden="true"
              :style="{ background: `hsl(${categoryHue(sop.category)} 78% 52%)` }"
            />
            {{ sop.category || '未分类' }}
          </span>
          <span v-if="sop.pinned" class="sop-card__pin" title="已置顶">
            <svg
              viewBox="0 0 24 24"
              width="15"
              height="15"              aria-hidden="true"
              fill="currentColor"
            >
              <path
                d="M14.8 3.4 20.6 9.2l-2.1 1.1-1.2 4.3-2.0-2.0-4.4 4.4-1.0-.1-.1-1 4.4-4.4-2.0-2.0 4.3-1.2z"
              />
            </svg>
          </span>
        </div>

        <h3 class="sop-card__title">{{ sop.title }}</h3>
        <p class="sop-card__summary">{{ cardSummary(sop) }}</p>

        <!-- 复用数据：使用次数（大数字） + 平均耗时（次级） -->
        <div class="metrics">
          <div class="metric">
            <span class="metric__num">{{ sop.useCount ?? 0 }}</span>
            <span class="metric__label">次复用</span>
          </div>
          <div class="metric metric--sub">
            <span class="metric__sub-text">{{ formatAvgMinutes(sop.avgMinutes) }}</span>
          </div>
        </div>

        <footer class="sop-card__foot">
          <div class="sop-card__meta">
            <span class="sop-card__ver">{{ sop.version }}</span>
            <span class="sop-card__sep" aria-hidden="true">·</span>
            <span>{{ sop.stepCount ?? 0 }} 步</span>
            <span class="sop-card__sep" aria-hidden="true">·</span>
            <span>{{ formatRelative(sop.lastUsedAt) }}</span>
          </div>
          <div class="sop-card__actions">
            <span class="sop-card__status">{{ statusLabel(sop.status) }}</span>
            <button
              type="button"
              class="mini-btn"
              aria-label="编辑"
              title="编辑"
              @click.stop="emit('edit', sop.id)"
            >
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                aria-hidden="true"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M16.4 4.6 19.4 7.6 8.6 18.4l-3.6.8.8-3.6z" />
                <path d="M14.2 6.8l3 3" />
              </svg>
            </button>
            <button
              type="button"
              class="mini-btn mini-btn--danger"
              aria-label="删除"
              title="删除"
              @click="handleDelete(sop, $event)"
            >
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                aria-hidden="true"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M5.4 7.4h13.2" />
                <path d="M9.4 7.4V5.6a1.4 1.4 0 0 1 1.4-1.4h2.4a1.4 1.4 0 0 1 1.4 1.4v1.8" />
                <path d="M7 7.4l.9 11.2a1.6 1.6 0 0 0 1.6 1.5h5a1.6 1.6 0 0 0 1.6-1.5L17 7.4" />
              </svg>
            </button>
          </div>
        </footer>
      </article>
    </div>

    <!-- 分页 -->
    <nav v-if="totalPages > 1" class="pager glass glass--thin" aria-label="分页">
      <button
        type="button"
        class="pager__btn"
        :disabled="store.page <= 1"
        @click="changePage(store.page - 1)"
      >
        上一页
      </button>
      <span class="pager__info">{{ store.page }} / {{ totalPages }}</span>
      <button
        type="button"
        class="pager__btn"
        :disabled="store.page >= totalPages"
        @click="changePage(store.page + 1)"
      >
        下一页
      </button>
    </nav>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/glass' as *;

/* ---- 页头 ---- */
.sop-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 22px 26px;
  border-radius: var(--r-xl);
  @include enter-rise;
}

.sop-head__title {
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--text-primary);
}

.sop-head__subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.sop-head__new {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 55%, var(--accent-press) 100%);
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.42);
  @include pressable;
}

/* ---- 工具栏 ---- */
.sop-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: var(--r-lg);
}

.search {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 10px;
  height: 44px;
  min-width: 0;
  padding: 0 14px;
  border-radius: var(--r-pill);
  transition: border-color var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

.search:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.18), var(--glass-inner);
}

.search__icon {
  flex: 0 0 auto;
  color: var(--text-tertiary);
}

.search__input {
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
  font-size: 15px;
  color: var(--text-primary);
  background: transparent;
  border: 0;
}

.search__input::placeholder {
  color: var(--text-tertiary);
}

.search__input:focus-visible {
  outline: none;
}

.search__clear {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  color: var(--text-tertiary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.toolbar__search-btn {
  flex: 0 0 auto;
  height: 44px;
  padding: 0 18px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

/* ---- 分类 chips ---- */
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 14px;
  font-size: 13px;
  color: var(--text-secondary);
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.chip__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.chip--on {
  color: #fff;
  background: var(--accent);
  border-color: transparent;
}

.chip--on .chip__dot {
  background: #fff !important;
}

/* ---- 卡片流 ---- */
.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(276px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.sop-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 22px;
  text-align: left;
  border-radius: var(--r-lg);
  cursor: pointer;
  @include pressable;
}

@media (prefers-reduced-motion: no-preference) {
  .sop-card {
    animation: sop-card-in var(--dur-slow) var(--spring) both;
    animation-delay: var(--card-delay, 0ms);
  }
}

@keyframes sop-card-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sop-card:hover {
  border-color: var(--accent);
}

.sop-card--pinned {
  box-shadow: var(--glass-shadow), var(--glass-inner),
    0 0 0 1px rgba(0, 122, 255, 0.32);
}

.sop-card--skeleton {
  min-height: 190px;
  cursor: default;
  opacity: 0.5;
}

.sop-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.sop-card__cat {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
}

.sop-card__dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.05);
}

.sop-card__pin {
  display: inline-flex;
  align-items: center;
  color: var(--accent);
}

.sop-card__title {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sop-card__summary {
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 复用数据：大数字 + 次级文字 */
.metrics {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-top: 2px;
}

.metric {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;
}

.metric__num {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.5px;
  color: var(--accent);
}

.metric__label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.metric--sub {
  padding-left: 14px;
  border-left: 1px solid var(--separator);
}

.metric__sub-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.sop-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--separator);
}

.sop-card__meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.sop-card__ver {
  font-weight: 600;
  color: var(--text-secondary);
}

.sop-card__actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.sop-card__status {
  margin-right: 2px;
  padding: 3px 9px;
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
}

.mini-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: var(--text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  transition: color var(--dur) var(--spring), background-color var(--dur) var(--spring),
    transform var(--dur-fast) var(--spring);
}

@media (prefers-reduced-motion: no-preference) {
  .mini-btn:active {
    transform: scale(0.97);
  }
}

.mini-btn:hover {
  color: var(--text-primary);
  background: var(--glass-bg-thin);
}

.mini-btn--danger:hover {
  color: var(--danger);
}

/* ---- 空态 ---- */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 16px;
  padding: 44px 28px;
  text-align: center;
  border-radius: var(--r-xl);
  @include enter-rise;
}

.empty__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  color: var(--accent);
  border-radius: var(--r-pill);
}

.empty__title {
  font-size: 19px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty__desc {
  max-width: 320px;
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.empty__btn {
  height: 44px;
  margin-top: 20px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(180deg, #3d9bff 0%, var(--accent) 55%, var(--accent-press) 100%);
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.42);
  @include pressable;
}

/* ---- 分页 ---- */
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 18px;
  padding: 8px 12px;
  border-radius: var(--r-pill);
}

.pager__btn {
  height: 34px;
  padding: 0 16px;
  font-size: 14px;
  color: var(--text-primary);
  background: transparent;
  border: 0;
  border-radius: var(--r-pill);
  cursor: pointer;
  @include pressable;
}

.pager__btn:disabled {
  color: var(--text-tertiary);
  cursor: not-allowed;
}

.pager__info {
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 480px) {
  .sop-head {
    flex-direction: column;
    align-items: flex-start;
    padding: 18px 18px 20px;
    border-radius: var(--r-lg);
  }

  .sop-head__title {
    font-size: 23px;
  }

  .sop-head__new {
    width: 100%;
    justify-content: center;
  }

  .cards {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .metric__num {
    font-size: 26px;
  }
}
</style>
