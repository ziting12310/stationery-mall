<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '../api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

/** 注册表单数据 */
const form = reactive({
  username: '',
  password: ''
})

/** 基础非空校验 */
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/** 注册事件：校验 → 调接口 → 成功后跳登录页 */
async function handleRegister(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({ username: form.username.trim(), password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="auth-card jp-card" shadow="never">
    <template #header>
      <div class="card-title jp-title">用户注册</div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="70px"
      @submit.prevent="handleRegister"
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
          @keyup.enter="handleRegister"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="submit-btn jp-btn jp-btn-primary" :loading="loading" @click="handleRegister">
          注册
        </el-button>
      </el-form-item>
    </el-form>

    <div class="switch-line jp-sub">
      已有账号？
      <el-button link type="primary" @click="router.push('/login')">返回登录</el-button>
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
