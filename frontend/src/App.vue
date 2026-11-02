<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAuth, getToken, USER_INFO_KEY } from './utils/request'
import { logout, type UserInfo } from './api/auth'

const route = useRoute()
const router = useRouter()

/** 当前路由路径，用于导航高亮 */
const activePath = computed(() => {
  if (route.path.startsWith('/goods')) return '/'
  return route.path
})

/** 登录状态与用户名（从 localStorage 读取，登录页写入后通过 watch 同步） */
const token = ref(getToken())
const userInfo = ref<UserInfo | null>(null)

/** 读取本地用户信息 */
function loadUserInfo(): void {
  const raw = localStorage.getItem(USER_INFO_KEY)
  try {
    userInfo.value = raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    userInfo.value = null
  }
}

loadUserInfo()

// 路由变化时刷新登录态（登录/登出后无需全局状态管理即可同步导航栏）
watch(() => route.fullPath, () => {
  token.value = getToken()
  loadUserInfo()
})

/** 退出登录：先调用后端登出接口使 token 失效，再清理本地登录态 */
async function handleLogout(): Promise<void> {
  try {
    await logout()
  } catch {
    // token 已失效等情况不影响本地登出
  }
  clearAuth()
  token.value = ''
  userInfo.value = null
  ElMessage.success('已退出登录')
  router.push('/')
}

/** 导航栏搜索关键词 */
const searchKeyword = ref('')

/**
 * 顶部搜索：关键词为空时提示且不发请求；
 * 否则跳转商品首页并携带 keyword，由首页调用后端模糊搜索接口。
 */
function handleSearch(): void {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入商品关键词')
    return
  }
  router.push({ path: '/', query: { keyword } })
}

/** 未登录点击购物车 / 个人中心时先登录 */
function goAuthPage(path: string): void {
  router.push(path)
}
</script>

<template>
  <el-container class="app-container jp-page">
    <!-- 全局公共导航栏 -->
    <el-header class="app-header">
      <div class="header-inner">
        <div class="logo" @click="router.push('/')">文具商城</div>

        <el-menu
          class="nav-menu"
          mode="horizontal"
          :default-active="activePath"
          :ellipsis="false"
          router
        >
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/cart">购物车</el-menu-item>
          <!-- 新增：我的订单（位于购物车与个人中心之间） -->
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-menu-item index="/user">个人中心</el-menu-item>
        </el-menu>

        <!-- 顶部搜索：输入框 + 搜索按钮（手绘涂鸦风） -->
        <div class="header-search">
          <el-input
            v-model="searchKeyword"
            class="search-input"
            placeholder="搜索文具…"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button class="search-btn jp-btn jp-btn-primary" type="primary" round @click="handleSearch">
            搜索
          </el-button>
        </div>

        <div class="header-right">
          <template v-if="token">
            <!-- 方形贴纸头像占位（静态占位，不引入额外数据绑定） -->
            <div class="avatar">文</div>
            <span class="username">{{ userInfo?.username || '已登录用户' }}</span>
            <el-button link type="primary" class="logout-btn" @click="handleLogout">退出登录</el-button>
          </template>
          <template v-else>
            <el-button link type="primary" @click="goAuthPage('/login')">登录</el-button>
            <el-button link @click="goAuthPage('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <!-- 页面内容 -->
    <el-main class="app-main">
      <router-view />
    </el-main>

    <el-footer class="app-footer">文具商城 · 课程实践项目</el-footer>
  </el-container>
</template>

<style>
* {
  box-sizing: border-box;
}

/* ==================== 全局基础样式（手绘涂鸦笔记本） ==================== */
body {
  margin: 0;
  background-color: #fffef5;
  /* 笔记本横线纹理 */
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0 31px,
    rgba(78, 205, 196, 0.16) 31px 32px
  );
  color: #2c2c2c;
  font-family: 'Segoe Print', 'Comic Sans MS', 'PingFang SC', 'Microsoft YaHei',
    sans-serif;
  font-weight: 400;
  letter-spacing: 0.01em;
}

.app-container {
  position: relative;
  min-height: 100vh;
  background-color: transparent;
}

