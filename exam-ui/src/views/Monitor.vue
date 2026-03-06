<template>
  <div class="monitor-page">
    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <div class="left">
            <span class="title">教师监考台</span>
            <el-tag size="small" :type="wsConnected ? 'success' : 'info'">{{ wsConnected ? '实时连接' : '轮询模式' }}</el-tag>
          </div>
          <div class="right">
            <el-select v-model="examId" placeholder="选择考试" style="width: 280px" @change="fetchDashboard">
              <el-option v-for="e in exams" :key="e.id" :label="e.title" :value="e.id" />
            </el-select>
          </div>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col :span="6"><div class="kpi"><div>在线人数</div><b>{{ dashboard.onlineCount || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="kpi"><div>总人数</div><b>{{ dashboard.totalStudents || 0 }}</b></div></el-col>
        <el-col :span="6"><div class="kpi"><div>在线率</div><b>{{ dashboard.onlineRate || 0 }}%</b></div></el-col>
        <el-col :span="6"><div class="kpi"><div>违规事件(近30条)</div><b>{{ (dashboard.recentViolations || []).length }}</b></div></el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>在线学生明细</span></template>
          <el-table :data="dashboard.onlineStudents || []" size="small" height="420" v-loading="loading">
            <el-table-column prop="studentNo" label="学号" width="150" />
            <el-table-column prop="studentName" label="姓名" width="120" />
            <el-table-column label="进度" min-width="120">
              <template #default="scope">
                {{ scope.row.answeredCount || 0 }}/{{ scope.row.totalCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="剩余时间" min-width="110">
              <template #default="scope">{{ formatDuration(scope.row.timeLeftSeconds) }}</template>
            </el-table-column>
            <el-table-column label="最后心跳" min-width="160">
              <template #default="scope">{{ formatDateTime(scope.row.updatedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>风险学生 Top10</span></template>
          <el-table :data="dashboard.riskTopStudents || []" size="small" height="420" v-loading="loading">
            <el-table-column label="#" width="60">
              <template #default="scope">{{ scope.$index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="studentNo" label="学号" width="150" />
            <el-table-column prop="studentName" label="姓名" width="120" />
            <el-table-column prop="eventCount" label="事件数" width="90" />
            <el-table-column prop="riskScore" label="风险分" width="90" />
            <el-table-column label="等级" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.riskLevel === 'HIGH' ? 'danger' : (scope.row.riskLevel === 'MEDIUM' ? 'warning' : 'success')">
                  {{ scope.row.riskLevel }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px;" shadow="never">
      <template #header><span>最近违规流（最新30条）</span></template>
      <el-table :data="dashboard.recentViolations || []" size="small" height="360" v-loading="loading">
        <el-table-column prop="happenedAt" label="时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.happenedAt) }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="type" label="事件类型" width="160">
          <template #default="scope"><el-tag size="small">{{ scope.row.type }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="durationSeconds" label="时长(s)" width="90" />
        <el-table-column prop="detail" label="详情" min-width="220" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import request from '../utils/request'

const exams = ref([])
const examId = ref(null)
const loading = ref(false)
const dashboard = ref({
  onlineCount: 0,
  totalStudents: 0,
  onlineRate: 0,
  onlineStudents: [],
  riskTopStudents: [],
  recentViolations: []
})

let pollTimer = null
let ws = null
let wsReconnectTimer = null
const wsConnected = ref(false)

const fetchExams = async () => {
  exams.value = await request.get('/teacher/exams')
  if (exams.value.length > 0 && !examId.value) {
    examId.value = exams.value[0].id
  }
}

const fetchDashboard = async () => {
  if (!examId.value) return
  loading.value = true
  try {
    dashboard.value = await request.get('/teacher/monitor/dashboard', { params: { examId: examId.value } })
  } finally {
    loading.value = false
  }
}

const formatDateTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

const formatDuration = (seconds) => {
  const safe = Math.max(0, Number(seconds || 0))
  const h = Math.floor(safe / 3600).toString().padStart(2, '0')
  const m = Math.floor((safe % 3600) / 60).toString().padStart(2, '0')
  const s = (safe % 60).toString().padStart(2, '0')
  return `${h}:${m}:${s}`
}

const getWsUrl = () => {
  const token = localStorage.getItem('token') || ''
  if (!token) return ''
  const apiBase = request.defaults.baseURL || ''
  const wsBase = apiBase.replace(/^http/i, 'ws').replace(/\/api\/?$/, '')
  return `${wsBase}/ws/teacher-risk?token=${encodeURIComponent(token)}`
}

const connectWs = () => {
  const url = getWsUrl()
  if (!url) return
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return

  ws = new WebSocket(url)
  ws.onopen = () => {
    wsConnected.value = true
  }
  ws.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data)
      if (Number(payload.examId) === Number(examId.value) && (payload.kind === 'CHEAT_EVENT' || payload.kind === 'HEARTBEAT')) {
        fetchDashboard()
      }
    } catch (_) {
      // ignore parse error
    }
  }
  ws.onclose = () => {
    wsConnected.value = false
    if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
    wsReconnectTimer = setTimeout(connectWs, 3000)
  }
  ws.onerror = () => {
    wsConnected.value = false
    try { ws?.close() } catch (_) {}
  }
}

onMounted(async () => {
  await fetchExams()
  await fetchDashboard()
  connectWs()
  pollTimer = setInterval(() => {
    if (!wsConnected.value) fetchDashboard()
  }, 15000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  if (ws) {
    try { ws.close() } catch (_) {}
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.toolbar .left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toolbar .title {
  font-weight: 600;
}
.kpi {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fafafa;
  color: #606266;
}
.kpi b {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  color: #303133;
}
</style>
