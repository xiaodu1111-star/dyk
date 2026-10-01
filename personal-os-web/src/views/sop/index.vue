<script setup lang="ts">
/** SOP 库占位页（M0 基础设施）；M2 模块窗口整体替换本组件内容，不碰 router */
</script>

<template>
  <div class="page">
    <section class="card glass glass--thick">
      <h1 class="card__title">SOP 清单</h1>
      <p class="card__subtitle">SOP 库模块开发中 · M2</p>
    </section>
  </div>
</template>

<style scoped lang="scss">
.page {
  display: flex;
  justify-content: center;
  margin-top: 8vh;
  padding-bottom: 24px;
}

.card {
  width: 100%;
  max-width: 720px;
  padding: 28px 32px;
  border-radius: var(--r-xl);
  text-align: center;
}

.card__title {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 0.2px;
  color: var(--text-primary);
}

.card__subtitle {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

@media (max-width: 480px) {
  .page {
    margin-top: 4vh;
  }

  .card {
    max-width: none;
    padding: 22px 20px;
    border-radius: var(--r-lg);
  }

  .card__title {
    font-size: 24px;
  }
}
</style>
