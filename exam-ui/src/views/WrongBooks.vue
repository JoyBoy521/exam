<template>
  <div class="wrong-books-container">
    <div class="stats-header">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card orange">
            <div class="num">{{ bookList.length }}</div>
            <div class="label">待消化题目</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card blue">
            <div class="num">南阳师院</div>
            <div class="label">个人空间</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">我的个人错题笔记</span>
        </div>
      </template>

      <el-empty v-if="bookList.length === 0" description="你的错题本空空如也，去考试详情页收藏题目吧！" />

      <div v-else class="question-list">
        <el-card v-for="item in bookList" :key="item.id" class="q-item-card" shadow="hover">
          <div class="q-header">
            <el-tag size="small" type="danger" effect="dark">{{ item.errorType }}</el-tag>
            <span class="q-type-label">{{ formatType(item.type) }}</span>
            <el-button type="info" link icon="Delete" style="margin-left: auto;" @click="handleRemove(item.id)">移除</el-button>
          </div>

          <div class="q-stem">{{ item.stem }}</div>

          <div class="my-notes">
            <div class="notes-tag">我的笔记</div>
            <p class="notes-content">{{ item.notes || '暂无心得记录。' }}</p>
          </div>

          <el-collapse>
            <el-collapse-item title="查看题目原题与解析" name="1">
              <div class="options-area" v-if="item.options">
                <div v-for="(opt, idx) in item.options" :key="idx" class="opt-line"
                     :class="{ 'is-correct': hasChoice(item.correctAnswer, getLetter(idx)) }">
                  {{ getLetter(idx) }}. {{ opt }}
                </div>
              </div>
              <div class="analysis-box">
                <strong>标准解析：</strong> {{ item.analysis }}
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '../utils/request'

const bookList = ref([])
const loading = ref(false)

const formatType = (t) => ({ 'SINGLE_CHOICE': '单选题', 'MULTIPLE_CHOICE': '多选题', 'TRUE_FALSE': '判断题', 'SHORT_ANSWER': '简答题' }[t] || t)
const getLetter = (idx) => String.fromCharCode(65 + idx)
const hasChoice = (answer, letter) => {
  if (!answer) return false
  return String(answer)
    .split(',')
    .map(x => x.trim().toUpperCase())
    .includes(String(letter).trim().toUpperCase())
}

const fetchList = async () => {
  loading.value = true
  try {
    bookList.value = await request.get('/student/wrong-book/list')
  } finally {
    loading.value = false
  }
}

const handleRemove = (id) => {
  ElMessageBox.confirm('确认将此题移出错题本吗？', '提示').then(async () => {
    await request.delete(`/student/wrong-book/${id}`)
    ElMessage.success('已移除')
    fetchList()
  })
}

onMounted(fetchList)
</script>

<style scoped>
.wrong-books-container { padding: 10px; }
.stats-header { margin-bottom: 25px; }
.stat-card { padding: 20px; border-radius: 12px; color: #fff; text-align: center; }
.stat-card.orange { background: linear-gradient(135deg, #f6d365 0%, #fda085 100%); }
.stat-card.blue { background: linear-gradient(135deg, #4D58B5 0%, #7b86e0 100%); }
.stat-card .num { font-size: 28px; font-weight: bold; }
.stat-card .label { font-size: 13px; opacity: 0.8; margin-top: 5px; }

.main-card { border-radius: 8px; border: none; }
.title { font-size: 18px; font-weight: bold; color: #4D58B5; }
.q-item-card { margin-bottom: 20px; border-top: 4px solid #f56c6c; }
.q-header { display: flex; align-items: center; gap: 10px; margin-bottom: 15px; }
.q-type-label { font-size: 13px; color: #909399; }
.q-stem { font-size: 16px; font-weight: 500; margin-bottom: 20px; }

.my-notes { background: #fdf6ec; padding: 15px; border-radius: 6px; margin-bottom: 15px; }
.notes-tag { color: #e6a23c; font-size: 12px; font-weight: bold; margin-bottom: 5px; }
.notes-content { color: #606266; font-size: 14px; margin: 0; }

.opt-line { padding: 8px; font-size: 14px; }
.is-correct { color: #67c23a; font-weight: bold; }
.analysis-box { background: #fafafa; padding: 10px; font-size: 13px; color: #909399; margin-top: 10px; }
</style>
