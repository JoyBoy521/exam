<template>
  <el-card shadow="never">
    <template #header>
      <span style="font-weight:600;">管理中心</span>
    </template>

    <el-tabs v-model="active">
      <el-tab-pane label="补考审批" name="makeup">
        <div class="toolbar">
          <el-input v-model="makeupFilters.keyword" clearable placeholder="搜索考试/学号/姓名/原因" style="width: 280px;" @clear="onMakeupFilterChange" />
          <el-button type="primary" plain @click="onMakeupFilterChange">筛选</el-button>
        </div>
        <el-table :data="makeupList" v-loading="loadingMakeup">
          <el-table-column prop="examTitle" label="考试" min-width="180" />
          <el-table-column prop="studentNo" label="学号" width="140" />
          <el-table-column prop="studentName" label="姓名" width="120" />
          <el-table-column prop="reason" label="申请原因" min-width="220" />
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="makeupStatus(scope.row.status).tag">{{ makeupStatus(scope.row.status).text }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <el-button size="small" type="success" @click="review(scope.row, 'APPROVED')" :disabled="scope.row.status !== 'PENDING'">批准</el-button>
              <el-button size="small" type="danger" @click="review(scope.row, 'REJECTED')" :disabled="scope.row.status !== 'PENDING'">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="makeupPager.page"
          v-model:page-size="makeupPager.size"
          :total="makeupPager.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 12px; justify-content: flex-end;"
          @size-change="fetchMakeup"
          @current-change="fetchMakeup"
        />
      </el-tab-pane>

      <el-tab-pane label="平行组" name="parallel">
        <el-table :data="parallelGroups" v-loading="loadingGroup">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="name" label="组名" min-width="200" />
          <el-table-column label="试卷ID" min-width="220">
            <template #default="scope">{{ (scope.row.paperIds || []).join(', ') }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="isAdmin" label="账号管理" name="accounts">
        <div class="toolbar">
          <el-button type="primary" @click="openCreate">新增账号</el-button>
          <el-button @click="fetchAccounts">刷新</el-button>
          <el-select v-model="accountFilters.role" clearable placeholder="角色" style="width: 110px;" @change="onAccountFilterChange">
            <el-option label="TEACHER" value="TEACHER" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
          <el-select v-model="accountFilters.status" clearable placeholder="状态" style="width: 110px;" @change="onAccountFilterChange">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
          <el-input v-model="accountFilters.keyword" clearable placeholder="搜索用户名/姓名" style="width: 220px;" @clear="onAccountFilterChange" />
          <el-button type="primary" plain @click="onAccountFilterChange">筛选</el-button>
        </div>
        <el-table :data="accounts" v-loading="loadingAccount">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" min-width="150" />
          <el-table-column prop="displayName" label="姓名" min-width="140" />
          <el-table-column prop="role" label="角色" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'primary'">
                {{ scope.row.role }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
                {{ scope.row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="scope">
              <el-button
                size="small"
                :type="scope.row.status === 1 ? 'warning' : 'success'"
                @click="toggleStatus(scope.row)"
              >
                {{ scope.row.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button size="small" @click="resetPassword(scope.row)">重置密码</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="accountPager.page"
          v-model:page-size="accountPager.size"
          :total="accountPager.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 12px; justify-content: flex-end;"
          @size-change="fetchAccounts"
          @current-change="fetchAccounts"
        />
      </el-tab-pane>
    </el-tabs>
    <LoadErrorBar :message="loadError" @retry="handleRetry" />
  </el-card>

  <el-dialog v-model="createVisible" title="新增账号" width="460px">
    <el-form :model="createForm" :rules="rules" ref="createFormRef" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="createForm.username" placeholder="如 teacher02" />
      </el-form-item>
      <el-form-item label="姓名" prop="displayName">
        <el-input v-model="createForm.displayName" placeholder="如 李老师" />
      </el-form-item>
      <el-form-item label="角色" prop="role">
        <el-select v-model="createForm.role" style="width: 100%;">
          <el-option label="TEACHER" value="TEACHER" />
          <el-option label="ADMIN" value="ADMIN" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import LoadErrorBar from '../components/LoadErrorBar.vue'
import { makeupStatusMeta } from '../utils/format'

const active = ref('makeup')
const makeupList = ref([])
const parallelGroups = ref([])
const accounts = ref([])
const loadingMakeup = ref(false)
const loadingGroup = ref(false)
const loadingAccount = ref(false)
const loadError = ref('')
const isAdmin = localStorage.getItem('role') === 'ADMIN'
const makeupPager = ref({ page: 1, size: 10, total: 0 })
const accountPager = ref({ page: 1, size: 10, total: 0 })
const makeupFilters = ref({ keyword: '' })
const accountFilters = ref({ keyword: '', role: '', status: null })

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = ref({
  username: '',
  displayName: '',
  role: 'TEACHER'
})
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const fetchMakeup = async () => {
  loadingMakeup.value = true
  loadError.value = ''
  try {
    const res = await request.get('/teacher/makeup-requests', {
      params: {
        page: makeupPager.value.page,
        size: makeupPager.value.size,
        keyword: makeupFilters.value.keyword?.trim() || undefined
      }
    })
    makeupList.value = res.list || []
    makeupPager.value.total = Number(res.total || 0)
  } catch (e) {
    loadError.value = e?.response?.data?.error || '加载补考申请失败'
  } finally {
    loadingMakeup.value = false
  }
}

const fetchGroups = async () => {
  loadingGroup.value = true
  loadError.value = ''
  try {
    parallelGroups.value = await request.get('/teacher/parallel-groups')
  } catch (e) {
    loadError.value = e?.response?.data?.error || '加载平行组失败'
  } finally {
    loadingGroup.value = false
  }
}

const fetchAccounts = async () => {
  if (!isAdmin) return
  loadingAccount.value = true
  loadError.value = ''
  try {
    const res = await request.get('/teacher/accounts', {
      params: {
        page: accountPager.value.page,
        size: accountPager.value.size,
        keyword: accountFilters.value.keyword?.trim() || undefined,
        role: accountFilters.value.role || undefined,
        status: accountFilters.value.status == null ? undefined : accountFilters.value.status
      }
    })
    accounts.value = res.list || []
    accountPager.value.total = Number(res.total || 0)
  } catch (e) {
    loadError.value = e?.response?.data?.error || '加载账号列表失败'
  } finally {
    loadingAccount.value = false
  }
}

const review = async (row, status) => {
  await request.post(`/teacher/makeup-requests/${row.id}/review`, {
    status,
    teacherComment: status === 'APPROVED' ? '审核通过' : '审核未通过',
    approvedExtraMinutes: status === 'APPROVED' ? 20 : null
  })
  ElMessage.success('已处理')
  fetchMakeup()
}

const onMakeupFilterChange = () => {
  makeupPager.value.page = 1
  fetchMakeup()
}

const openCreate = () => {
  createForm.value = { username: '', displayName: '', role: 'TEACHER' }
  createVisible.value = true
}

const submitCreate = async () => {
  await createFormRef.value?.validate()
  creating.value = true
  try {
    await request.post('/teacher/accounts', createForm.value)
    ElMessage.success('账号创建成功，默认密码 123456')
    createVisible.value = false
    fetchAccounts()
  } finally {
    creating.value = false
  }
}

const toggleStatus = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  await request.put(`/teacher/accounts/${row.id}/status`, { status: nextStatus })
  ElMessage.success('状态已更新')
  fetchAccounts()
}

const onAccountFilterChange = () => {
  accountPager.value.page = 1
  fetchAccounts()
}

const makeupStatus = (status) => makeupStatusMeta(status)

const handleRetry = () => {
  fetchMakeup()
  fetchGroups()
  fetchAccounts()
}

const resetPassword = async (row) => {
  try {
    await ElMessageBox.confirm(`确认将 ${row.username} 的密码重置为 123456 吗？`, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.post(`/teacher/accounts/${row.id}/reset-password`)
    ElMessage.success('密码已重置为 123456')
  } catch {
    // 取消操作不提示错误
  }
}

onMounted(() => {
  fetchMakeup()
  fetchGroups()
  fetchAccounts()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
}
</style>
