<template>
  <div class="login-container">
    <el-card class="login-box" shadow="hover">
      <div class="logo-title">智能考试系统</div>
      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入账号 (学生请用学号)" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
// 默认值仅供测试，建议正式环境下清空
const form = ref({ username: '', password: '' })
const loading = ref(false)

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    return ElMessage.warning('请输入账号和密码')
  }

  loading.value = true
  try {
    // 1. 调用后端的登录接口
    const res = await request.post('/auth/login', form.value)
    
    // 2. 将登录信息完整存入浏览器缓存
    localStorage.setItem('token', res.token) 
    localStorage.setItem('username', res.username)
    localStorage.setItem('role', res.role)
    if (res.userId !== undefined && res.userId !== null) {
      localStorage.setItem('userId', String(res.userId))
    }

    ElMessage.success(`欢迎回来，${res.username}！`)

    // 3. 【关键逻辑】根据不同角色跳转到对应的门户页面
    if (res.role === 'STUDENT') {
      // 学生跳转到带有南阳师范学院样式的学生个人空间
      router.push('/student/exam') 
    } else {
      // 教师或管理员跳转到原来的考试管理中心
      router.push('/exam') 
    }
  } catch (e) {
    // 错误由拦截器统一处理，此处无需额外操作
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #2b364b;
  background-image: radial-gradient(circle at 50% 50%, #38465e 0%, #1a2235 100%);
}
.login-box {
  width: 420px;
  padding: 20px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
}
.logo-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 30px;
  letter-spacing: 2px;
}
.login-btn {
  width: 100%;
  border-radius: 8px;
  font-size: 16px;
  letter-spacing: 5px;
}
</style>