<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getDashboardHome, type DashboardData } from '@/api/dashboard'

const router = useRouter()
const userStore = useUserStore()

const data = ref<DashboardData | null>(null)
const loading = ref(true)
const failed = ref(false)

/** 进度百分比（0-100），分母为 0 时返回 0 */
function percent(done: number, total: number): number {
  if (!total || total <= 0) return 0
  return Math.min(100, Math.round((done / total) * 100))
}

async function load(): Promise<void> {
  loading.value = true
  failed.value = false
  try {
    data.value = await getDashboardHome()
  } catch {
    // 首页降级：不弹错误提示，卡片显示占位说明即可
    failed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchMe().catch(() => undefined)
  }
  load()
})
</script>

<template>
  <div class="home">
    <!-- 问候 -->
    <section class="greeting glass glass--thick">
      <h1 class="greeting__title">你好，{{ userStore.displayName }}</h1>
      <p class="greeting__subtitle">
        <template v-if="data">今天是 {{ data.today.date }} · {{ data.today.week }}</template>
        <template v-else>正在汇总今天…</template>
      </p>
    </section>

    <p v-if="failed" class="home__notice glass glass--thin">
      数据加载失败，稍后再试（页面其余功能不受影响）
    </p>

    <div class="cards">
      <!-- 工作 -->
      <section
        class="card glass glass--thin"
        role="link"
        tabindex="0"
        aria-label="查看工作任务"
        @click="router.push('/work')"
        @keyup.enter="router.push('/work')"
      >
        <header class="card__head">
          <h2 class="card__title">工作</h2>
          <span v-if="data && data.work.overdue > 0" class="badge badge--danger">
            逾期 {{ data.work.overdue }}
          </span>
        </header>
        <p class="metric">
          <span class="metric__value">{{ data?.work.todayDone ?? 0 }}</span>
          <span class="metric__unit">/ {{ data?.work.todayTotal ?? 0 }} 今日任务</span>
        </p>
        <div class="bar" aria-hidden="true">
          <i class="bar__fill" :style="{ width: percent(data?.work.todayDone ?? 0, data?.work.todayTotal ?? 0) + '%' }" />
        </div>
        <p class="card__foot">点击进入工作台 →</p>
      </section>

      <!-- 生活 -->
      <section
        class="card glass glass--thin"
        role="link"
        tabindex="0"
        aria-label="查看生活习惯"
        @click="router.push('/life')"
        @keyup.enter="router.push('/life')"
      >
        <header class="card__head">
          <h2 class="card__title">生活</h2>
          <span v-if="data && data.streakDays > 0" class="chip chip--accent">
            连续 {{ data.streakDays }} 天
          </span>
        </header>
        <p class="metric">
          <span class="metric__value">{{ data?.life.checkinDone ?? 0 }}</span>
          <span class="metric__unit">/ {{ data?.life.checkinTotal ?? 0 }} 已打卡</span>
        </p>
        <ul v-if="data && data.life.habits.length" class="dots">
          <li
            v-for="habit in data.life.habits"
            :key="habit.id"
            class="dot"
            :class="{ 'dot--done': habit.done }"
            :title="habit.name"
          >
            {{ habit.name }}
          </li>
        </ul>
        <p class="card__foot">点击进入生活页 →</p>
      </section>

      <!-- SOP 清单 -->
      <section
        class="card glass glass--thin"
        role="link"
        tabindex="0"
        aria-label="查看 SOP 清单"
        @click="router.push('/sop')"
        @keyup.enter="router.push('/sop')"
      >
        <header class="card__head">
          <h2 class="card__title">SOP 清单</h2>
        </header>

        <ul v-if="data && data.sop.top.length" class="sop-list">
          <li v-for="item in data.sop.top" :key="item.id" class="sop-item">
            <span class="sop-item__title">{{ item.title }}</span>
            <span class="sop-item__count">用过 {{ item.useCount }} 次</span>
          </li>
        </ul>
        <p v-else class="card__empty">还没有 SOP，把重复做的事沉淀下来 →</p>

        <!-- 重复劳动提示：跨模块读活动流得出 -->
        <ul v-if="data && data.sopHints.length" class="hints">
          <li v-for="hint in data.sopHints" :key="hint.taskTitle" class="hint">
            「{{ hint.taskTitle }}」近 30 天做了 {{ hint.count }} 次，值得写成 SOP
          </li>
        </ul>

        <p class="card__foot">点击进入 SOP 库 →</p>
      </section>
    </div>

    <p v-if="loading" class="home__loading" aria-live="polite">加载中…</p>
  </div>
</template>

<style scoped lang="scss">
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 5vh;
  padding-bottom: 24px;
}

.greeting {
  width: 100%;
  max-width: 900px;
  padding: 26px 30px;
  border-radius: var(--r-xl);
}

@keyframes po-home-enter {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (prefers-reduced-motion: no-preference) {
  .greeting,
  .card {
    animation: po-home-enter var(--dur-slow) var(--spring) both;
  }
}

.greeting__title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--text-primary);
}

.greeting__subtitle {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.home__notice,
.home__loading {
  width: 100%;
  max-width: 900px;
  padding: 12px 18px;
  font-size: 13px;
  border-radius: var(--r-md);
}

.home__notice {
  color: var(--danger);
}

.home__loading {
  color: var(--text-tertiary);
  text-align: center;
}

.cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  width: 100%;
  max-width: 900px;
}

.card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px 22px 16px;
  border-radius: var(--r-lg);
  cursor: pointer;
  transition: transform var(--dur) var(--spring), box-shadow var(--dur) var(--spring);
}

.card:hover {
  box-shadow: var(--glass-shadow-dialog);
}

@media (prefers-reduced-motion: no-preference) {
  .card:hover {
    transform: translateY(-2px);
  }
}

.card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card__title {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: var(--text-secondary);
}

.badge,
.chip {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: var(--r-pill);
}

.badge--danger {
  color: #fff;
  background: var(--danger);
}

.chip--accent {
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 14%, transparent);
}

.metric {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.metric__value {
  font-size: 30px;
  font-weight: 600;
  line-height: 1.1;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.metric__unit {
  font-size: 13px;
  color: var(--text-secondary);
}

.bar {
  height: 5px;
  overflow: hidden;
  background: var(--separator);
  border-radius: var(--r-pill);
}

.bar__fill {
  display: block;
  height: 100%;
  background: var(--accent);
  border-radius: inherit;
  transition: width var(--dur-slow) var(--spring);
}

.dots {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.dot {
  padding: 3px 10px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
}

.dot--done {
  color: var(--success);
  border-color: color-mix(in srgb, var(--success) 40%, transparent);
  background: color-mix(in srgb, var(--success) 12%, transparent);
}

.sop-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sop-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}

.sop-item__title {
  overflow: hidden;
  color: var(--text-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sop-item__count {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-tertiary);
}

.card__empty,
.card__foot {
  font-size: 12px;
  color: var(--text-tertiary);
}

.card__empty {
  color: var(--text-secondary);
}

.hints {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid var(--separator);
}

.hint {
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-secondary);
}

@media (max-width: 860px) {
  .cards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .home {
    margin-top: 3vh;
    gap: 12px;
  }

  .greeting,
  .cards,
  .home__notice,
  .home__loading {
    max-width: none;
  }

  .greeting {
    padding: 20px 18px;
    border-radius: var(--r-lg);
  }

  .greeting__title {
    font-size: 24px;
  }

  .card {
    padding: 16px 18px 14px;
  }
}
</style>
