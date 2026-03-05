import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: 'http://127.0.0.1:8080/api', // 直接指向你的 Spring Boot 后端
  timeout: 5000
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
      ElMessage.error(error.response?.data?.error || '网络请求出错了')
    }
    return Promise.reject(error)
  }
)

export default request