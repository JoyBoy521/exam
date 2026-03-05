<template>
  <div class="class-manage-container">
    <div class="left-sidebar">
      <div class="sidebar-header">
        <span class="title">我的班级</span>
        <el-button type="primary" link @click="openCreateClassDialog">
          <el-icon><Plus /></el-icon> 新建
        </el-button>
      </div>
      <div class="sidebar-search">
        <el-input v-model="classKeyword" placeholder="搜索班级" :prefix-icon="Search" clearable />
      </div>

      <div class="class-list">
        <div
          class="class-item"
          v-for="item in filteredClasses"
          :key="item.id"
          :class="{ 'is-active': activeClassId === item.id }"
          @click="selectClass(item)"
        >
          <div class="class-icon"><el-icon><Collection /></el-icon></div>
          <div class="class-info">
            <div class="class-name" :title="item.name">{{ item.name }}</div>
            <div class="class-count">{{ item.studentCount }} 人</div>
          </div>
          <el-dropdown trigger="click" @command="(cmd) => handleClassCommand(cmd, item)" @click.stop>
            <el-icon class="more-icon"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="edit">重命名</el-dropdown-item>
                <el-dropdown-item command="delete" style="color: #f56c6c;">解散班级</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <div class="right-main">
      <div v-if="activeClass" class="main-content">
        <div class="main-header">
          <div class="class-title">
            {{ activeClass.name }} <el-tag size="small" type="info" style="margin-left: 10px;">共 {{ filteredStudents.length }} 人</el-tag>
          </div>
          <div class="actions">
            <el-button type="primary" plain @click="dialogStudentVisible = true">
              <el-icon><User /></el-icon> 添加学生
            </el-button>
            <el-button type="success" plain @click="openBatchDialog">
              <el-icon><Upload /></el-icon> 批量导入
            </el-button>
            <el-button type="info" plain @click="exportStudents">
              导出名单
            </el-button>
            <el-input v-model="studentKeyword" placeholder="搜索姓名/学号" :prefix-icon="Search" style="width: 220px; margin-left: 15px;" clearable />
          </div>
        </div>

        <el-table :data="pagedStudents" class="custom-table" :header-cell-style="{ background: '#f5f7fa', color: '#606266' }">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="studentNo" label="学号" width="150" />
          <el-table-column prop="name" label="姓名" width="150">
            <template #default="scope">
              <span style="font-weight: bold; color: #303133;">{{ scope.row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="联系电话" width="180" />
          <el-table-column label="加入时间" min-width="180">
            <template #default="scope">
              {{ formatDateTime(scope.row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openEditStudentDialog(scope.row)">编辑</el-button>
              <el-button link type="warning" @click="resetPassword(scope.row.id)">重置密码</el-button>
              <el-button link type="danger" @click="removeStudent(scope.row.id)">移出班级</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-footer">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="filteredStudents.length"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
          />
        </div>
      </div>

      <el-empty v-else description="请在左侧选择或创建一个班级" style="margin-top: 100px;" />
    </div>

    <el-dialog v-model="dialogClassVisible" :title="classDialogMode === 'edit' ? '重命名班级' : '新建班级'" width="420px">
      <el-form label-width="88px">
        <el-form-item label="班级名称">
          <el-input v-model="classForm.name" placeholder="例如：2026级软件工程1班" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogClassVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClass">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogStudentVisible" title="添加单个学生" width="420px">
      <el-form label-width="88px">
        <el-form-item label="学生学号">
          <el-input v-model="studentForm.studentNo" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="学生姓名">
          <el-input v-model="studentForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="studentForm.phone" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogStudentVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStudent">确认添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogEditStudentVisible" title="编辑学生信息" width="420px">
      <el-form label-width="88px">
        <el-form-item label="学号">
          <el-input v-model="editStudentForm.studentNo" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editStudentForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="editStudentForm.phone" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogEditStudentVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEditStudent">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogBatchVisible" title="批量导入学生" width="560px">
      <el-alert title="每行一个学生，格式：学号,姓名,联系电话（联系电话可留空）" type="info" :closable="false" style="margin-bottom: 12px;" />
      <div style="margin-bottom: 10px; display: flex; justify-content: space-between;">
        <div>
          <el-button type="primary" plain @click="triggerBatchFileSelect">上传 CSV 文件</el-button>
          <el-button style="margin-left: 8px;" plain @click="downloadBatchTemplate">下载模板</el-button>
        </div>
        <span style="font-size: 12px; color: #909399;">支持 .csv / .txt</span>
      </div>
      <input ref="batchFileInputRef" type="file" accept=".csv,.txt" style="display:none" @change="handleBatchFileChange" />
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="12"
        placeholder="20260010,赵六,13800000000\n20260011,孙七,"
      />
      <el-alert
        v-if="batchErrors.length"
        style="margin-top: 12px;"
        :title="`有 ${batchErrors.length} 行导入失败，请核对后重试`"
        type="warning"
        :closable="false"
      />
      <el-table v-if="batchErrors.length" :data="batchErrors" size="small" max-height="180" style="margin-top: 8px;">
        <el-table-column prop="lineNo" label="行号" width="70" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="reason" label="失败原因" />
      </el-table>
      <template #footer>
        <el-button @click="dialogBatchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Plus, Collection, MoreFilled, User, Upload, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const classList = ref([])
const activeClassId = ref(null)
const activeClass = computed(() => classList.value.find(c => c.id === activeClassId.value))
const studentList = ref([])
const studentKeyword = ref('')
const classKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredStudents = computed(() => {
  const kw = studentKeyword.value.trim().toLowerCase()
  if (!kw) return studentList.value
  return studentList.value.filter(s =>
    (s.name || '').toLowerCase().includes(kw) ||
    (s.studentNo || '').toLowerCase().includes(kw)
  )
})

const filteredClasses = computed(() => {
  const kw = classKeyword.value.trim().toLowerCase()
  if (!kw) return classList.value
  return classList.value.filter(c => (c.name || '').toLowerCase().includes(kw))
})

const pagedStudents = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredStudents.value.slice(start, end)
})

watch(filteredStudents, () => {
  currentPage.value = 1
})

const fetchClasses = async () => {
  classList.value = await request.get('/teacher/classes')
  if (classList.value.length > 0) {
    if (!activeClassId.value || !classList.value.find(c => c.id === activeClassId.value)) {
      await selectClass(classList.value[0])
    }
  } else {
    activeClassId.value = null
    studentList.value = []
  }
}

const selectClass = async (item) => {
  activeClassId.value = item.id
  studentList.value = await request.get(`/teacher/classes/${item.id}/students`)
  currentPage.value = 1
}

onMounted(fetchClasses)

const dialogClassVisible = ref(false)
const classDialogMode = ref('create')
const editingClassId = ref(null)
const classForm = ref({ name: '' })

const openCreateClassDialog = () => {
  classDialogMode.value = 'create'
  editingClassId.value = null
  classForm.value = { name: '' }
  dialogClassVisible.value = true
}

const saveClass = async () => {
  if (!classForm.value.name || !classForm.value.name.trim()) {
    return ElMessage.warning('请输入班级名称')
  }
  if (classDialogMode.value === 'edit' && editingClassId.value) {
    await request.put(`/teacher/classes/${editingClassId.value}`, { name: classForm.value.name.trim() })
    ElMessage.success('班级名称已更新')
  } else {
    await request.post('/teacher/classes', { name: classForm.value.name.trim() })
    ElMessage.success('班级创建成功')
  }
  dialogClassVisible.value = false
  await fetchClasses()
}

const handleClassCommand = (cmd, item) => {
  if (cmd === 'edit') {
    classDialogMode.value = 'edit'
    editingClassId.value = item.id
    classForm.value = { name: item.name }
    dialogClassVisible.value = true
    return
  }

  if (cmd === 'delete') {
    ElMessageBox.confirm('解散后班级内所有学生数据会被删除，确认解散吗？', '严重警告', { type: 'error' }).then(async () => {
      await request.delete(`/teacher/classes/${item.id}`)
      ElMessage.success('班级已解散')
      if (activeClassId.value === item.id) {
        activeClassId.value = null
      }
      await fetchClasses()
    }).catch(() => {})
  }
}

const dialogStudentVisible = ref(false)
const studentForm = ref({ studentNo: '', name: '', phone: '' })

const saveStudent = async () => {
  if (!activeClassId.value) {
    return ElMessage.warning('请先选择班级')
  }
  if (!studentForm.value.studentNo || !studentForm.value.name) {
    return ElMessage.warning('请填写完整信息')
  }
  await request.post('/teacher/students', {
    classId: activeClassId.value,
    studentNo: studentForm.value.studentNo.trim(),
    name: studentForm.value.name.trim(),
    phone: (studentForm.value.phone || '').trim() || null
  })
  ElMessage.success('添加学生成功（初始密码：123456）')
  dialogStudentVisible.value = false
  studentForm.value = { studentNo: '', name: '', phone: '' }
  await selectClass(activeClass.value)
  await fetchClasses()
}

const dialogBatchVisible = ref(false)
const batchText = ref('')
const batchFileInputRef = ref(null)
const batchErrors = ref([])

const submitBatch = async () => {
  if (!activeClassId.value) {
    return ElMessage.warning('请先选择班级')
  }
  const lines = batchText.value.split('\n').map(x => x.trim()).filter(Boolean)
  if (lines.length === 0) {
    return ElMessage.warning('请至少输入一行数据')
  }

  const contentLines = (lines[0].includes('学号') && lines[0].includes('姓名')) ? lines.slice(1) : lines
  if (contentLines.length === 0) {
    return ElMessage.warning('未检测到可导入的数据行')
  }
  const students = contentLines.map(line => {
    const parts = line.split(/[,\t，]/)
    return {
      studentNo: (parts[0] || '').trim(),
      name: (parts[1] || '').trim(),
      phone: (parts[2] || '').trim()
    }
  })

  const res = await request.post('/teacher/students/batch', {
    classId: activeClassId.value,
    students
  })
  batchErrors.value = res.errors || []
  ElMessage.success(`导入完成：成功 ${res.success || 0}，跳过 ${res.skipped || 0}`)
  await selectClass(activeClass.value)
  await fetchClasses()
  if ((res.skipped || 0) === 0) {
    dialogBatchVisible.value = false
    batchText.value = ''
    batchErrors.value = []
  }
}

const triggerBatchFileSelect = () => {
  batchFileInputRef.value?.click()
}

const handleBatchFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  const text = await file.text()
  const normalized = text.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n')
  batchText.value = normalized
  event.target.value = ''
}

const openBatchDialog = () => {
  dialogBatchVisible.value = true
  batchText.value = ''
  batchErrors.value = []
}

const downloadBatchTemplate = () => {
  const content = '学号,姓名,联系电话\n20260010,赵六,13800000000\n20260011,孙七,\n'
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'student_import_template.csv'
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

const removeStudent = (id) => {
  ElMessageBox.confirm('确认将该学生移出本班级吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/teacher/students/${id}`)
    ElMessage.success('已移出')
    await selectClass(activeClass.value)
    await fetchClasses()
  }).catch(() => {})
}

const formatDateTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

const dialogEditStudentVisible = ref(false)
const editStudentForm = ref({ id: null, studentNo: '', name: '', phone: '' })

const openEditStudentDialog = (row) => {
  editStudentForm.value = {
    id: row.id,
    studentNo: row.studentNo || '',
    name: row.name || '',
    phone: row.phone || ''
  }
  dialogEditStudentVisible.value = true
}

const saveEditStudent = async () => {
  if (!editStudentForm.value.name || !editStudentForm.value.name.trim()) {
    return ElMessage.warning('姓名不能为空')
  }
  await request.put(`/teacher/students/${editStudentForm.value.id}`, {
    name: editStudentForm.value.name.trim(),
    phone: (editStudentForm.value.phone || '').trim() || null
  })
  ElMessage.success('学生信息已更新')
  dialogEditStudentVisible.value = false
  await selectClass(activeClass.value)
}

const resetPassword = (id) => {
  ElMessageBox.confirm('确认重置该学生密码为 123456 吗？', '提示', { type: 'warning' }).then(async () => {
    const msg = await request.post(`/teacher/students/${id}/reset-password`)
    ElMessage.success(msg || '密码已重置')
  }).catch(() => {})
}

const exportStudents = async () => {
  if (!activeClassId.value) {
    return ElMessage.warning('请先选择班级')
  }
  const blob = await request.get(`/teacher/classes/${activeClassId.value}/students/export`, {
    responseType: 'blob'
  })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${activeClass.value?.name || 'class'}_students.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}
</script>

<style scoped>
.class-manage-container {
  display: flex;
  height: calc(100vh - 100px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.left-sidebar {
  width: 260px;
  background: #fafafa;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #ebeef5;
}
.sidebar-header .title { font-size: 16px; font-weight: bold; color: #303133; }
.sidebar-search { padding: 10px; border-bottom: 1px solid #ebeef5; }
.class-list { flex: 1; overflow-y: auto; padding: 10px; }
.class-item {
  display: flex;
  align-items: center;
  padding: 12px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: 0.2s;
  margin-bottom: 5px;
}
.class-item:hover { background: #f0f2f5; }
.class-item.is-active { background: #ebf3ff; color: #409eff; }
.class-icon {
  width: 32px;
  height: 32px;
  background: #fff;
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #409eff;
  margin-right: 10px;
  border: 1px solid #dcdfe6;
}
.class-item.is-active .class-icon { background: #409eff; color: #fff; border-color: #409eff; }
.class-info { flex: 1; overflow: hidden; }
.class-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}
.class-count { font-size: 12px; color: #909399; }
.more-icon { color: #909399; padding: 5px; border-radius: 4px; }
.more-icon:hover { background: #dcdfe6; }

.right-main { flex: 1; display: flex; flex-direction: column; }
.main-content { display: flex; flex-direction: column; height: 100%; }
.main-header {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid #ebeef5;
}
.class-title { font-size: 18px; font-weight: bold; color: #303133; }
.actions { display: flex; align-items: center; }
.custom-table { flex: 1; padding: 20px; }
.table-footer {
  padding: 0 20px 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
