import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL: API_BASE,
  timeout: Number(import.meta.env.VITE_API_TIMEOUT || 10000)
})

// 请求拦截器：如果本地存了 Token，每次发请求都偷偷塞进请求头里
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理后端的报错
request.interceptors.response.use(
  response => response.data, // 直接返回真实数据，剥离 axios 的外层包装
  error => {
    // 如果是 401，说明没登录或 Token 过期，直接踢回登录页
    if (error.response?.status === 401) {
      ElMessage.warning('请先登录！')
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      if (!error.response) {
        const msg = error.code === 'ECONNABORTED' ? '请求超时，请检查后端服务' : '网络不可用或后端未启动'
        ElMessage.error(msg)
      } else {
        ElMessage.error(error.response?.data?.error || '网络请求出错了')
      }
    }
    return Promise.reject(error)
  }
)

export const getWsBase = () => {
  const apiBase = request.defaults.baseURL || '/api'
  if (/^https?:\/\//i.test(apiBase)) {
    return apiBase.replace(/^http/i, 'ws').replace(/\/api\/?$/, '')
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}`
}

export default request
