import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import BlankLayout from '@/layouts/BlankLayout.vue'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/login',
    component: BlankLayout,
    children: [
      { path: '', name: 'Login', component: () => import('@/views/login/index.vue') },
    ],
  },
  {
    path: '/',
    component: BasicLayout,
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue') },
      { path: 'category', name: 'Category', component: () => import('@/views/category/index.vue') },
      { path: 'bank', name: 'Bank', component: () => import('@/views/bank/index.vue') },
      { path: 'question', name: 'Question', component: () => import('@/views/question/index.vue') },
      { path: 'eztest/api', name: 'EztestApi', component: () => import('@/views/eztest/api.vue') },
      { path: 'user', name: 'User', component: () => import('@/views/user/index.vue') },
      { path: 'activity/users', name: 'ActivityUsers', component: () => import('@/views/activity/users.vue') },
      { path: 'record/practice', name: 'PracticeRecord', component: () => import('@/views/record/practice.vue') },
      { path: 'record/exam', name: 'ExamRecord', component: () => import('@/views/record/exam.vue') },
      { path: 'activity/answers', name: 'ActivityAnswers', component: () => import('@/views/activity/answers.vue') },
      { path: 'favorite', name: 'Favorite', component: () => import('@/views/favorite/index.vue') },
      { path: 'wrong', name: 'Wrong', component: () => import('@/views/wrong/index.vue') },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/statistics/index.vue') },
      { path: 'vip/plan', name: 'VipPlan', component: () => import('@/views/vip/plan.vue') },
      { path: 'vip/order', name: 'VipOrder', component: () => import('@/views/vip/order.vue') },
      { path: 'ai/config', name: 'AiConfig', component: () => import('@/views/ai/config.vue') },
      { path: 'ai/batch', name: 'AiBatch', component: () => import('@/views/ai/batch.vue') },
      { path: 'ai/log', name: 'AiLog', component: () => import('@/views/ai/log.vue') },
      { path: 'bank/convert', name: 'BankConvert', component: () => import('@/views/bank/convert.vue') },
      { path: 'system/admin', name: 'SystemAdmin', component: () => import('@/views/system/admin.vue') },
      { path: 'system/setting', name: 'SystemSetting', component: () => import('@/views/system/setting.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  if (to.path !== '/login' && !authStore.token) {
    next('/login')
    return
  }
  if (to.path !== '/login' && authStore.token && !authStore.infoLoaded) {
    await authStore.fetchInfo()
    if (!authStore.adminInfo) {
      next('/login')
      return
    }
  }
  if (to.path !== '/login' && authStore.adminInfo?.role !== 'super_admin') {
    const allowed = new Set(['/dashboard', '/category', '/bank', '/question', '/eztest/api'])
    next(allowed.has(to.path) ? undefined : '/dashboard')
  } else {
    next()
  }
})

export default router
