<template>
  <div class="practice-page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <span class="title">错题重练</span>
          <div>
            <el-input-number v-model="limit" :min="5" :max="100" :step="5" size="small" style="margin-right: 8px;" />
            <el-button size="small" @click="fetchQuestions">刷新题目</el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="questions.length === 0" description="错题本暂无可练习题目，先去考试吧！" />

      <div v-else>
        <el-alert type="info" :closable="false" style="margin-bottom: 12px;">
          本次共 {{ questions.length }} 题。客观题自动判分，简答题仅提供参考答案。
        </el-alert>

        <el-card v-for="(q, index) in questions" :key="q.id" class="q-card" shadow="hover">
          <div class="q-head">
            <el-tag size="small">{{ formatType(q.type) }}</el-tag>
            <span>第 {{ index + 1 }} 题</span>
            <el-tag size="small" :type="masteryTagType(q.masteryLevel)">{{ masteryText(q.masteryLevel) }}</el-tag>
            <span class="mini-metric">正确率 {{ Number(q.accuracy || 0).toFixed(2) }}%</span>
            <span class="mini-metric">练习 {{ q.practiceCount || 0 }} 次</span>
          </div>
          <div class="q-stem" v-html="q.stem"></div>

          <div v-if="q.type === 'SINGLE_CHOICE' || q.type === 'TRUE_FALSE'" class="q-options">
            <el-radio-group v-model="answers[q.id]" class="stack">
              <el-radio v-for="(opt, idx) in q.options" :key="idx" :value="getLetter(idx)">
                {{ getLetter(idx) }}. {{ opt }}
              </el-radio>
            </el-radio-group>
          </div>

          <div v-else-if="q.type === 'MULTIPLE_CHOICE'" class="q-options">
            <el-checkbox-group v-model="answers[q.id]" class="stack">
              <el-checkbox v-for="(opt, idx) in q.options" :key="idx" :label="getLetter(idx)">
                {{ getLetter(idx) }}. {{ opt }}
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <div v-else>
            <el-input v-model="answers[q.id]" type="textarea" :rows="4" placeholder="请输入你的答案" />
          </div>
        </el-card>

        <div class="actions">
          <el-button type="primary" size="large" @click="submitPractice">提交练习</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="result" shadow="never" style="margin-top: 16px;">
      <template #header><span class="title">练习结果</span></template>
      <el-row :gutter="16">
        <el-col :span="8"><div class="kpi"><div>客观题总数</div><b>{{ result.objectiveCount }}</b></div></el-col>
        <el-col :span="8"><div class="kpi"><div>客观题答对</div><b>{{ result.objectiveCorrect }}</b></div></el-col>
        <el-col :span="8"><div class="kpi"><div>客观题正确率</div><b>{{ result.objectiveAccuracy }}%</b></div></el-col>
      </el-row>
      <el-table :data="result.details || []" style="margin-top: 12px;" size="small">
        <el-table-column prop="type" label="题型" width="120">
          <template #default="scope">{{ formatType(scope.row.type) }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题目" min-width="240">
          <template #default="scope">
            <div class="ellipsis" :title="stripHtml(scope.row.stem)">{{ stripHtml(scope.row.stem) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="userAnswer" label="你的答案" width="160" />
        <el-table-column prop="correctAnswer" label="参考答案" width="160" />
        <el-table-column label="结果" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.isCorrect === true" type="success">正确</el-tag>
            <el-tag v-else-if="scope.row.isCorrect === false" type="danger">错误</el-tag>
            <el-tag v-else type="info">主观题</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="掌握度" width="120">
          <template #default="scope">
            <el-tag :type="masteryTagType(scope.row.masteryLevel)">
              {{ masteryText(scope.row.masteryLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="正确率" width="100">
          <template #default="scope">{{ Number(scope.row.accuracy || 0).toFixed(2) }}%</template>
        </el-table-column>
        <el-table-column label="练习次数" width="100">
          <template #default="scope">{{ scope.row.practiceCount || 0 }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { masteryMeta } from '../utils/format'

const loading = ref(false)
const questions = ref([])
const answers = ref({})
const result = ref(null)
const limit = ref(20)
const route = useRoute()

const getLetter = (idx) => String.fromCharCode(65 + idx)
const formatType = (t) => ({ SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', TRUE_FALSE: '判断题', SHORT_ANSWER: '简答题' }[t] || t)
const masteryText = (v) => masteryMeta(v).text
const masteryTagType = (v) => masteryMeta(v).tag

const stripHtml = (html) => String(html || '').replace(/<[^>]*>/g, '')

const fetchQuestions = async () => {
  loading.value = true
  try {
    const data = await request.get('/student/wrong-book/practice', {
      params: {
        limit: limit.value,
        courseId: route.query.courseId ? Number(route.query.courseId) : undefined
      }
    })
    questions.value = data || []
    result.value = null
    const newAnswers = {}
    for (const q of questions.value) {
      newAnswers[q.id] = q.type === 'MULTIPLE_CHOICE' ? [] : ''
    }
    answers.value = newAnswers
  } finally {
    loading.value = false
  }
}

const normalizeAnswers = () => {
  const payload = {}
  for (const q of questions.value) {
    const raw = answers.value[q.id]
    if (q.type === 'MULTIPLE_CHOICE') {
      payload[q.id] = Array.isArray(raw) ? [...new Set(raw)].sort().join(',') : ''
    } else {
      payload[q.id] = String(raw || '').trim()
    }
  }
  return payload
}

const submitPractice = async () => {
  if (questions.value.length === 0) return
  loading.value = true
  try {
    result.value = await request.post('/student/wrong-book/practice/submit', {
      answers: normalizeAnswers()
    })
    ElMessage.success('练习已提交')
  } finally {
    loading.value = false
  }
}

fetchQuestions()
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
.q-card { margin-bottom: 12px; }
.q-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.q-stem { margin-bottom: 12px; line-height: 1.7; }
.stack { display: flex; flex-direction: column; gap: 8px; }
.actions { text-align: center; margin-top: 10px; }
.mini-metric { font-size: 12px; color: #909399; }
.kpi { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px 12px; background: #fafafa; color: #606266; }
.kpi b { display: block; margin-top: 6px; font-size: 20px; color: #303133; }
.ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
