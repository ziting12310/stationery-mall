import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/** localStorage 中保存 token 的 key（登录成功后写入） */
export const TOKEN_KEY = 'token'
/** localStorage 中保存当前用户信息的 key */
export const USER_INFO_KEY = 'userInfo'

/** 取出 token（未登录返回空串） */
export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 清空本地登录态 */
export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
}

/**
 * 跳转登录页（401 统一处理）
 * 使用 window.location 而非 router，避免 request.ts 与 router 循环依赖
 */
function redirectToLogin(): void {
  clearAuth()
  if (window.location.pathname !== '/login') {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

/**
 * 统一业务异常处理
 * code 约定：200 成功、401 未登录/登录失效、403 权限不足，其余为业务错误
 */
function handleBusinessError(code: number, message?: string): void {
  if (code === 401) {
    ElMessage.error('登录已失效，请重新登录')
    redirectToLogin()
    return
  }
  if (code === 403) {
    ElMessage.error(message || '无权限访问该资源')
    return
  }
  ElMessage.error(message || '操作失败，请稍后重试')
}

/** axios 实例：开发阶段可通过 VITE_API_BASE_URL 切换到 Apifox Mock 地址 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

/** 请求配置扩展：silent=true 时失败不弹全局提示，由调用方自行静默处理 */
export interface RequestConfig extends AxiosRequestConfig {
  silent?: boolean
}

/**
 * 请求拦截器：localStorage 存在 token 时自动携带
 * 按接口约定使用 `Authorization: <token>`（不加 Bearer 前缀）
 */
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器：
 * - code=200 → 直接返回 data，页面无需重复解包
 * - 401 → 清理 token 并跳登录页
 * - 403 / 其他 → ElMessage 提示并 reject
 */
service.interceptors.response.use(
  (response) => {
    const body = response.data
    // 成功码：api-spec.md 约定 code=0；Apifox Mock 可能使用 200，两者都视为成功
    if (body && (body.code === 0 || body.code === 200)) {
      return body.data
    }
    // 静默请求：不弹全局提示，直接交给调用方处理
    if ((response.config as RequestConfig).silent) {
      return Promise.reject(new Error(body?.message || '请求失败'))
    }
    const code = body?.code ?? -1
    handleBusinessError(code, body?.message)
    return Promise.reject(new Error(body?.message || '请求失败'))
  },
  (error) => {
    // 静默请求：不弹全局提示，由调用方自行处理（如订单页读取评价状态）
    if ((error.config as RequestConfig | undefined)?.silent) {
      return Promise.reject(error)
    }
    // HTTP 层错误：后端把 401/403 放在 HTTP 状态码里时，同样按业务码处理
    const status: number | undefined = error.response?.status
    const body = error.response?.data
    const code: number = body?.code ?? status ?? -1

    if (status === 401 || code === 401) {
      ElMessage.error('登录已失效，请重新登录')
      redirectToLogin()
    } else if (status === 403 || code === 403) {
      ElMessage.error(body?.message || '无权限访问该资源')
    } else if (body?.message) {
      ElMessage.error(body.message)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

/**
 * 统一请求方法（泛型返回已解包的 data）
 * 页面不直接使用 service，统一经由 api 目录调用
 */
export function request<T = unknown>(config: RequestConfig): Promise<T> {
  return service.request(config) as unknown as Promise<T>
}

export default service
