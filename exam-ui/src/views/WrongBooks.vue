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
            <div class="num">{{ masteredCount }}</div>
            <div class="label">已掌握</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card red">
            <div class="num">{{ needReviewCount }}</div>
            <div class="label">需巩固</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card green">
            <div class="num">{{ avgAccuracy }}%</div>
            <div class="label">平均正确率</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="title">我的个人错题笔记</span>
          <div class="filters">
            <el-select v-model="filters.masteryLevel" clearable placeholder="掌握度" style="width: 130px;" @change="onFilterChange">
              <el-option label="未练习" value="UNPRACTICED" />
              <el-option label="需巩固" value="NEEDS_REVIEW" />
              <el-option label="提升中" value="IMPROVING" />
              <el-option label="已掌握" value="MASTERED" />
              <el-option label="主观题" value="SUBJECTIVE" />
            </el-select>
            <el-input v-model="filters.keyword" clearable placeholder="搜索题干/笔记/错误类型" style="width: 240px;" @clear="onFilterChange" />
            <el-button type="primary" plain @click="onFilterChange">筛选</el-button>
          </div>
        </div>
      </template>
      <LoadErrorBar :message="loadError" @retry="fetchList" />

      <el-empty v-if="bookList.length === 0" description="你的错题本空空如也，去考试详情页收藏题目吧！" />

      <div v-else class="question-list">
        <el-card v-for="item in bookList" :key="item.id" class="q-item-card" shadow="hover">
          <div class="q-header">
            <el-tag size="small" type="danger" effect="dark">{{ item.errorType }}</el-tag>
            <span class="q-type-label">{{ formatType(item.type) }}</span>
            <el-tag size="small" :type="masteryTagType(item.masteryLevel)">{{ masteryText(item.masteryLevel) }}</el-tag>
            <el-button type="info" link icon="Delete" style="margin-left: auto;" @click="handleRemove(item.id)">移除</el-button>
          </div>

          <div class="q-stem">{{ item.stem }}</div>

          <div class="mastery-panel">
            <span>练习 {{ item.practiceCount || 0 }} 次</span>
            <span>答对 {{ item.correctCount || 0 }} 次</span>
            <span>正确率 {{ Number(item.accuracy || 0).toFixed(2) }}%</span>
            <span v-if="item.lastPracticeTime">最近练习 {{ item.lastPracticeTime }}</span>
          </div>

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
      <el-pagination
        v-model:current-page="pager.page"
        v-model:page-size="pager.size"
        :total="pager.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end;"
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '../utils/request'
import LoadErrorBar from '../components/LoadErrorBar.vue'
import { masteryMeta } from '../utils/format'

const bookList = ref([])
const loading = ref(false)
const loadError = ref('')
const pager = ref({ page: 1, size: 10, total: 0 })
const filters = ref({ masteryLevel: '', keyword: '' })

const formatType = (t) => ({ 'SINGLE_CHOICE': '单选题', 'MULTIPLE_CHOICE': '多选题', 'TRUE_FALSE': '判断题', 'SHORT_ANSWER': '简答题' }[t] || t)
const masteryText = (v) => masteryMeta(v).text
const masteryTagType = (v) => masteryMeta(v).tag
const getLetter = (idx) => String.fromCharCode(65 + idx)
const hasChoice = (answer, letter) => {
  if (!answer) return false
  return String(answer)
    .split(',')
    .map(x => x.trim().toUpperCase())
    .includes(String(letter).trim().toUpperCase())
}

const masteredCount = computed(() => bookList.value.filter(x => x.masteryLevel === 'MASTERED').length)
const needReviewCount = computed(() => bookList.value.filter(x => x.masteryLevel === 'NEEDS_REVIEW').length)
const avgAccuracy = computed(() => {
  if (bookList.value.length === 0) return '0.00'
  const sum = bookList.value.reduce((acc, x) => acc + Number(x.accuracy || 0), 0)
  return (sum / bookList.value.length).toFixed(2)
})

const fetchList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get('/student/wrong-book/list', {
      params: {
        page: pager.value.page,
        size: pager.value.size,
        masteryLevel: filters.value.masteryLevel || undefined,
        keyword: filters.value.keyword?.trim() || undefined
      }
    })
    bookList.value = res.list || []
    pager.value.total = Number(res.total || 0)
  } catch (e) {
    loadError.value = e?.response?.data?.error || '加载错题本失败'
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  pager.value.page = 1
  fetchList()
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
.stat-card.red { background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%); }
.stat-card.green { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
.stat-card .num { font-size: 28px; font-weight: bold; }
.stat-card .label { font-size: 13px; opacity: 0.8; margin-top: 5px; }

.main-card { border-radius: 8px; border: none; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.filters { display: flex; align-items: center; gap: 8px; }
.title { font-size: 18px; font-weight: bold; color: #4D58B5; }
.q-item-card { margin-bottom: 20px; border-top: 4px solid #f56c6c; }
.q-header { display: flex; align-items: center; gap: 10px; margin-bottom: 15px; }
.q-type-label { font-size: 13px; color: #909399; }
.q-stem { font-size: 16px; font-weight: 500; margin-bottom: 20px; }
.mastery-panel { display: flex; flex-wrap: wrap; gap: 14px; font-size: 13px; color: #606266; margin-bottom: 12px; }

.my-notes { background: #fdf6ec; padding: 15px; border-radius: 6px; margin-bottom: 15px; }
.notes-tag { color: #e6a23c; font-size: 12px; font-weight: bold; margin-bottom: 5px; }
.notes-content { color: #606266; font-size: 14px; margin: 0; }

.opt-line { padding: 8px; font-size: 14px; }
.is-correct { color: #67c23a; font-weight: bold; }
.analysis-box { background: #fafafa; padding: 10px; font-size: 13px; color: #909399; margin-top: 10px; }
</style>
