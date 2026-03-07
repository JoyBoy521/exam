<template>
  <div class="question-list-container">
    <div class="top-action-bar">
      <div class="left-actions">
        <el-button type="primary" color="#409eff" class="round-btn" @click="goToEdit()">
          <el-icon style="margin-right: 4px"><Plus /></el-icon> 创建题目
        </el-button>
        <el-button class="round-btn plain-btn">批量导入</el-button>
        <el-button class="round-btn plain-btn">新建文件夹</el-button>
      </div>
      <div class="right-actions">
        <el-checkbox v-model="showDetail" style="margin-right: 15px;">显示题目详情</el-checkbox>
        <el-button link type="info"><el-icon><Setting /></el-icon> 安全设置</el-button>
        <el-button link type="info"><el-icon><Menu /></el-icon> 题型管理</el-button>
        <el-button link type="info"><el-icon><DocumentCopy /></el-icon> 查重</el-button>
        <el-button link type="info"><el-icon><Upload /></el-icon> 导出全部</el-button>
        <el-input v-model="filters.keyword" placeholder="搜索题干" prefix-icon="Search" class="search-input" @keyup.enter="fetchQuestions" />
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <span class="label">课程</span>
        <el-select v-model="filters.course" placeholder="全部课程" style="width: 140px;">
          <el-option label="软件工程" value="1" />
          <el-option label="计算机网络" value="2" />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="label">题型</span>
        <el-select v-model="filters.type" placeholder="全部题型" style="width: 140px;" clearable @change="fetchQuestions">
          <el-option label="单选题" value="SINGLE_CHOICE" />
          <el-option label="多选题" value="MULTIPLE_CHOICE" />
          <el-option label="填空题" value="FILL_BLANK" />
          <el-option label="判断题" value="TRUE_FALSE" />
          <el-option label="简答题" value="SHORT_ANSWER" />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="label">知识点</span>
        <el-select v-model="filters.knowledge" placeholder="请选择" filterable style="width: 180px;">
          <el-option label="未关联知识点" value="null" />
          <el-option v-for="k in existingKnowledges" :key="k" :label="k" :value="k" />
        </el-select>
      </div>
    </div>

    <LoadErrorBar :message="loadError" @retry="fetchQuestions" />
    <div class="list-summary">全部题目 (共 {{ tableData.length }} 题)</div>

    <el-table :data="tableData" class="custom-table" :header-cell-style="{ background: '#fff', color: '#909399', fontWeight: 'normal', borderBottom: '1px solid #ebeef5' }" v-loading="loading">
      <el-table-column type="selection" width="40" />
      <el-table-column label="序号" type="index" width="60" />
      
      <el-table-column label="文件夹/题目 ↓" min-width="350">
        <template #default="scope">
          <div class="stem-text">{{ scope.row.stem }}</div>
          <div v-if="showDetail" class="detail-box">
            <div v-if="scope.row.type === 'SINGLE_CHOICE'" class="options-preview">
              <div v-for="(opt, idx) in scope.row.options" :key="idx" class="opt-line" :class="{'is-answer': getLetter(idx) === scope.row.answer}">
                {{ getLetter(idx) }}. {{ opt }}
              </div>
            </div>
            <div class="answer-preview">
              <el-tag size="small" type="success" effect="dark">正确答案</el-tag> 
              <span style="margin-left: 8px; color: #67c23a; font-weight: bold;">{{ scope.row.answer }}</span>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="type" label="题型" width="90">
        <template #default="scope">{{ formatType(scope.row.type) }}</template>
      </el-table-column>
      <el-table-column label="难易 ↓" width="80">
        <template #default="scope">{{ scope.row.difficulty === 1 ? '易' : (scope.row.difficulty === 3 ? '难' : '中') }}</template>
      </el-table-column>
      <el-table-column label="正确率 ↓" width="80">
        <template #default>14%</template>
      </el-table-column>
      
      <el-table-column label="创建者 ↓" width="100">
        <template #default>{{ currentUsername }}</template>
      </el-table-column>
      
      <el-table-column label="创建时间 ↓" width="120">
        <template #default="scope">{{ scope.row.createTime ? scope.row.createTime.split('T')[0] : '-' }}</template>
      </el-table-column>

      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" plain @click="goToEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="bottom-footer">
      <el-button link type="info" class="recycle-btn">
        <el-icon><Delete /></el-icon> 回收站
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Setting, Menu, DocumentCopy, Upload, Search, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import LoadErrorBar from '../components/LoadErrorBar.vue'

