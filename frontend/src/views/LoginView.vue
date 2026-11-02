<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { login } from '../api/auth'
import { TOKEN_KEY, USER_INFO_KEY } from '../utils/request'

const route = useRoute()
const router = useRouter()

/** 表单引用（用于 validate 校验） */
const formRef = ref<FormInstance>()
/** 提交中状态，防止重复点击 */
const loading = ref(false)

/** 登录表单数据 */
const form = reactive({
  username: '',
  password: ''
})

/** 基础非空校验 */
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/** 登录事件：校验 → 调接口 → 存储 token → 跳转 */
async function handleLogin(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await login({ username: form.username.trim(), password: form.password })
    // token 与用户信息写入 localStorage，供请求拦截器与导航栏使用
    // 后端 LoginVO 为扁平结构（userId/username/role），这里组装成 UserInfo
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(
      USER_INFO_KEY,
      JSON.stringify({ id: data.userId, username: data.username, role: data.role })
    )
    ElMessage.success('登录成功')

    // 若由路由守卫跳转而来，登录后回到原页面
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.push(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="auth-card jp-card" shadow="never">
    <template #header>
      <div class="card-title jp-title">用户登录</div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="70px"
      @submit.prevent="handleLogin"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" clearable />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          show-password
          @keyup.enter="handleLogin"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="submit-btn jp-btn jp-btn-primary" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form-item>
    </el-form>

    <div class="switch-line jp-sub">
      还没有账号？
      <el-button link type="primary" @click="router.push('/register')">立即注册</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.auth-card {
  max-width: 460px;
  margin: 40px auto;
  background-color: #ffffff;
}

.auth-card :deep(.el-card__header) {
  padding: 24px 24px 16px;
  border-bottom: 1px dashed rgba(44, 44, 44, 0.35);
}

.card-title {
  font-size: 18px;
  font-weight: 500;
  letter-spacing: 0.14em;
  color: #2c2c2c;
}

.submit-btn {
  width: 100%;
  height: 40px;
  margin-top: 8px;
}

.switch-line {
  margin-top: 16px;
  text-align: center;
  color: #5a5a5a;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0.06em;
}
</style>
