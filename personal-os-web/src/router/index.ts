import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/layouts/BlankLayout.vue'),
    meta: { public: true },
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/login/Login.vue')
      }
    ]
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录一律跳登录页；已登录访问登录页则跳首页
router.beforeEach((to) => {
  const userStore = useUserStore()
  const isPublic = to.meta.public === true

  if (!isPublic && !userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (isPublic && userStore.token && to.name === 'Login') {
    return { path: '/' }
  }
  return true
})

export default router
