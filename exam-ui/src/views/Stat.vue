<template>
  <div class="stat-page">
    <el-row :gutter="16" class="cards">
      <el-col :span="6"><el-card><div class="k">题目数</div><div class="v">{{ stat.questionCount || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="k">试卷数</div><div class="v">{{ stat.paperCount || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="k">平行组</div><div class="v">{{ stat.parallelGroupCount || 0 }}</div></el-card></el-col>
      <el-col :span="6"><el-card><div class="k">分配记录</div><div class="v">{{ stat.assignmentCount || 0 }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px;">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>考试状态总览</span></template>
          <el-row :gutter="10">
            <el-col :span="8"><div class="mini"><div>未开始</div><b>{{ todo.draftLikeExamCount || 0 }}</b></div></el-col>
            <el-col :span="8"><div class="mini"><div>进行中</div><b>{{ todo.ongoingExamCount || 0 }}</b></div></el-col>
            <el-col :span="8"><div class="mini"><div>已结束</div><b>{{ todo.finishedExamCount || 0 }}</b></div></el-col>
          </el-row>
          <el-row :gutter="10" style="margin-top:10px;">
            <el-col :span="12"><div class="mini"><div>总交卷</div><b>{{ todo.totalSubmissionCount || 0 }}</b></div></el-col>
            <el-col :span="12"><div class="mini"><div>待人工批阅</div><b>{{ todo.pendingManualReviewCount || 0 }}</b></div></el-col>
          </el-row>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>题型分布</span></template>
          <el-table :data="questionTypeRows" size="small" height="220">
            <el-table-column prop="type" label="题型" />
            <el-table-column prop="count" label="数量" width="100" />
            <el-table-column label="占比" width="180">
              <template #default="scope">
                <el-progress :percentage="scope.row.percent" :stroke-width="12" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px;">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>近7天交卷趋势</span></template>
          <el-table :data="trendRows" size="small" height="240">
            <el-table-column prop="dateLabel" label="日期" width="120" />
            <el-table-column prop="count" label="交卷数" width="90" />
            <el-table-column label="趋势">
              <template #default="scope">
                <el-progress :percentage="scope.row.percent" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>异常班级排行 Top10</span></template>
          <el-table :data="abnormalClassRows" size="small" height="240">
            <el-table-column label="#" width="60">
              <template #default="scope">{{ scope.$index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="className" label="班级" />
            <el-table-column prop="riskScore" label="风险分" width="90" />
            <el-table-column prop="riskStudentCount" label="异常人数" width="90" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>通过率分层</span></template>
          <div class="pass-overview">
            <div>已出分：<b>{{ advanced.gradedCount || 0 }}</b></div>
            <div>待批阅：<b>{{ advanced.pendingCount || 0 }}</b></div>
            <div>总体通过率：<b>{{ advanced.overallPassRate || 0 }}%</b></div>
          </div>
          <el-table :data="passLayerRows" size="small" height="188">
            <el-table-column prop="label" label="分层" />
            <el-table-column prop="count" label="人数" width="90" />
            <el-table-column prop="rate" label="占比" width="90">
              <template #default="scope">{{ scope.row.rate }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px;">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>知识点覆盖 Top10</span></template>
          <el-table :data="stat.topKnowledgeCoverage || []" size="small" height="260">
            <el-table-column prop="knowledgePoint" label="知识点" />
            <el-table-column prop="count" label="题量" width="100" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>试卷分配分布</span></template>
          <el-table :data="paperDistributionRows" size="small" height="260">
            <el-table-column prop="paperId" label="试卷ID" width="120" />
            <el-table-column prop="count" label="分配人数" width="120" />
            <el-table-column label="占比">
              <template #default="scope">
                <el-progress :percentage="scope.row.percent" :stroke-width="12" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px;" shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span>防作弊风险汇总</span>
          <el-select v-model="examId" placeholder="选择考试" style="width:260px" @change="fetchRisk">
            <el-option v-for="e in exams" :key="e.id" :label="e.title" :value="e.id" />
          </el-select>
        </div>
      </template>
      <el-table :data="riskList" v-loading="loadingRisk">
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="eventCount" label="事件数" width="100" />
        <el-table-column prop="riskScore" label="风险分" width="100" />
        <el-table-column prop="riskLevel" label="风险等级" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.riskLevel === 'HIGH' ? 'danger' : (scope.row.riskLevel === 'MEDIUM' ? 'warning' : 'success')">
              {{ scope.row.riskLevel }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import request from '../utils/request'

const stat = ref({})
const todo = ref({})
const exams = ref([])
const examId = ref(null)
const riskList = ref([])
const loadingRisk = ref(false)
const advanced = ref({
  submissionTrend: [],
  abnormalClassRanking: [],
  passRateLayers: [],
  overallPassRate: 0,
  gradedCount: 0,
  pendingCount: 0
})

const typeNameMap = {
  SINGLE_CHOICE: '单选题',
  TRUE_FALSE: '判断题',
  SHORT_ANSWER: '简答题'
}

const questionTypeRows = computed(() => {
  const src = stat.value.questionTypeDistribution || {}
  const total = Object.values(src).reduce((a, b) => a + Number(b || 0), 0)
  return Object.keys(src).map((k) => {
    const c = Number(src[k] || 0)
    return {
      type: typeNameMap[k] || k,
      count: c,
      percent: total > 0 ? Math.round((c * 100) / total) : 0
    }
  }).sort((a, b) => b.count - a.count)
})

const paperDistributionRows = computed(() => {
  const src = stat.value.paperAssignmentDistribution || {}
  const total = Object.values(src).reduce((a, b) => a + Number(b || 0), 0)
  return Object.keys(src).map((paperId) => {
    const c = Number(src[paperId] || 0)
    return {
      paperId,
      count: c,
      percent: total > 0 ? Math.round((c * 100) / total) : 0
    }
  }).sort((a, b) => b.count - a.count)
})

const trendRows = computed(() => {
  const src = advanced.value.submissionTrend || []
  const maxCount = src.reduce((m, item) => Math.max(m, Number(item.count || 0)), 0)
  return src.map(item => ({
    ...item,
    dateLabel: (item.date || '').slice(5),
    count: Number(item.count || 0),
    percent: maxCount > 0 ? Math.round(Number(item.count || 0) * 100 / maxCount) : 0
  }))
})

const abnormalClassRows = computed(() => advanced.value.abnormalClassRanking || [])
const passLayerRows = computed(() => advanced.value.passRateLayers || [])

const fetchOverview = async () => {
  const [s, t, a] = await Promise.all([
    request.get('/teacher/statistics'),
    request.get('/teacher/todo'),
    request.get('/teacher/statistics/advanced')
  ])
  stat.value = s
  todo.value = t
  advanced.value = a || {}
}

const fetchExams = async () => {
  exams.value = await request.get('/teacher/exams')
  if (exams.value.length && !examId.value) {
    examId.value = exams.value[0].id
    fetchRisk()
  }
}

const fetchRisk = async () => {
  if (!examId.value) return
  loadingRisk.value = true
  try {
    riskList.value = await request.get('/teacher/cheat-events/risk-summary', { params: { examId: examId.value } })
  } finally {
    loadingRisk.value = false
  }
}

onMounted(() => {
  fetchOverview()
  fetchExams()
})
</script>

<style scoped>
.cards .k { color: #909399; font-size: 13px; }
.cards .v { margin-top: 8px; font-size: 24px; font-weight: 700; color: #303133; }
.mini {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fafafa;
  color: #606266;
}
.mini b {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  color: #303133;
}
.pass-overview {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;
}
.pass-overview b {
  color: #303133;
}
</style>
