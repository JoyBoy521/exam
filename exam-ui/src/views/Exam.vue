<template>
  <div class="exam-list-container">
    <div class="top-action-bar">
      <div class="left-actions">
        <el-button type="primary" color="#409eff" class="round-btn" @click="openCreateDialog">
          <el-icon style="margin-right: 4px"><Monitor /></el-icon> 发布新考试
        </el-button>
      </div>
      <div class="right-actions">
        <el-input placeholder="搜索考试名称" prefix-icon="Search" class="search-input" />
      </div>
    </div>

    <el-table :data="tableData" class="custom-table" v-loading="loading">
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTag(scope.row.status)" effect="dark" size="small">
            {{ formatStatus(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="考试名称" min-width="200" />
      <el-table-column label="多套试卷(千人千卷)" min-width="180">
        <template #default="scope">
          <el-tag type="info" size="small" v-for="pid in (scope.row.paperIds ? scope.row.paperIds.split(',') : [])" :key="pid" style="margin-right: 5px;">卷ID: {{ pid }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="考试时间" width="320">
        <template #default="scope">
          <span class="time-text">{{ formatTime(scope.row.startTime) }}</span>
          <span style="margin: 0 8px; color: #909399;">至</span>
          <span class="time-text">{{ formatTime(scope.row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="success" @click="viewSubmissions(scope.row.id)">成绩批阅</el-button>
          <el-button link type="danger" @click="revokeExam(scope.row)">撤回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="发布新考试 (支持防作弊)" width="650px">
      <el-form :model="form" label-width="100px" label-position="top">
        <el-form-item label="考试名称">
          <el-input v-model="form.title" placeholder="例如：2026级软件工程期中统一考试" />
        </el-form-item>
        
        <el-form-item label="发放班级">
          <el-select v-model="form.classId" placeholder="请选择要考试的班级" style="width: 100%">
            <el-option v-for="c in classList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="组卷模式">
          <el-radio-group v-model="form.paperMode">
            <el-radio label="standard">标准模式 (选择已有试卷/AB卷)</el-radio>
            <el-radio label="random">随机策略 (根据规则实时生成试卷)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="考试试卷" v-if="form.paperMode === 'standard'">
          <el-select v-model="form.paperIds" multiple placeholder="请选择试卷（选多套即开启随机防作弊）" style="width: 100%">
            <el-option v-for="p in paperList" :key="p.id" :label="p.title" :value="p.id" />
          </el-select>
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">注：选择多套试卷时，系统会自动为同班学生随机分配不同试卷。</div>
        </el-form-item>

        <el-form-item label="抽题规则" v-if="form.paperMode === 'random'">
          <div class="random-rules-box" style="width: 100%;">
            <div v-for="(rule, index) in form.randomRules" :key="index" style="display: flex; gap: 8px; margin-bottom: 10px;">
              <el-select v-model="rule.questionType" placeholder="题型" style="flex: 1.5;">
                <el-option label="单选题" value="SINGLE_CHOICE" />
                <el-option label="多选题" value="MULTIPLE_CHOICE" />
                <el-option label="判断题" value="TRUE_FALSE" />
                <el-option label="简答题" value="SHORT_ANSWER" />
              </el-select>
              <el-select v-model="rule.difficulty" placeholder="难度" style="flex: 1;">
                <el-option label="简单" :value="1" />
                <el-option label="中等" :value="2" />
                <el-option label="困难" :value="3" />
              </el-select>
              <el-input-number v-model="rule.count" :min="1" placeholder="数量" style="width: 90px;" :controls="false" /> 
              <span style="line-height: 32px; font-size: 13px;">题</span>
              <el-input-number v-model="rule.scorePerItem" :min="1" placeholder="分/题" style="width: 90px;" :controls="false" />
              <span style="line-height: 32px; font-size: 13px;">分</span>
              <el-button type="danger" circle icon="Delete" size="small" style="margin-left: 5px; margin-top: 4px;" @click="removeRule(index)" />
            </div>
            <el-button type="primary" plain size="small" @click="addRule" style="width: 100%;">+ 添加规则</el-button>
            <div style="margin-top: 10px; font-weight: bold; color: #f56c6c; text-align: right;">
              预计试卷总分：{{ computedTotalScore }} 分
            </div>
          </div>
        </el-form-item>

        <el-form-item label="起止时间">
          <el-date-picker
            v-model="form.timeRange" type="datetimerange" range-separator="至"
            start-placeholder="开始时间" end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateExam" :loading="btnLoading">立即发布场次</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor, Search, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const tableData = ref([])
const paperList = ref([])
const classList = ref([]) 
const loading = ref(false)
const btnLoading = ref(false)
const dialogVisible = ref(false)

const form = ref({
  title: '',
  classId: null,
  paperMode: 'standard', // 默认标准模式
  paperIds: [], 
  randomRules: [
    { questionType: 'SINGLE_CHOICE', difficulty: 1, count: 10, scorePerItem: 2 } // 默认给一条规则
  ],
  timeRange: []
})

// 计算随机组卷的总分
const computedTotalScore = computed(() => {
  return form.value.randomRules.reduce((total, rule) => {
    return total + ((rule.count || 0) * (rule.scorePerItem || 0))
  }, 0)
})

const addRule = () => {
  form.value.randomRules.push({ questionType: 'SINGLE_CHOICE', difficulty: 1, count: 5, scorePerItem: 2 })
}
const removeRule = (index) => {
  form.value.randomRules.splice(index, 1)
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '-'
const formatStatus = (s) => ({ 'NOT_STARTED': '未开始', 'ONGOING': '进行中', 'FINISHED': '已结束' }[s] || s)
const getStatusTag = (s) => ({ 'NOT_STARTED': 'info', 'ONGOING': 'success', 'FINISHED': 'danger' }[s] || '')

const fetchExams = async () => {
  loading.value = true
  try {
    tableData.value = await request.get('/teacher/exams')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = async () => {
  dialogVisible.value = true
  paperList.value = await request.get('/teacher/papers')
  classList.value = await request.get('/teacher/classes') 
}

const handleCreateExam = async () => {
  if (!form.value.title || !form.value.classId || form.value.timeRange.length < 2) {
    return ElMessage.warning('请完整填写基础信息和时间')
  }
  
  if (form.value.paperMode === 'standard' && form.value.paperIds.length === 0) {
    return ElMessage.warning('请至少选择一套试卷')
  }

  if (form.value.paperMode === 'random' && form.value.randomRules.length === 0) {
    return ElMessage.warning('请至少添加一条抽题规则')
  }
  
  btnLoading.value = true
  try {
    let finalPaperIds = form.value.paperIds

    // 【核心】如果选择了随机组卷，先去调用后端生成一张新试卷！
    if (form.value.paperMode === 'random') {
      const generatedPaperId = await request.post('/teacher/papers/random', {
        title: form.value.title + ' (随机策略卷)',
        rules: form.value.randomRules
      })
      finalPaperIds = [generatedPaperId] // 拿到新生成的试卷ID，替换上去
    }

    // 统一下发考试
    await request.post('/teacher/exams', {
      title: form.value.title,
      classId: form.value.classId,
      paperIds: finalPaperIds, // 支持数组
      startTime: form.value.timeRange[0],
      endTime: form.value.timeRange[1]
    })
    
    ElMessage.success('考试场次发布成功！')
    dialogVisible.value = false
    fetchExams()
  } catch (err) {
    // 错误处理交由 request 拦截器
  } finally {
    btnLoading.value = false
  }
}

const viewSubmissions = (examId) => {
  router.push(`/marking/${examId}`)
}

const revokeExam = async (row) => {
  if (row.status === 'FINISHED') {
    return ElMessage.warning('该考试已结束')
  }
  await request.post(`/teacher/exams/${row.id}/revoke`)
  ElMessage.success('考试已撤回')
  fetchExams()
}

onMounted(fetchExams)
</script>

<style scoped>
.exam-list-container { background-color: #fff; border-radius: 8px; padding: 24px; min-height: 80vh; }
.top-action-bar { display: flex; justify-content: space-between; margin-bottom: 24px; align-items: center;}
.round-btn { border-radius: 20px; }
.search-input { width: 200px; }
.search-input :deep(.el-input__wrapper) { border-radius: 20px; }
.time-text { font-size: 13px; color: #606266; }
.random-rules-box { background: #fafafa; padding: 15px; border-radius: 8px; border: 1px dashed #dcdfe6; }
</style>
