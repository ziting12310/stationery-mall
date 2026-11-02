import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '../utils/request'

/**
 * 路由表（路径与页面清单严格对应）
 * meta.requiresAuth = true 的页面：未登录会被守卫拦截到 /login
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { title: '文具商城' }
  },
  {
    path: '/goods/:id',
    name: 'GoodsDetail',
    component: () => import('../views/GoodsDetailView.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/CartView.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('../views/OrderView.vue'),
    meta: { title: '订单确认', requiresAuth: true }
  },
  {
    // 我的订单（新增；/order 已被“订单确认页”占用，故列表页使用 /orders）
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/OrdersView.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('../views/UserView.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    // 兜底：未匹配路径回到首页
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局路由守卫（登录权限控制）
 * 1. 未登录访问购物车 / 订单 / 个人中心 → /login?redirect=原路径
 * 2. 已登录再访问登录 / 注册页 → 直接回首页
 */
router.beforeEach((to) => {
  const token = getToken()

  if (to.meta.requiresAuth && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (token && (to.path === '/login' || to.path === '/register')) {
    return { path: '/' }
  }
  return true
})

/** 同步页面标题 */
router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 文具商城` : '文具商城'
})

export default router
