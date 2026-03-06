<template>
  <div class="paper-edit-layout">
    <div class="dark-header">
      <div class="header-left" @click="$router.back()"><el-icon><Back /></el-icon> 返回</div>
      <div class="header-center">
        <el-input v-model="paperTitle" placeholder="请输入试卷标题..." class="title-input" />
      </div>
      <div class="header-right">
        <div class="score-info">总分：<span class="score-num">{{ totalScore }}</span></div>
        <el-button type="primary" :loading="saveLoading" @click="handleSavePaper">完成组卷</el-button>
      </div>
    </div>

    <div class="main-content">
      <div class="left-panel">
        <div class="panel-header">从题库选择题目</div>
        <div class="filter-section">
          <el-select v-model="query.type" placeholder="题型" size="small" style="width: 120px;" clearable @change="fetchQuestions">
            <el-option label="单选题" value="SINGLE_CHOICE" />
            <el-option label="多选题" value="MULTIPLE_CHOICE" />
            <el-option label="判断题" value="TRUE_FALSE" />
            <el-option label="简答题" value="SHORT_ANSWER" />
          </el-select>
          <el-input v-model="query.keyword" placeholder="搜索题干" size="small" style="width: 180px;" @keyup.enter="fetchQuestions" />
        </div>

        <el-scrollbar class="q-list">
          <div v-for="q in questionPool" :key="q.id" class="q-card">
            <div class="q-tag">[{{ formatType(q.type) }}]</div>
            <div class="q-stem">{{ q.stem }}</div>
            <div class="q-actions">
              <el-button 
                type="primary" 
                size="small" 
                :disabled="isSelected(q.id)"
                @click="addToPaper(q)"
              >
                {{ isSelected(q.id) ? '已加入' : '加入试卷' }}
              </el-button>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <div class="right-panel">
        <div class="panel-header">已选题目 ({{ selectedQuestions.length }})</div>
        <el-empty v-if="selectedQuestions.length === 0" description="请从左侧点击加入题目" />
        
        <el-scrollbar v-else class="selected-list">
          <div v-for="(q, index) in selectedQuestions" :key="q.id" class="selected-card">
            <div class="card-num">{{ index + 1 }}</div>
            <div class="card-main">
              <div class="card-stem">{{ q.stem }}</div>
              <div class="card-footer">
                <div class="score-setter">
                  分值：<el-input-number v-model="q.score" :min="1" size="small" style="width: 90px;" />
                </div>
                <el-button type="danger" link @click="removeFromPaper(index)">
                  <el-icon><Delete /></el-icon> 移除
                </el-button>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Back, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const paperTitle = ref('未命名试卷')
const questionPool = ref([])
const selectedQuestions = ref([])
const saveLoading = ref(false)
const query = ref({ type: '', keyword: '' })

const totalScore = computed(() => {
  return selectedQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
})

const formatType = (t) => ({ 'SINGLE_CHOICE': '单选', 'MULTIPLE_CHOICE': '多选', 'TRUE_FALSE': '判断', 'SHORT_ANSWER': '简答' }[t] || t)
const isSelected = (id) => selectedQuestions.value.some(sq => sq.id === id)

const fetchQuestions = async () => {
  const res = await request.get('/teacher/questions', { params: query.value })
  questionPool.value = res
}

const addToPaper = (q) => {
  selectedQuestions.value.push({
    id: q.id,
    stem: q.stem,
    score: 5 // 默认每题5分
  })
}

const removeFromPaper = (index) => {
  selectedQuestions.value.splice(index, 1)
}

const handleSavePaper = async () => {
  if (!paperTitle.value.trim()) return ElMessage.warning('请输入试卷标题')
  if (selectedQuestions.value.length === 0) return ElMessage.warning('试卷至少包含一道题')

  saveLoading.value = true
  try {
    // 对齐后端 CreateManualPaperRequest
    const payload = {
      title: paperTitle.value,
      questionIds: selectedQuestions.value.map(q => q.id)
    }
    await request.post('/teacher/papers/manual', payload)
    ElMessage.success('手动组卷成功！')
    router.push('/paper')
  } finally {
    saveLoading.value = false
  }
}

onMounted(fetchQuestions)
</script>

<style scoped>
.paper-edit-layout { height: 100vh; display: flex; flex-direction: column; background-color: #f5f7fa; }
.dark-header { height: 60px; background-color: #2b364b; display: flex; justify-content: space-between; align-items: center; padding: 0 24px; color: #fff; }
.title-input :deep(.el-input__wrapper) { background: transparent; box-shadow: none; border-bottom: 1px solid #4c5d7a; border-radius: 0; }
.title-input :deep(.el-input__inner) { color: #fff; font-size: 16px; }
.score-info { margin-right: 20px; font-size: 14px; }
.score-num { color: #f56c6c; font-size: 20px; font-weight: bold; }

.main-content { flex: 1; display: flex; padding: 20px; gap: 20px; overflow: hidden; }
.panel-header { font-weight: bold; padding-bottom: 15px; border-bottom: 1px solid #ebeef5; margin-bottom: 15px; color: #303133; }

.left-panel { flex: 3; background: #fff; border-radius: 8px; padding: 20px; display: flex; flex-direction: column; }
.filter-section { display: flex; gap: 10px; margin-bottom: 20px; }
.q-list { flex: 1; }
.q-card { border: 1px solid #f0f0f0; padding: 15px; border-radius: 6px; margin-bottom: 12px; display: flex; align-items: flex-start; transition: 0.3s; }
.q-card:hover { border-color: #409eff; }
.q-tag { font-size: 12px; color: #409eff; margin-right: 10px; flex-shrink: 0; margin-top: 2px; }
.q-stem { flex: 1; font-size: 14px; color: #606266; line-height: 1.6; }

.right-panel { flex: 2; background: #fff; border-radius: 8px; padding: 20px; display: flex; flex-direction: column; border-left: 4px solid #409eff; }
.selected-card { display: flex; gap: 15px; padding: 15px; background: #fcfcfc; border: 1px solid #ebeef5; border-radius: 6px; margin-bottom: 10px; }
.card-num { width: 24px; height: 24px; background: #409eff; color: #fff; border-radius: 50%; display: flex; justify-content: center; align-items: center; font-size: 12px; }
.card-main { flex: 1; }
.card-stem { font-size: 13px; color: #303133; margin-bottom: 10px; }
.card-footer { display: flex; justify-content: space-between; align-items: center; }
.score-setter { font-size: 12px; color: #909399; }
</style>
