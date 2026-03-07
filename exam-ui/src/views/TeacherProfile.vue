<template>
  <div class="profile-page">
    <el-card shadow="never">
      <template #header><span class="title">个人中心</span></template>

      <el-descriptions :column="2" border v-loading="loadingInfo">
        <el-descriptions-item label="当前账号">{{ profile.username || username }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleText }}</el-descriptions-item>
        <el-descriptions-item label="显示名称">{{ profile.displayName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ profile.createTime ? String(profile.createTime).replace('T',' ').slice(0,16) : '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="12" style="margin-top:12px;">
        <el-col :span="8"><div class="kpi">考试总数 {{ profile.examCount || 0 }}</div></el-col>
        <el-col :span="8"><div class="kpi">进行中 {{ profile.ongoingExamCount || 0 }}</div></el-col>
        <el-col :span="8"><div class="kpi">待批阅 {{ profile.markingCount || 0 }}</div></el-col>
      </el-row>

      <el-divider>基础资料</el-divider>
      <el-form label-width="110px" style="max-width: 520px;">
        <el-form-item label="显示名称">
          <el-input v-model="displayName" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingBasic" @click="saveBasic">保存资料</el-button>
        </el-form-item>
      </el-form>

      <el-divider>修改密码</el-divider>
      <el-form label-width="110px" style="max-width: 520px;">
        <el-form-item label="旧密码">
          <el-input v-model="form.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">保存并重新登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:12px;">
      <template #header>
        <div class="todo-header">
          <span class="title">待做任务</span>
          <el-button size="small" @click="fetchTasks">刷新</el-button>
        </div>
      </template>
      <el-empty v-if="tasks.length === 0" description="当前暂无待处理任务" />
      <el-table v-else :data="tasks" size="small">
        <el-table-column prop="title" label="任务" min-width="160" />
        <el-table-column prop="description" label="说明" min-width="220" />
        <el-table-column prop="count" label="数量" width="90" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="primary" link @click="goTask(scope.row)">立即处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '')
const role = ref(localStorage.getItem('role') || 'TEACHER')
const saving = ref(false)
const savingBasic = ref(false)
const loadingInfo = ref(false)
const displayName = ref('')
const profile = ref({})
const tasks = ref([])
const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const roleText = computed(() => role.value === 'ADMIN' ? '管理员' : '教师')

const fetchInfo = async () => {
  loadingInfo.value = true
  try {
    profile.value = await request.get('/teacher/profile/info')
    displayName.value = profile.value.displayName || ''
  } finally {
    loadingInfo.value = false
  }
}

const saveBasic = async () => {
  if (!displayName.value || !displayName.value.trim()) {
    ElMessage.warning('显示名称不能为空')
    return
  }
  savingBasic.value = true
  try {
    await request.post('/teacher/profile/update-basic', { displayName: displayName.value.trim() })
    localStorage.setItem('username', displayName.value.trim())
    username.value = displayName.value.trim()
    ElMessage.success('资料已更新')
    await fetchInfo()
  } finally {
    savingBasic.value = false
  }
}

const submit = async () => {
  if (!form.value.oldPassword || !form.value.newPassword || !form.value.confirmPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await request.post('/teacher/profile/change-password', {
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    localStorage.clear()
    router.replace('/login')
  } finally {
    saving.value = false
  }
}

const fetchTasks = async () => {
  tasks.value = await request.get('/teacher/todo/tasks')
}

const goTask = (task) => {
  if (!task?.route) return
  router.push(task.route)
}

onMounted(async () => {
  await Promise.all([fetchInfo(), fetchTasks()])
})
</script>

<style scoped>
.title { font-weight: 600; }
.profile-page { padding: 10px; }
.todo-header { display: flex; justify-content: space-between; align-items: center; }
.kpi { border: 1px solid #ebeef5; background: #fafafa; border-radius: 8px; padding: 10px; text-align: center; }
</style>