/* 纸页左侧红色页边线（手绘虚线） */
.app-container::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 24px;
  width: 1px;
  border-left: 1px dashed rgba(255, 107, 107, 0.55);
  pointer-events: none;
  z-index: 0;
}

/* ==================== 吸顶导航栏：纸底 + 虚线墨边 ==================== */
.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background-color: #fffef5;
  border-bottom: 1px dashed #2c2c2c;
  padding: 0;
}

.header-inner {
  position: relative;
  max-width: 1180px;
  height: 64px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0.06em;
  color: #2c2c2c;
  cursor: pointer;
  white-space: nowrap;
  text-decoration: underline wavy #ff6b6b 1px;
  text-underline-offset: 6px;
  transition: all 300ms ease-out;
}

.logo:hover {
  color: #ff6b6b;
  transform: rotate(-1deg);
}

/* 中间导航：菜单项像手写便签，激活时贴一条黄色马克笔 */
.nav-menu {
  flex: 1;
  height: 64px;
  display: flex;
  align-items: center;
  border-bottom: none !important;
  background: transparent;
}

.nav-menu .el-menu-item {
  height: 36px;
  line-height: 36px;
  margin: 0 4px;
  padding: 0 16px;
  border: 1px dashed transparent;
  border-radius: 4px;
  border-bottom: 1px dashed transparent !important;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.04em;
  color: #2c2c2c;
  background: transparent;
  box-shadow: none;
  transition: all 300ms ease-out;
}

.nav-menu .el-menu-item:hover {
  background-color: rgba(255, 217, 61, 0.35);
  color: #2c2c2c;
  border-color: #2c2c2c;
  transform: rotate(-1deg);
}

.nav-menu .el-menu-item.is-active {
  background-color: #ffd93d;
  color: #2c2c2c;
  font-weight: 500;
  border: 1px dashed #2c2c2c !important;
  border-bottom: 1px dashed #2c2c2c !important;
  box-shadow: 2px 2px 0 #2c2c2c;
}

/* ==================== 顶部搜索框（虚线边框 + 黄硬阴影） ==================== */
.header-search {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-search .search-input {
  width: 180px;
}

.header-search .search-input .el-input__wrapper {
  background: #fffef5;
  border: 1px dashed #2c2c2c;
  border-radius: 4px;
  box-shadow: 2px 2px 0 #ffd93d;
  transition: all 300ms ease-out;
}

.header-search .search-input .el-input__wrapper:hover {
  box-shadow: 3px 3px 0 #4ecdc4;
}

.header-search .search-input .el-input__wrapper.is-focus {
  border-color: #ff6b6b;
  box-shadow: 3px 3px 0 #4ecdc4;
}

.header-search .search-input .el-input__inner {
  color: #2c2c2c;
  font-size: 13px;
}

.header-search .search-btn {
  transition: all 300ms ease-out;
}

/* 窄屏：搜索框收窄，避免挤压导航 */
@media (max-width: 960px) {
  .header-search .search-input {
    width: 130px;
  }
}

/* 右侧登录态 */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}
/* 方形贴纸头像（不用圆形，保持手绘涂鸦的方形标签感） */
.avatar {
  width: 30px;
  height: 30px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 500;
  color: #2c2c2c;
  background: rgba(255, 217, 61, 0.55);
  border: 1px dashed #2c2c2c;
  box-shadow: 2px 2px 0 #4ecdc4;
  transform: rotate(-1deg);
}

.username {
  color: #2c2c2c;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.02em;
  transition: all 300ms ease-out;
}

.username:hover {
  color: #ff6b6b;
}

.logout-btn {
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.02em;
  transition: all 300ms ease-out;
}

/* ==================== 内容区 / 页脚 ==================== */
.app-main {
  position: relative;
  max-width: 1180px;
  width: 100%;
  margin: 0 auto;
  padding: 16px 20px 32px;
  background-color: transparent;
}

.app-footer {
  text-align: center;
  color: #4f4f4f;
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0.04em;
  line-height: 64px;
  border-top: 1px dashed #2c2c2c;
  background: #fffef5;
}
</style>
