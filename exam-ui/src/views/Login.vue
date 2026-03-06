<template>
  <div class="login-wrapper">
    <div class="login-container">
      <div class="login-left">
        <div class="left-content">
          <img src="https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg" alt="logo" class="logo" />
          <h2>智能在线考试平台</h2>
          <p>公平 · 高效 · 智能的现代化教务系统</p>
        </div>
      </div>

      <div class="login-right">
        <div class="form-box">
          <h3 class="title">欢迎登录</h3>
          
          <el-tabs v-model="loginForm.role" class="role-tabs">
            <el-tab-pane label="🎓 学生端" name="STUDENT"></el-tab-pane>
            <el-tab-pane label="👨‍🏫 教师/管理端" name="TEACHER"></el-tab-pane>
          </el-tabs>

          <el-form :model="loginForm" :rules="rules" ref="loginRef" size="large">
            <el-form-item prop="username">
              <el-input 
                v-model="loginForm.username" 
                :prefix-icon="User" 
                placeholder="请输入学号 / 工号"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            
            <el-form-item prop="password">
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                :prefix-icon="Lock" 
                placeholder="请输入密码" 
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item>
              <el-button 
                type="primary" 
                class="submit-btn" 
                :loading="loading" 
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request' // 确保你的 axios 实例路径正确

const router = useRouter()
const loginRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  role: 'STUDENT'
})

const rules = {
  username: [{ required: true, message: '账号不能为空', trigger: 'blur' }],
  password: [{ required: true, message: '密码不能为空', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!loginRef.value) return
  await loginRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 请求后端的 AuthController
        const res = await request.post('/auth/login', {
          username: loginForm.username,
          password: loginForm.password
        })
        
        localStorage.setItem('token', res.token)
        localStorage.setItem('role', res.role)
        localStorage.setItem('username', res.displayName)
        
        ElMessage.success('登录成功！')
        
        // 根据实际返回的角色和选择进行跳转拦截
        if (res.role === 'STUDENT') {
          router.push('/student/exam')
        } else {
          router.push('/exam') // 教师端管理页面
        }
      } catch (error) {
        // 错误提示由 request.js 拦截器统一处理，这里可不做额外操作
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-wrapper {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
}

.login-container {
  display: flex;
  width: 900px;
  height: 500px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #4D58B5 0%, #303f9f 100%);
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

/* 左侧装饰小球 */
.login-left::before {
  content: '';
  position: absolute;
  top: -50px;
  left: -50px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}
.login-left::after {
  content: '';
  position: absolute;
  bottom: -80px;
  right: -50px;
  width: 250px;
  height: 250px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

.left-content {
  text-align: center;
  z-index: 1;
}

.logo {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 4px solid rgba(255, 255, 255, 0.3);
  margin-bottom: 20px;
}

.left-content h2 {
  font-size: 28px;
  margin: 0 0 10px;
  letter-spacing: 2px;
}
.left-content p {
  font-size: 14px;
  color: #d0d4ff;
  margin: 0;
}

.login-right {
  flex: 1;
  padding: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.title {
  font-size: 26px;
  color: #333;
  margin-bottom: 20px;
  font-weight: 600;
}

.role-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

.submit-btn {
  width: 100%;
  border-radius: 8px;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 10px;
  height: 44px;
  background: linear-gradient(135deg, #4D58B5 0%, #303f9f 100%);
  border: none;
  transition: all 0.3s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(77, 88, 181, 0.4);
}
</style>