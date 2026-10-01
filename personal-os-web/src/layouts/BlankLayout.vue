<script setup lang="ts">
import ThemeToggle from '@/components/ThemeToggle.vue'

// 空白布局：全屏壁纸容器，供登录页等无框架页面使用
</script>

<template>
  <div class="layout-blank">
    <div class="wallpaper" aria-hidden="true"></div>

    <div class="layout-blank__theme">
      <ThemeToggle />
    </div>

    <div class="layout-blank__content">
      <router-view />
    </div>
  </div>
</template>

<style scoped lang="scss">
.layout-blank {
  position: relative;
  min-height: 100vh;
}

.layout-blank__theme {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 3;
}

.layout-blank__content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
}

@media (max-width: 480px) {
  .layout-blank__theme {
    top: 12px;
    right: 12px;
  }

  .layout-blank__content {
    padding: 16px;
  }
}
</style>
