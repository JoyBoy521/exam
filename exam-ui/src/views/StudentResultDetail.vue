<template>
  <div class="detail-container" v-loading="loading">
    <el-page-header @back="$router.back()" :title="detail.examTitle">
      <template #content>
        <span class="text-large font-600 mr-3"> 答卷详情 </span>
        <el-tag type="danger" size="large" effect="dark">总分：{{ detail.totalScore }}</el-tag>
      </template>
    </el-page-header>

    <div class="question-list">
      <el-card v-for="(item, index) in detail.details" :key="index" class="q-card">
        <div class="q-header">
          <el-tag size="small">{{ formatType(item.type) }}</el-tag>
          <span class="q-index">第 {{ index + 1 }} 题</span>
          <el-icon v-if="item.isCorrect === 1" color="#67c23a" size="24"><CircleCheck /></el-icon>
          <el-icon v-else color="#f56c6c" size="24"><CircleClose /></el-icon>
          
          <el-button 
            type="warning" icon="Star" circle size="small" 
            style="margin-left: auto;" 
            title="加入错题本"
            @click="openCollectDialog(item)"
          />
        </div>
        
        <p class="q-stem">{{ item.stem }}</p>

        <div v-if="item.options" class="options-box">
          <div v-for="(opt, idx) in item.options" :key="idx" 
               class="opt-item"
               :class="{ 
                 'is-correct': getLetter(idx) === item.correctAnswer,
                 'is-wrong': getLetter(idx) === item.studentAnswer && item.studentAnswer !== item.correctAnswer 
               }">
            {{ getLetter(idx) }}. {{ opt }}
          </div>
        </div>

        <div class="answer-analysis">
          <p><strong>您的答案：</strong> <span :class="item.isCorrect === 1 ? 'text-success' : 'text-danger'">{{ item.studentAnswer }}</span></p>
          <p><strong>正确答案：</strong> <span class="text-success">{{ item.correctAnswer }}</span></p>
          <div class="analysis-box">
            <strong>题目解析：</strong>
            <p>{{ item.analysis || '暂无解析' }}</p>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="collectVisible" title="加入个人错题本" width="400px">
      <el-form :model="collectForm" label-position="top">
        <el-form-item label="错误归因">
          <el-select v-model="collectForm.errorType" placeholder="请选择错误原因" style="width:100%">
            <el-option label="知识盲区" value="知识盲区" />
            <el-option label="审题不严" value="审题不严" />
            <el-option label="马虎大意" value="马虎大意" />
            <el-option label="经验总结" value="经验总结" />
          </el-select>
        </el-form-item>
        <el-form-item label="我的心得/笔记">
          <el-input v-model="collectForm.notes" type="textarea" rows="3" placeholder="写下这道题的难点或以后要注意的地方..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCollect">确认加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheck, CircleClose, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const recordId = route.params.recordId
const detail = ref({ details: [] })
const loading = ref(false)

// 收藏相关
const collectVisible = ref(false)
const collectForm = ref({ questionId: null, errorType: '知识盲区', notes: '' })

const formatType = (t) => ({ 'SINGLE_CHOICE': '单选题', 'TRUE_FALSE': '判断题', 'SHORT_ANSWER': '简答题' }[t] || t)
const getLetter = (i) => String.fromCharCode(65 + i)

const fetchDetail = async () => {
  loading.value = true
  try {
    detail.value = await request.get(`/student/exams/records/${recordId}/detail`)
  } finally {
    loading.value = false
  }
}

const openCollectDialog = (item) => {
  collectForm.value.questionId = item.questionId // 适配不同接口返回的ID字段
  collectVisible.value = true
}

const handleCollect = async () => {
  await request.post('/student/wrong-book/add', collectForm.value)
  ElMessage.success('已成功加入错题本，可在侧边栏查看')
  collectVisible.value = false
  collectForm.value.notes = ''
}

onMounted(fetchDetail)
</script>

<style scoped>
.detail-container { padding: 20px; }
.q-card { margin-top: 20px; border-radius: 8px; }
.q-header { display: flex; align-items: center; gap: 10px; margin-bottom: 15px; }
.q-stem { font-size: 16px; margin-bottom: 20px; font-weight: 500; }
.opt-item { padding: 12px; margin-bottom: 8px; border-radius: 4px; border: 1px solid #ebeef5; }
.is-correct { background-color: #f0f9eb; border-color: #67c23a; color: #67c23a; font-weight: bold; }
.is-wrong { background-color: #fef0f0; border-color: #f56c6c; color: #f56c6c; }
.answer-analysis { margin-top: 20px; padding-top: 15px; border-top: 1px dashed #dcdfe6; }
.text-success { color: #67c23a; font-weight: bold; }
.text-danger { color: #f56c6c; font-weight: bold; }
.analysis-box { background: #fdf6ec; padding: 12px; border-radius: 6px; margin-top: 10px; color: #e6a23c; }
</style>