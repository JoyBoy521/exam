<template>
  <div class="profile-page">
    <el-card shadow="never">
      <template #header><span class="title">个人资料与安全</span></template>

      <el-descriptions :column="2" border v-loading="loadingInfo">
        <el-descriptions-item label="当前账号">{{ profile.studentNo || username }}</el-descriptions-item>
        <el-descriptions-item label="角色">学生</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ profile.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ profile.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ profile.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ profile.createTime ? String(profile.createTime).replace('T',' ').slice(0,16) : '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="12" style="margin-top: 12px;">
        <el-col :span="8"><div class="kpi">考试记录 {{ profile.recordCount || 0 }}</div></el-col>
        <el-col :span="8"><div class="kpi">错题总数 {{ profile.wrongCount || 0 }}</div></el-col>
        <el-col :span="8"><div class="kpi">已掌握 {{ profile.masteredCount || 0 }}</div></el-col>
      </el-row>

      <el-divider>基础资料</el-divider>
      <el-form label-width="110px" style="max-width: 520px;">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="editForm.phone" />
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const username = ref(localStorage.getItem('username') || '')
const saving = ref(false)
const loadingInfo = ref(false)
const savingBasic = ref(false)
const profile = ref({})
const editForm = ref({ name: '', phone: '' })
const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const fetchInfo = async () => {
  loadingInfo.value = true
  try {
    profile.value = await request.get('/student/profile/info')
    editForm.value = {
      name: profile.value.name || '',
      phone: profile.value.phone || ''
    }
  } finally {
    loadingInfo.value = false
  }
}

const saveBasic = async () => {
  if (!editForm.value.name || !editForm.value.name.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  savingBasic.value = true
  try {
    await request.post('/student/profile/update-basic', {
      name: editForm.value.name.trim(),
      phone: editForm.value.phone?.trim() || ''
    })
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
    await request.post('/student/profile/change-password', {
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

onMounted(fetchInfo)
</script>

<style scoped>
.title { font-weight: 600; }
.profile-page { padding: 10px; }
.kpi { border: 1px solid #ebeef5; background: #fafafa; border-radius: 8px; padding: 10px; text-align: center; }
</style>
