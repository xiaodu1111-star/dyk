<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

/** 功能领域占位；本期均未开放 */
const domains = ['工作', '学习', '运动', '理财', '生活']

onMounted(() => {
  // 刷新页面后 store 为空时补拉一次当前用户信息
  if (!userStore.userInfo) {
    userStore.fetchMe().catch(() => undefined)
  }
})
</script>

<template>
  <div class="home">
    <section class="greeting glass glass--thick">
      <h1 class="greeting__title">你好，{{ userStore.displayName }}</h1>
      <p class="greeting__subtitle">主页面功能开发中</p>
    </section>

    <section class="domains glass glass--thin" aria-labelledby="domains-title">
      <h2 id="domains-title" class="domains__title">即将上线</h2>
      <ul class="domains__list">
        <li
          v-for="domain in domains"
          :key="domain"
          class="domain-pill"
          aria-disabled="true"
          title="即将上线"
        >
          {{ domain }}
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped lang="scss">
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  /* 内容整体垂直居中偏上，避免下方大片空白 */
  margin-top: 8vh;
  padding-bottom: 24px;
}

.greeting {
  width: 100%;
  max-width: 720px;
  /* 高度按内容来，不占满 */
  padding: 28px 32px;
  border-radius: var(--r-xl);
}

@media (prefers-reduced-motion: no-preference) {
  .greeting {
    animation: po-home-enter var(--dur-slow) var(--spring) both;
  }
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

/* 五个领域收进一张玻璃卡，不再是孤立的漂浮元素 */
.domains {
  width: 100%;
  max-width: 720px;
  padding: 20px 24px 22px;
  border-radius: var(--r-lg);
}

.domains__title {
  margin-bottom: 14px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.4px;
  color: var(--text-tertiary);
}

.domains__list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* 未启用态用"去饱和 + 次要文字色"表达，而不是降透明度，保证文字清晰可读 */
.domain-pill {
  display: inline-flex;
  align-items: center;
  height: 40px;
  padding: 0 20px;
  font-size: 14px;
  color: var(--text-secondary);
  background: var(--glass-bg-thin);
  border: 1px solid var(--separator);
  border-radius: var(--r-pill);
  cursor: not-allowed;
  filter: saturate(0.6);
  transition: color var(--dur) var(--spring), filter var(--dur) var(--spring);
}

.domain-pill:hover {
  color: var(--text-primary);
  filter: saturate(1);
}

@media (max-width: 480px) {
  .home {
    margin-top: 4vh;
    gap: 14px;
  }

  .greeting,
  .domains {
    /* 移动端卡片铺满 */
    max-width: none;
  }

  .greeting {
    padding: 22px 20px;
    border-radius: var(--r-lg);
  }

  .greeting__title {
    font-size: 24px;
  }

  .domains {
    padding: 16px 18px 18px;
  }

  .domain-pill {
    padding: 0 16px;
  }
}
</style>
