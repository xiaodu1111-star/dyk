import axios from 'axios'
import type {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

/** localStorage 中 token 的存储键 */
export const TOKEN_KEY = 'personal_os_token'

/** 单次请求的可选行为 */
export interface RequestOptions {
  /** 为 true 时不弹全局 ElMessage，由调用方自行展示错误（如登录页内联错误条） */
  silent?: boolean
}

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// ---- 请求拦截：注入 token 头 ----
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.set('token', token)
  }
  return config
})

// ---- 响应拦截：统一拆 R ----
http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const res = response.data
    const silent = (response.config as RequestOptions).silent === true

    if (res.code === 0) {
      // 成功：直接返回 data，业务层无需再解包
      return res.data as unknown as AxiosResponse
    }

    if (res.code === 401) {
      // 未登录 / token 失效：清 token 并跳登录
      localStorage.removeItem(TOKEN_KEY)
      if (!silent) {
        ElMessage.error(res.msg || '登录已过期，请重新登录')
      }
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.msg || '未登录'))
    }

    if (!silent) {
      ElMessage.error(res.msg || '请求失败')
    }
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const silent = (error?.config as RequestOptions | undefined)?.silent === true
    const msg = error?.message || '网络异常，请稍后重试'
    if (!silent) {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

/** GET 请求，直接返回业务 data */
export function get<T>(
  url: string,
  params?: Record<string, unknown>,
  options?: RequestOptions
): Promise<T> {
  const config = { params, ...(options ?? {}) } as AxiosRequestConfig
  return http.get(url, config) as unknown as Promise<T>
}

/** POST 请求，直接返回业务 data */
export function post<T>(
  url: string,
  data?: Record<string, unknown>,
  options?: RequestOptions
): Promise<T> {
  const config = { ...(options ?? {}) } as AxiosRequestConfig
  return http.post(url, data, config) as unknown as Promise<T>
}

export default http
