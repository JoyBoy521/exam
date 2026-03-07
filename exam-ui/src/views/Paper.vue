<template>
  <div class="paper-list-container">
    <div class="top-action-bar">
      <div class="left-actions">
        <el-button type="primary" color="#409eff" class="round-btn" @click="$router.push('/paper/edit')">
          <el-icon style="margin-right: 4px"><DocumentAdd /></el-icon> 手动组卷
        </el-button>
        <el-button type="success" class="round-btn" @click="dialogRandomVisible = true">
          <el-icon style="margin-right: 4px"><MagicStick /></el-icon> 随机抽题组卷
        </el-button>
      </div>
      <div class="right-actions">
        <el-input v-model="keyword" placeholder="搜索试卷名称" prefix-icon="Search" class="search-input" @keyup.enter="fetchPapers" />
        <el-button type="primary" link style="margin-left: 10px;" @click="fetchPapers">搜索</el-button>
      </div>
    </div>

    <LoadErrorBar :message="loadError" @retry="fetchPapers" />
    <el-table :data="tableData" class="custom-table" :header-cell-style="{ background: '#fafafa', color: '#606266', fontWeight: 'bold' }" v-loading="loading">
      <el-table-column label="序号" type="index" width="80" align="center" />
      <el-table-column prop="id" label="试卷ID" width="100" />
      <el-table-column prop="title" label="试卷名称" min-width="250">
        <template #default="scope">
          <span style="font-weight: 500; color: #303133;">{{ scope.row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="包含题量" width="120" align="center">
        <template #default="scope">
          <el-tag type="info" effect="plain" round>{{ scope.row.questionIds ? scope.row.questionIds.length : 0 }} 题</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="组卷方式" width="120" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.title.includes('随机') ? 'warning' : 'primary'">
            {{ scope.row.title.includes('随机') ? '随机组卷' : '手动组卷' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建者" width="120" align="center">
        <template #default>{{ currentUsername }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" align="center">
        <template #default="scope">
          {{ scope.row.createTime ? scope.row.createTime.replace('T', ' ').substring(0, 16) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary">预览</el-button>
          <el-button link type="success">去发布考试</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogRandomVisible" title="随机抽题组卷" width="500px">
      <el-form label-width="100px">
        <el-form-item label="试卷名称">
          <el-input v-model="randomForm.title" placeholder="例如：期中测试随机卷A" />
        </el-form-item>
        <el-form-item label="单选题抽题">
          <el-input-number v-model="randomForm.singleChoiceCount" :min="0" :max="50" /> <span style="margin-left:10px; color:#909399;">道</span>
        </el-form-item>
        <el-form-item label="判断题抽题">
          <el-input-number v-model="randomForm.trueFalseCount" :min="0" :max="50" /> <span style="margin-left:10px; color:#909399;">道</span>
        </el-form-item>
        <el-form-item label="简答题抽题">
          <el-input-number v-model="randomForm.shortAnswerCount" :min="0" :max="50" /> <span style="margin-left:10px; color:#909399;">道</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogRandomVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRandomPaper" :loading="btnLoading">立即生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { DocumentAdd, MagicStick, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import LoadErrorBar from '../components/LoadErrorBar.vue'

const currentUsername = ref(localStorage.getItem('username') || '管理员')
const tableData = ref([])
const loading = ref(false)
const loadError = ref('')
const btnLoading = ref(false)
const keyword = ref('')

const dialogRandomVisible = ref(false)
const randomForm = ref({
  title: '',
  singleChoiceCount: 2,
  trueFalseCount: 2,
  shortAnswerCount: 1
})

const fetchPapers = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get('/teacher/papers')
    const list = Array.isArray(res) ? res : []
    const kw = keyword.value.trim().toLowerCase()
    tableData.value = !kw ? list : list.filter(x => String(x.title || '').toLowerCase().includes(kw))
  } catch (e) {
    loadError.value = e?.response?.data?.error || '加载试卷失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => { fetchPapers() })

const submitRandomPaper = async () => {
  if (!randomForm.value.title) return ElMessage.warning('请输入试卷名称')

  const rules = []
  if (randomForm.value.singleChoiceCount > 0) {
    rules.push({ questionType: 'SINGLE_CHOICE', difficulty: null, count: randomForm.value.singleChoiceCount, scorePerItem: 5 })
  }
  if (randomForm.value.trueFalseCount > 0) {
    rules.push({ questionType: 'TRUE_FALSE', difficulty: null, count: randomForm.value.trueFalseCount, scorePerItem: 5 })
  }
  if (randomForm.value.shortAnswerCount > 0) {
    rules.push({ questionType: 'SHORT_ANSWER', difficulty: null, count: randomForm.value.shortAnswerCount, scorePerItem: 10 })
  }
  if (rules.length === 0) {
    return ElMessage.warning('请至少设置一种题型数量')
  }

  btnLoading.value = true
  try {
    await request.post('/teacher/papers/random', {
      title: randomForm.value.title,
      rules
    })
    ElMessage.success('随机试卷生成成功！')
    dialogRandomVisible.value = false
    fetchPapers()
    randomForm.value.title = ''
  } finally {
    btnLoading.value = false
  }
}
</script>

<style scoped>
.paper-list-container { background-color: #fff; border-radius: 8px; padding: 24px; min-height: calc(100vh - 100px); }
.top-action-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.round-btn { border-radius: 20px; padding: 8px 20px; font-weight: bold; letter-spacing: 1px;}
.right-actions { display: flex; align-items: center; }
.search-input { width: 220px; }
.search-input :deep(.el-input__wrapper) { border-radius: 20px; }
.custom-table { border: 1px solid #ebeef5; border-radius: 8px; overflow: hidden; }
</style>