const router = useRouter()
// 动态获取登录时的用户名，如果没拿到就默认叫“管理员”
const currentUsername = ref(localStorage.getItem('username') || '管理员')

const showDetail = ref(false)
const filters = ref({ course: '', type: '', knowledge: '', keyword: '' })
const tableData = ref([])
const existingKnowledges = ref([]) 
const loading = ref(false)
const loadError = ref('')

const getLetter = (index) => String.fromCharCode(65 + index)

const formatType = (type) => {
  const map = { 'SINGLE_CHOICE': '单选题', 'MULTIPLE_CHOICE': '多选题', 'FILL_BLANK': '填空题', 'TRUE_FALSE': '判断题', 'SHORT_ANSWER': '简答题' }
  return map[type] || '单选题'
}

const fetchQuestions = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get('/teacher/questions', {
      params: { type: filters.value.type || undefined, keyword: filters.value.keyword || undefined }
    })
    tableData.value = res
    
    // 动态提取已有题目中的知识点
    const kSet = new Set()
    res.forEach(q => {
      if (q.knowledgePoints) q.knowledgePoints.forEach(k => kSet.add(k))
    })
    existingKnowledges.value = Array.from(kSet)
  } catch (error) {
    loadError.value = error?.response?.data?.error || '加载题库失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => { fetchQuestions() })

const goToEdit = (row) => {
  // 如果传了 row，说明是点击“编辑”进来的；否则是点击“创建题目”进来的
  if (row && row.id) {
    router.push(`/question/edit?id=${row.id}`)
  } else {
    router.push('/question/edit')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要彻底删除这道题目吗？', '删除警告', { 
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/teacher/questions/${id}`)
    ElMessage.success('删除成功')
    fetchQuestions()
  } catch (e) {}
}
</script>

<style scoped>
.question-list-container { background-color: #fff; border-radius: 8px; padding: 24px; min-height: calc(100vh - 100px); position: relative; }
.top-action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.round-btn { border-radius: 20px; padding: 8px 20px; }
.plain-btn { border-color: #dcdfe6; color: #606266; }
.right-actions { display: flex; align-items: center; font-size: 13px; color: #606266; }
.right-actions .el-button { color: #606266; margin-left: 12px; }
.search-input { width: 180px; margin-left: 15px; }
.search-input :deep(.el-input__wrapper) { border-radius: 20px; }
.filter-bar { display: flex; align-items: center; gap: 24px; margin-bottom: 20px; }
.filter-item { display: flex; align-items: center; }
.filter-item .label { margin-right: 12px; color: #606266; font-size: 14px; font-weight: bold; }
.list-summary { font-size: 12px; color: #909399; margin-bottom: 12px; text-align: right; }
.custom-table { border: none; font-size: 13px; }
.stem-text { color: #303133; }
.detail-box { margin-top: 12px; padding: 12px; background-color: #f8f9fa; border-radius: 4px; }
.opt-line { margin-bottom: 4px; color: #606266; font-size: 13px; }
.opt-line.is-answer { color: #67c23a; font-weight: bold; }
.answer-preview { margin-top: 10px; font-size: 13px; }
.bottom-footer { position: absolute; bottom: 20px; left: 24px; }
.recycle-btn { font-size: 14px; color: #909399; }
</style>
