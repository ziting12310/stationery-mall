<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { USER_INFO_KEY } from '../utils/request'
import type { UserInfo } from '../api/auth'

const router = useRouter()

const user = ref<UserInfo | null>(null)
const loading = ref(false)

/** 角色文案：后端返回枚举名 ADMIN / NORMAL_USER */
const roleText = computed<string>(() => {
  if (!user.value) return '-'
  return user.value.role === 'ADMIN' ? '管理员' : '普通用户'
})

/**
 * 加载当前登录用户信息。
 * 说明：api-spec 未提供 /user/info 接口，登录返回的用户信息已在登录时写入 localStorage，
 * 这里直接读取本地缓存展示（路由守卫保证进入本页时一定已登录）。
 */
function loadUserInfo(): void {
  loading.value = true
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    user.value = raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadUserInfo)
</script>

<template>
  <div class="user-page jp-page" v-loading="loading">
    <el-card shadow="never" class="profile-card jp-card">
      <template #header>
        <span class="title jp-title">个人中心</span>
      </template>

      <div v-if="user" class="profile-body">
        <!-- 蓝色渐变头部 + 圆形头像 -->
        <div class="profile-banner">
          <div class="avatar-large">文</div>
          <div class="profile-meta">
            <div class="profile-name">{{ user.username }}</div>
            <div class="profile-role jp-sub">{{ roleText }}</div>
          </div>
        </div>

        <!-- 分行展示，行间分隔线，替代原生表格样式 -->
        <div class="info-list">
          <div class="info-row">
            <span class="info-label">用户 ID</span>
            <span class="info-value">{{ user.id }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ user.username }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">角色</span>
            <span class="info-value">{{ roleText }}</span>
          </div>
        </div>
      </div>

      <el-empty v-else-if="!loading" description="获取用户信息失败">
        <el-button type="primary" class="jp-btn jp-btn-primary" @click="router.push('/login')">重新登录</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<style scoped>
.user-page {
  max-width: 72rem;
  margin: 0 auto;
  padding: 8px 0 48px;
}

/* 白色纸卡 + 虚线墨边 + 硬边黄阴影 */
.profile-card {
  max-width: 680px;
  margin: 0 auto;
  border: 1px dashed #2c2c2c;
  border-radius: 4px;
  box-shadow: 2px 2px 0 #ffd93d;
  overflow: hidden;
}

.profile-card :deep(.el-card__header) {
  padding: 20px 28px;
  border-bottom: 1px dashed rgba(44, 44, 44, 0.35);
  background: #ffffff;
}

.profile-card :deep(.el-card__body) {
  padding: 28px;
}

.title {
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 0.12em;
  color: #2c2c2c;
}

.profile-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 顶部信息区：黄色半透明马克笔底 + 虚线墨边，无渐变无重阴影 */
.profile-banner {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 28px;
  border-radius: 4px;
  color: #2c2c2c;
  background: rgba(255, 217, 61, 0.2);
  border: 1px dashed rgba(44, 44, 44, 0.35);
}

.avatar-large {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 400;
  color: #2c2c2c;
  background: #ffffff;
  border: 1px dashed rgba(44, 44, 44, 0.4);
}

.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.profile-name {
  font-size: 20px;
  font-weight: 400;
  letter-spacing: 0.08em;
  color: #2c2c2c;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.profile-role {
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
  color: #5a5a5a;
}

/* 信息分行展示 */
.info-list {
  padding: 0;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 4px;
  border-bottom: 1px dashed rgba(44, 44, 44, 0.35);
  transition: all 300ms ease-out;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row:hover {
  background: #fffef5;
}

.info-label {
  color: #5a5a5a;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
}

.info-value {
  color: #2c2c2c;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.04em;
}

@media (max-width: 767px) {
  .profile-card :deep(.el-card__body) {
    padding: 20px;
  }

  .profile-banner {
    padding: 20px;
  }
}
</style>
