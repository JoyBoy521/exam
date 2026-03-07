<template>
  <div class="course-detail" v-loading="loading">
    <el-page-header @back="router.push('/student/courses')" content="课程详情" />

    <el-card shadow="never" style="margin-top: 10px;">
      <h2 style="margin:0;">{{ summary.courseTitle || '课程' }}</h2>
      <p style="margin:6px 0 0;color:#6b7789;">{{ summary.courseDescription || '暂无课程说明' }}</p>
      <el-row :gutter="12" style="margin-top:12px;">
        <el-col :xs="12" :md="4"><div class="kpi">考试 {{ summary.examCount || 0 }}</div></el-col>
        <el-col :xs="12" :md="4"><div class="kpi">进行中 {{ summary.ongoingExamCount || 0 }}</div></el-col>
        <el-col :xs="12" :md="4"><div class="kpi">待开考 {{ summary.upcomingExamCount || 0 }}</div></el-col>
        <el-col :xs="12" :md="4"><div class="kpi">课程错题 {{ summary.wrongCount || 0 }}</div></el-col>
        <el-col :xs="12" :md="4"><div class="kpi">已掌握 {{ summary.masteredCount || 0 }}</div></el-col>
        <el-col :xs="12" :md="4"><div class="kpi">练习次数 {{ summary.practiceCount || 0 }}</div></el-col>
      </el-row>
      <div style="margin-top: 12px;display:flex;gap:8px;flex-wrap:wrap;">
        <el-button type="primary" @click="router.push(`/student/exam?courseId=${courseId}`)">进入本课考试</el-button>
        <el-button @click="router.push(`/student/wrong-practice?courseId=${courseId}`)">本课错题练习</el-button>
        <el-button @click="router.push(`/student/wrong-books?courseId=${courseId}`)">本课错题本</el-button>
        <el-button @click="router.push(`/student/results?courseId=${courseId}`)">本课成绩</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-top: 12px;">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never">
          <template #header>本课程考试</template>
          <el-table :data="exams" size="small">
            <el-table-column prop="title" label="考试" min-width="180" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag size="small" :type="statusTag(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开始" width="150">
              <template #default="scope">{{ formatTime(scope.row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button type="primary" link :disabled="scope.row.status !== 'ONGOING'" @click="router.push(`/exam-room/${scope.row.id}`)">
                  进入
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="9">
        <el-card shadow="never">
          <template #header>课程错题预览</template>
          <el-empty v-if="wrongPreview.length===0" description="暂无错题" />
          <div v-else class="wrong-list">
            <div class="wrong-row" v-for="w in wrongPreview" :key="w.id">
              <div class="stem" :title="stripHtml(w.stem)">{{ stripHtml(w.stem) }}</div>
              <div class="meta">掌握度：{{ w.masteryLevel || '-' }} ｜ 正确率：{{ Number(w.accuracy || 0).toFixed(2) }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const courseId = route.params.id
const loading = ref(false)
const summary = ref({})
const exams = ref([])
const wrongPreview = ref([])

const formatTime = (v) => (v ? String(v).replace('T', ' ').slice(0, 16) : '-')
const statusText = (s) => ({ NOT_STARTED: '未开始', ONGOING: '进行中', FINISHED: '已结束' }[s] || s)
const statusTag = (s) => ({ NOT_STARTED: 'warning', ONGOING: 'success', FINISHED: 'info' }[s] || '')
const stripHtml = (h) => String(h || '').replace(/<[^>]*>/g, '')

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/student/courses/${courseId}/overview`)
    summary.value = res.summary || {}
    exams.value = res.exams || []
    wrongPreview.value = res.wrongPreview || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.course-detail { padding: 10px; }
.kpi { border:1px solid #ebeef5; border-radius:8px; background:#fafafa; padding:10px; text-align:center; }
.wrong-list { display: grid; gap: 10px; }
.wrong-row { border:1px solid #ebeef5; border-radius:8px; padding:10px; }
.stem { font-size:13px; color:#2e3745; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta { font-size:12px; color:#7c8898; margin-top:4px; }
</style>
