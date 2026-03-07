<template>
  <div class="auth-page">
    <div class="brand-panel">
      <div class="brand-badge">Nanyang Normal University</div>
      <h1>智能在线考试平台</h1>
      <p>考试、监考、阅卷、复盘，一体化闭环。</p>
      <div class="brand-kpis">
        <div class="kpi"><b>稳定</b><span>自动保存与心跳保活</span></div>
        <div class="kpi"><b>公平</b><span>随机试卷与风险监测</span></div>
        <div class="kpi"><b>高效</b><span>客观题自动判分</span></div>
      </div>
    </div>

    <div class="form-panel">
      <el-card shadow="never" class="auth-card">
        <template #header>
          <div class="header-row">
            <span class="title">账号登录</span>
            <el-tag type="info" size="small">{{ identity === 'STUDENT' ? '学生端' : '教师端' }}</el-tag>
          </div>
        </template>

        <el-segmented
          v-model="identity"
          :options="[
            { label: '学生', value: 'STUDENT' },
            { label: '教师/管理员', value: 'TEACHER' }
          ]"
          style="width: 100%; margin-bottom: 14px;"
        />

        <el-segmented
          v-if="identity === 'STUDENT'"
          v-model="studentMode"
          :options="[
            { label: '登录', value: 'LOGIN' },
            { label: '注册', value: 'REGISTER' }
          ]"
          style="width: 100%; margin-bottom: 14px;"
        />

        <el-form
          v-if="identity !== 'STUDENT' || studentMode === 'LOGIN'"
          ref="loginRef"
          :model="loginForm"
          :rules="loginRules"
          size="large"
        >
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" :prefix-icon="User" :placeholder="identity === 'STUDENT' ? '请输入学号' : '请输入教师账号'" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" :prefix-icon="Lock" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loadingLogin" @click="handleLogin">
              {{ loadingLogin ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <el-form
          v-else
          ref="registerRef"
          :model="registerForm"
          :rules="registerRules"
          size="large"
        >
          <el-form-item prop="studentNo">
            <el-input v-model="registerForm.studentNo" :prefix-icon="User" placeholder="学号（4-32位）" />
          </el-form-item>
          <el-form-item prop="name">
            <el-input v-model="registerForm.name" placeholder="姓名" />
          </el-form-item>
          <el-form-item prop="classId">
            <el-select v-model="registerForm.classId" placeholder="选择班级" style="width: 100%;" filterable>
              <el-option v-for="c in classOptions" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="registerForm.password" type="password" :prefix-icon="Lock" show-password placeholder="密码（至少6位）" />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" :prefix-icon="Lock" show-password placeholder="确认密码" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loadingRegister" @click="handleRegister">
              {{ loadingRegister ? '注册中...' : '注册并返回登录' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const identity = ref('STUDENT')
const studentMode = ref('LOGIN')
const classOptions = ref([])

const loginRef = ref(null)
const loadingLogin = ref(false)
const loginForm = reactive({
  username: '',
  password: ''
})
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRef = ref(null)
const loadingRegister = ref(false)
const registerForm = reactive({
  studentNo: '',
  name: '',
  classId: null,
  password: '',
  confirmPassword: ''
})
const registerRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{
    validator: (_, value, callback) => {
      if (!value) {
        callback(new Error('请确认密码'))
        return
      }
      if (value !== registerForm.password) {
        callback(new Error('两次密码输入不一致'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }]
}

const fetchClasses = async () => {
  try {
    classOptions.value = await request.get('/auth/classes')
  } catch {
    classOptions.value = []
  }
}

const handleLogin = async () => {
  if (!loginRef.value) return
  await loginRef.value.validate(async (valid) => {
    if (!valid) return
    loadingLogin.value = true
    try {
      const res = await request.post('/auth/login', {
        username: loginForm.username.trim(),
        password: loginForm.password
      })
      localStorage.setItem('token', res.token)
      localStorage.setItem('role', res.role)
      localStorage.setItem('username', res.username || '')

      ElMessage.success('登录成功')
      if (res.role === 'STUDENT') {
        router.replace('/student/exam')
      } else {
        router.replace('/exam')
      }
    } finally {
      loadingLogin.value = false
    }
  })
}

const handleRegister = async () => {
  if (!registerRef.value) return
  await registerRef.value.validate(async (valid) => {
    if (!valid) return
    loadingRegister.value = true
    try {
      await request.post('/auth/register', {
        studentNo: registerForm.studentNo.trim(),
        name: registerForm.name.trim(),
        classId: registerForm.classId,
        password: registerForm.password
      })
      ElMessage.success('注册成功，请登录')
      studentMode.value = 'LOGIN'
      loginForm.username = registerForm.studentNo.trim()
      loginForm.password = ''
      registerForm.studentNo = ''
      registerForm.name = ''
      registerForm.classId = null
      registerForm.password = ''
      registerForm.confirmPassword = ''
    } finally {
      loadingRegister.value = false
    }
  })
}

watch(identity, (val) => {
  if (val !== 'STUDENT') {
    studentMode.value = 'LOGIN'
  }
})

watch([identity, studentMode], ([id, mode]) => {
  if (id === 'STUDENT' && mode === 'REGISTER' && classOptions.value.length === 0) {
    fetchClasses()
  }
})

onMounted(() => {
  if (identity.value === 'STUDENT' && studentMode.value === 'REGISTER') {
    fetchClasses()
  }
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  background: radial-gradient(circle at 0% 0%, #1d3566 0, #0f2144 40%, #09162f 100%);
}
.brand-panel {
  color: #e8f0ff;
  padding: 56px 56px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.brand-badge {
  width: fit-content;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  margin-bottom: 16px;
}
.brand-panel h1 { margin: 0; font-size: 42px; line-height: 1.2; }
.brand-panel p { margin: 12px 0 0; font-size: 16px; color: #b9caef; }
.brand-kpis { margin-top: 28px; display: grid; gap: 12px; }
.kpi { background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.12); border-radius: 12px; padding: 12px 14px; }
.kpi b { display: block; margin-bottom: 2px; }
.kpi span { color: #c6d4f2; font-size: 13px; }

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.auth-card {
  width: min(460px, 100%);
  border-radius: 16px;
  border: none;
}
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title { font-size: 20px; font-weight: 700; }
.submit-btn { width: 100%; }

@media (max-width: 980px) {
  .auth-page { grid-template-columns: 1fr; }
  .brand-panel { padding: 24px 24px 8px; }
  .brand-panel h1 { font-size: 28px; }
}
</style>
