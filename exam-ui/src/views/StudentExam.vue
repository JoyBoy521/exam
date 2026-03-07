<template>
  <div class="student-exam-container" v-loading="loading">
    <div class="welcome-banner">
      <div class="text">
        <h2>你好，{{ currentUsername }} 同学</h2>
        <p>今天继续保持节奏，先完成待考任务，再做错题巩固。</p>
      </div>
      <div class="quick-actions">
        <el-button type="primary" plain @click="router.push('/student/wrong-practice')">开始错题练习</el-button>
        <el-button type="success" plain @click="router.push('/student/results')">查看成绩单</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="24" :sm="12" :md="6"><div class="kpi"><div>进行中考试</div><b>{{ ongoingExams.length }}</b></div></el-col>
      <el-col :xs="24" :sm="12" :md="6"><div class="kpi"><div>待开考</div><b>{{ upcomingExams.length }}</b></div></el-col>
      <el-col :xs="24" :sm="12" :md="6"><div class="kpi"><div>今日复习建议</div><b>{{ dailyPlan.targetCount || 0 }}</b></div></el-col>
      <el-col :xs="24" :sm="12" :md="6"><div class="kpi"><div>补考待审核</div><b>{{ makeupSummary.pendingCount || 0 }}</b></div></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 12px;">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header><span class="card-title">考试任务中心</span></template>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8" v-for="exam in tableData" :key="exam.id">
              <el-card class="exam-card" :class="exam.status" shadow="hover">
                <div class="card-tag">{{ formatStatus(exam.status) }}</div>
                <h3 class="exam-title" :title="exam.title">{{ exam.title }}</h3>
                <div class="exam-info">
                  <p><el-icon><Calendar /></el-icon> 开始：{{ formatTime(exam.startTime) }}</p>
                  <p><el-icon><Timer /></el-icon> 结束：{{ formatTime(exam.endTime) }}</p>
                </div>
                <div class="card-footer">
                  <el-button
                    type="primary"
                    class="enter-btn"
                    :disabled="exam.status !== 'ONGOING'"
                    @click="enterExam(exam.id)"
                  >
                    {{ exam.status === 'ONGOING' ? '进入考场' : (exam.status === 'NOT_STARTED' ? '等待开考' : '已结束') }}
                  </el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-empty v-if="tableData.length === 0" description="近期暂无考试安排" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" style="margin-bottom: 12px;">
          <template #header><span class="card-title">学习周报（近7天）</span></template>
          <div class="plan-metrics">
            <div>累计练习 <b>{{ weeklyReport.practiceTimes || 0 }}</b> 次</div>
            <div>平均正确率 <b>{{ Number(weeklyReport.avgAccuracy || 0).toFixed(2) }}%</b></div>
          </div>
          <el-row :gutter="8" class="mini-kpis">
            <el-col :span="12"><div class="mini-kpi">已练题目 {{ weeklyReport.practicedCount || 0 }}</div></el-col>
            <el-col :span="12"><div class="mini-kpi">已掌握 {{ weeklyReport.masteredCount || 0 }}</div></el-col>
            <el-col :span="12"><div class="mini-kpi">待巩固 {{ weeklyReport.needReviewCount || 0 }}</div></el-col>
            <el-col :span="12"><div class="mini-kpi">错题总数 {{ weeklyReport.totalWrongCount || 0 }}</div></el-col>
          </el-row>
          <el-table :data="weeklyTrendRows" size="small" max-height="180" style="margin-top: 8px;">
            <el-table-column prop="dateLabel" label="日期" width="90" />
            <el-table-column prop="count" label="活跃题数" width="80" />
            <el-table-column label="趋势">
              <template #default="scope">
                <el-progress :percentage="scope.row.percent" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never">
          <template #header><span class="card-title">今日复习计划</span></template>
          <div class="plan-metrics">
            <div>应复习 <b>{{ dailyPlan.dueCount || 0 }}</b> 题</div>
            <div>高优先级 <b style="color:#f56c6c;">{{ dailyPlan.highPriorityCount || 0 }}</b> 题</div>
          </div>
          <el-table :data="dailyPlan.recommended || []" size="small" max-height="280">
            <el-table-column label="题目" min-width="160">
              <template #default="scope">
                <div class="ellipsis" :title="stripHtml(scope.row.stem)">{{ stripHtml(scope.row.stem) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="掌握度" width="90">
              <template #default="scope">
                <el-tag size="small" :type="masteryMeta(scope.row.masteryLevel).tag">{{ masteryMeta(scope.row.masteryLevel).text }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 12px;">
            <el-button type="primary" style="width: 100%;" @click="router.push('/student/wrong-practice')">按计划开始练习</el-button>
          </div>
        </el-card>

        <el-card shadow="never" style="margin-top: 12px;">
          <template #header>
            <div class="header-between">
              <span class="card-title">补考申请</span>
              <el-button type="primary" link @click="openMakeupDialog">发起申请</el-button>
            </div>
          </template>
          <div class="plan-metrics">
            <div>待审核 <b>{{ makeupSummary.pendingCount || 0 }}</b></div>
            <div>已批准 <b style="color:#67c23a;">{{ makeupSummary.approvedCount || 0 }}</b></div>
            <div>已驳回 <b style="color:#f56c6c;">{{ makeupSummary.rejectedCount || 0 }}</b></div>
          </div>
          <el-table :data="makeupSummary.latest || []" size="small" max-height="220">
            <el-table-column prop="examTitle" label="考试" min-width="120" />
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-tag size="small" :type="makeupStatusMeta(scope.row.status).tag">{{ makeupStatusMeta(scope.row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="120">
              <template #default="scope">{{ formatDateTime(scope.row.requestedAt).slice(5, 16) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="makeupVisible" title="发起补考申请" width="520px">
      <el-form label-width="90px">
        <el-form-item label="目标考试">
          <el-select v-model="makeupForm.examId" style="width:100%" filterable placeholder="选择已结束考试">
            <el-option v-for="exam in makeupExamOptions" :key="exam.id" :label="exam.title" :value="exam.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="makeupForm.reason" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="makeupVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingMakeup" @click="submitMakeup">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, Timer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { formatDateTime, makeupStatusMeta, masteryMeta } from '../utils/format'

const router = useRouter()
const route = useRoute()
const currentUsername = ref(localStorage.getItem('username') || '学生')
const tableData = ref([])
const loading = ref(false)
const dailyPlan = ref({ targetCount: 10, dueCount: 0, highPriorityCount: 0, recommended: [] })
const makeupSummary = ref({ pendingCount: 0, approvedCount: 0, rejectedCount: 0, latest: [] })
const weeklyReport = ref({ practiceTimes: 0, avgAccuracy: 0, practicedCount: 0, masteredCount: 0, needReviewCount: 0, totalWrongCount: 0, weeklyTrend: [] })

const makeupVisible = ref(false)
const submittingMakeup = ref(false)
const makeupForm = ref({ examId: null, reason: '' })

const ongoingExams = computed(() => tableData.value.filter(e => e.status === 'ONGOING'))
const upcomingExams = computed(() => tableData.value.filter(e => e.status === 'NOT_STARTED'))
const makeupExamOptions = computed(() => tableData.value.filter(e => e.status === 'FINISHED'))
const weeklyTrendRows = computed(() => {
  const src = weeklyReport.value.weeklyTrend || []
  const maxCount = src.reduce((m, x) => Math.max(m, Number(x.count || 0)), 0)
  return src.map(x => ({
    ...x,
    dateLabel: String(x.date || '').slice(5),
    count: Number(x.count || 0),
    percent: maxCount <= 0 ? 0 : Math.round(Number(x.count || 0) * 100 / maxCount)
  }))
})

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '-'
const formatStatus = (s) => ({ NOT_STARTED: '未开始', ONGOING: '考试中', FINISHED: '已结束' }[s] || s)
const stripHtml = (html) => String(html || '').replace(/<[^>]*>/g, '')

const fetchExams = async () => {
  loading.value = true
  try {
    const courseId = route.query.courseId ? Number(route.query.courseId) : undefined
    tableData.value = await request.get('/student/exams/list', {
      params: { courseId }
    })
  } finally {
    loading.value = false
  }
}

const fetchDailyPlan = async () => {
  dailyPlan.value = await request.get('/student/wrong-book/daily-plan', { params: { targetCount: 10 } })
}

const fetchMakeupSummary = async () => {
  makeupSummary.value = await request.get('/student/makeup-requests/summary')
}

const fetchWeeklyReport = async () => {
  weeklyReport.value = await request.get('/student/wrong-book/weekly-report')
}

const enterExam = (examId) => {
  router.push(`/exam-room/${examId}`)
}

const openMakeupDialog = () => {
  makeupForm.value = { examId: null, reason: '' }
  makeupVisible.value = true
}

const submitMakeup = async () => {
  if (!makeupForm.value.examId) {
    ElMessage.warning('请选择目标考试')
    return
  }
  if (!makeupForm.value.reason || !makeupForm.value.reason.trim()) {
    ElMessage.warning('请填写申请原因')
    return
  }
  submittingMakeup.value = true
  try {
    await request.post('/student/makeup-requests', {
      examId: makeupForm.value.examId,
      reason: makeupForm.value.reason.trim()
    })
    ElMessage.success('补考申请已提交')
    makeupVisible.value = false
    await fetchMakeupSummary()
  } finally {
    submittingMakeup.value = false
  }
}

const showOnboarding = () => {
  const key = 'onboarding_student_v1'
  if (localStorage.getItem(key)) return
  ElMessage.info('欢迎使用学生端：先看考试任务，再按“今日复习计划”做错题巩固。')
  localStorage.setItem(key, '1')
}

onMounted(async () => {
  await Promise.all([fetchExams(), fetchDailyPlan(), fetchMakeupSummary(), fetchWeeklyReport()])
  showOnboarding()
})
</script>

<style scoped>
.student-exam-container { padding: 12px; }
.welcome-banner {
  background: linear-gradient(135deg, #1f4e89 0%, #123661 100%);
  border-radius: 14px;
  color: #fff;
  padding: 22px 24px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.welcome-banner h2 { margin: 0 0 6px; }
.welcome-banner p { margin: 0; color: #d6e6ff; }
.quick-actions { display: flex; gap: 8px; align-items: center; }
.kpi-row .kpi { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px 12px; background: #fff; color: #606266; }
.kpi-row .kpi b { display: block; margin-top: 6px; color: #303133; font-size: 22px; }
.card-title { font-weight: 600; }
.header-between { display: flex; justify-content: space-between; align-items: center; }

.exam-card {
  border-radius: 12px;
  margin-bottom: 12px;
  position: relative;
  overflow: hidden;
  border: none;
}
.exam-card.ONGOING { border-top: 4px solid #67c23a; }
.exam-card.NOT_STARTED { border-top: 4px solid #e6a23c; }
.exam-card.FINISHED { border-top: 4px solid #909399; opacity: 0.86; }
.card-tag {
  position: absolute;
  right: -30px;
  top: 14px;
  width: 120px;
  transform: rotate(45deg);
  text-align: center;
  font-size: 12px;
  color: #fff;
  font-weight: 600;
}
.ONGOING .card-tag { background: #67c23a; }
.NOT_STARTED .card-tag { background: #e6a23c; }
.FINISHED .card-tag { background: #909399; }
.exam-title { margin: 8px 0 16px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.exam-info p { margin: 8px 0; color: #606266; display: flex; gap: 8px; align-items: center; font-size: 13px; }
.card-footer { margin-top: 10px; }
.enter-btn { width: 100%; }

.plan-metrics { display: flex; justify-content: space-between; margin-bottom: 10px; color: #606266; font-size: 13px; }
.plan-metrics b { color: #303133; }
.ellipsis { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mini-kpis { font-size: 12px; color: #606266; }
.mini-kpi { background: #fafafa; border: 1px solid #ebeef5; border-radius: 6px; padding: 6px 8px; margin-bottom: 8px; }
@media (max-width: 900px) {
  .quick-actions { width: 100%; justify-content: flex-start; flex-wrap: wrap; }
  .welcome-banner { padding: 18px 16px; }
}
</style>
