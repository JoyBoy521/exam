<template>
  <div class="marking-container">
    <div class="page-header">
      <div class="left" @click="$router.back()">
        <el-icon><Back /></el-icon> 返回考试列表
      </div>
      <div class="title">批阅中心</div>
      <div class="right-stats">
        <el-tag type="success" effect="dark" round>待阅 {{ pendingCount }} / 总计 {{ allCount }}</el-tag>
      </div>
    </div>

    <el-card class="main-card" shadow="never">
      <LoadErrorBar :message="loadError" @retry="fetchRecords" />
      <div class="summary">
        <el-statistic title="总交卷" :value="allCount" />
        <el-statistic title="待阅主观题" :value="pendingCount" />
        <el-statistic title="已完成" :value="gradedCount" />
      </div>

      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" size="small">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="MARKING">待批阅</el-radio-button>
          <el-radio-button label="GRADED">已批阅</el-radio-button>
        </el-radio-group>
        <el-button
          type="primary"
          plain
          style="margin-left: 12px;"
          :disabled="pendingCount === 0"
          @click="startPipeline"
        >
          开始流水批阅
        </el-button>
        <el-button
          type="success"
          plain
          style="margin-left: 8px;"
          :disabled="selectedRows.length === 0"
          @click="runBatch('SETTLE_NO_SUBJECTIVE')"
        >
          批量结算(无主观题)
        </el-button>
        <el-button
          type="warning"
          plain
          :disabled="selectedRows.length === 0"
          @click="runBatch('REOPEN_MARKING')"
        >
          批量打回待阅
        </el-button>
        <el-button plain @click="openHistory">批阅历史</el-button>
        <el-tag style="margin-left: 8px;" type="info">已选 {{ selectedRows.length }}</el-tag>
        <el-input
          v-model="keyword"
          placeholder="搜索学生姓名/学号"
          :prefix-icon="Search"
          clearable
          style="width: 240px; margin-left: 12px;"
        />
        <el-select v-model="pendingTargetId" placeholder="快速定位未批学生" clearable style="width: 240px; margin-left: 8px;">
          <el-option
            v-for="item in pendingQuickList"
            :key="item.id"
            :label="`${item.studentName}（${item.studentNo}）`"
            :value="item.id"
          />
        </el-select>
        <el-button type="primary" plain :disabled="!pendingTargetId" @click="openPendingById">定位</el-button>
      </div>

      <el-table
        :data="filteredRecords"
        class="custom-table"
        stripe
        v-loading="loading"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="#" type="index" width="70" align="center" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="submitTime" label="交卷时间" width="180" />
        <el-table-column label="客观题分" width="120" align="center">
          <template #default="scope">
            <span class="score-text blue">{{ scope.row.objectiveScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="主观题分" width="120" align="center">
          <template #default="scope">
            <span v-if="scope.row.status === 'GRADED'" class="score-text green">{{ scope.row.subjectiveScore }}</span>
            <el-tag v-else type="warning" size="small">待批阅</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="100" align="center">
          <template #default="scope">
            <span class="score-text red" v-if="scope.row.status === 'GRADED'">{{ scope.row.totalScore }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'GRADED' ? 'success' : 'warning'">
              {{ scope.row.status === 'GRADED' ? '已批阅' : '待批阅' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="110" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="openMarkingDrawer(scope.row)">
              {{ scope.row.status === 'GRADED' ? '重新批阅' : '去批阅' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="recordPager.page"
        v-model:page-size="recordPager.size"
        :total="recordPager.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end;"
        @size-change="fetchRecords"
        @current-change="fetchRecords"
      />
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="55%"
      destroy-on-close
    >
      <div class="drawer-content">
        <el-alert title="客观题已自动判分。请为简答题打分，提交后可自动跳转下一份待阅试卷。" type="success" show-icon />

        <div v-if="subjectiveList.length === 0" class="empty-text">
          该试卷没有需要人工批阅的主观题
        </div>

        <div class="q-card" v-for="(item, index) in subjectiveList" :key="item.answerId">
          <div class="q-header">
            <span class="q-type">简答题 {{ index + 1 }}</span>
            <span class="q-score">满分 {{ item.maxScore }} 分</span>
          </div>
          <div class="q-stem" v-html="item.stem"></div>

          <div class="q-answer">
            <div class="ans-label">学生作答</div>
            <div class="ans-content">{{ item.studentAnswer || '未作答' }}</div>
          </div>

          <div class="q-answer standard">
            <div class="ans-label">参考答案</div>
            <div class="ans-content">{{ item.standardAnswer || '暂无参考答案' }}</div>
          </div>

          <div class="q-grading-box">
            <el-input-number v-model="item.givenScore" :min="0" :max="item.maxScore" :step="1" />
            <el-button size="small" @click="quickScore(item, 'zero')">0分</el-button>
            <el-button size="small" @click="quickScore(item, 'half')">半分</el-button>
            <el-button size="small" @click="quickScore(item, 'full')">满分</el-button>
          </div>
          <div class="comment-box">
            <div class="ans-label">教师评语</div>
            <div class="comment-actions">
              <el-button size="small" @click="applyComment(item, '内容完整，逻辑清晰。')">模板1</el-button>
              <el-button size="small" @click="applyComment(item, '要点基本齐全，但表达可更准确。')">模板2</el-button>
              <el-button size="small" @click="applyComment(item, '答案偏离题意，请结合参考答案复习。')">模板3</el-button>
            </div>
            <el-input v-model="item.teacherComment" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <div>
            当前主观题总分：
            <b class="score-red">{{ totalGiveScore }}</b>
          </div>
          <div class="footer-actions">
            <el-checkbox v-model="autoNext">提交后自动下一份</el-checkbox>
            <el-button @click="drawerVisible = false">取消</el-button>
            <el-button type="primary" :loading="grading" @click="submitScore">提交判分</el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="historyVisible" title="批阅历史追踪" width="78%">
      <div class="history-toolbar">
        <el-select v-model="historyFilters.action" placeholder="动作" clearable style="width: 120px;">
          <el-option label="批阅提交" value="GRADE_RECORD" />
          <el-option label="批量操作" value="BATCH_RECORD_ACTION" />
        </el-select>
        <el-select v-model="historyFilters.operatorId" placeholder="操作人" clearable style="width: 160px;">
          <el-option v-for="op in historyOperatorOptions" :key="op.id" :label="op.name" :value="op.id" />
        </el-select>
        <el-date-picker
          v-model="historyFilters.timeRange"
          type="datetimerange"
          range-separator="到"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <el-input v-model="historyFilters.keyword" placeholder="关键词(学号/姓名/详情)" clearable style="width: 220px;" />
        <el-button type="primary" @click="fetchHistory">查询</el-button>
        <el-button @click="resetHistoryFilters">重置</el-button>
        <el-button type="success" plain @click="exportHistoryCsv">导出CSV</el-button>
      </div>
      <el-table :data="historyRows" v-loading="historyLoading" size="small" max-height="520">
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="actionText" label="动作" width="110" />
        <el-table-column prop="recordId" label="记录ID" width="90" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="detail" label="详情" min-width="380" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Back, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { exportCsvRows } from '../utils/export'
import LoadErrorBar from '../components/LoadErrorBar.vue'

const route = useRoute()
const examId = route.params.examId

const statusFilter = ref(route.query.status ? String(route.query.status) : 'MARKING')
const keyword = ref(route.query.studentNo ? String(route.query.studentNo) : '')
const loading = ref(false)
const loadError = ref('')
const grading = ref(false)
const drawerVisible = ref(false)
const autoNext = ref(true)
const currentRecord = ref(null)
const recordList = ref([])
const subjectiveList = ref([])
const selectedRows = ref([])
const pendingTargetId = ref(null)
const recordPager = ref({ page: 1, size: 20, total: 0 })
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRows = ref([])
const historyFilters = ref({
  action: '',
  operatorId: null,
  timeRange: [],
  keyword: ''
})

const allCount = computed(() => recordList.value.length)
const pendingCount = computed(() => recordList.value.filter(x => x.status === 'MARKING').length)
const gradedCount = computed(() => recordList.value.filter(x => x.status === 'GRADED').length)

const filteredRecords = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return recordList.value.filter(r => {
    const hitStatus = statusFilter.value ? r.status === statusFilter.value : true
    const hitKeyword = !kw
      || String(r.studentName || '').toLowerCase().includes(kw)
      || String(r.studentNo || '').toLowerCase().includes(kw)
    return hitStatus && hitKeyword
  })
})
const pendingQuickList = computed(() => recordList.value.filter(x => x.status === 'MARKING'))

const drawerTitle = computed(() => {
  if (!currentRecord.value) return '批阅中心'
  return `批阅：${currentRecord.value.studentName}（${currentRecord.value.studentNo}）`
})

const totalGiveScore = computed(() =>
  subjectiveList.value.reduce((sum, item) => sum + (Number(item.givenScore) || 0), 0)
)

const fetchRecords = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get(`/teacher/records/exam/${examId}`, {
      params: { page: recordPager.value.page, size: recordPager.value.size }
    })
    recordList.value = res.list || []
    recordPager.value.total = Number(res.total || 0)
    selectedRows.value = []
    if (pendingTargetId.value && !recordList.value.some(x => Number(x.id) === Number(pendingTargetId.value))) {
      pendingTargetId.value = null
    }
  } catch (e) {
    loadError.value = e?.response?.data?.error || '加载阅卷记录失败'
  } finally {
    loading.value = false
  }
}

const onSelectionChange = (rows) => {
  selectedRows.value = rows || []
}

const openPendingById = () => {
  if (!pendingTargetId.value) return
  const row = recordList.value.find(x => Number(x.id) === Number(pendingTargetId.value))
  if (row) {
    openMarkingDrawer(row)
  }
}

const openMarkingDrawer = async (row) => {
  currentRecord.value = row
  drawerVisible.value = true
  subjectiveList.value = []
  const res = await request.get(`/teacher/records/${row.id}/subjective-answers`)
  subjectiveList.value = (res || []).map(item => ({
    ...item,
    givenScore: Number(item.givenScore || 0),
    teacherComment: item.teacherComment || ''
  }))
}

const quickScore = (item, mode) => {
  if (mode === 'zero') item.givenScore = 0
  if (mode === 'half') item.givenScore = Math.floor((item.maxScore || 0) / 2)
  if (mode === 'full') item.givenScore = item.maxScore || 0
}

const pickNextPending = (currentId) =>
  recordList.value.find(r => r.status === 'MARKING' && r.id !== currentId)

const startPipeline = async () => {
  const next = recordList.value.find(r => r.status === 'MARKING')
  if (!next) {
    ElMessage.info('当前没有待批阅试卷')
    return
  }
  await openMarkingDrawer(next)
}

const applyComment = (item, content) => {
  item.teacherComment = content
}

const runBatch = async (action) => {
  if (selectedRows.value.length === 0) return
  const recordIds = selectedRows.value.map(x => x.id)
  const res = await request.post('/teacher/records/batch-action', { action, recordIds })
  const updated = Number(res?.updated || 0)
  const skipped = Array.isArray(res?.skipped) ? res.skipped.length : 0
  ElMessage.success(`批量完成：成功 ${updated}，跳过 ${skipped}`)
  await fetchRecords()
}

const openHistory = async () => {
  historyVisible.value = true
  await fetchHistory()
}

const historyOperatorOptions = computed(() => {
  const map = new Map()
  for (const row of historyRows.value) {
    if (row?.operatorId == null) continue
    if (!map.has(row.operatorId)) {
      map.set(row.operatorId, row.operator || `user#${row.operatorId}`)
    }
  }
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }))
})

const fetchHistory = async () => {
  historyLoading.value = true
  try {
    const params = {}
    if (historyFilters.value.action) params.action = historyFilters.value.action
    if (historyFilters.value.operatorId != null) params.operatorId = historyFilters.value.operatorId
    if (historyFilters.value.keyword?.trim()) params.keyword = historyFilters.value.keyword.trim()
    const [startTime, endTime] = historyFilters.value.timeRange || []
    if (startTime) params.startTime = startTime
    if (endTime) params.endTime = endTime
    historyRows.value = await request.get(`/teacher/records/exam/${examId}/grading-history`, { params })
  } finally {
    historyLoading.value = false
  }
}

const resetHistoryFilters = async () => {
  historyFilters.value = { action: '', operatorId: null, timeRange: [], keyword: '' }
  await fetchHistory()
}

const exportHistoryCsv = () => {
  if (historyRows.value.length === 0) {
    ElMessage.info('暂无可导出的历史记录')
    return
  }
  exportCsvRows(
    ['时间', '操作人', '动作', '记录ID', '学号', '姓名', '详情'],
    historyRows.value.map(row => [
      row.createdAt,
      row.operator,
      row.actionText,
      row.recordId,
      row.studentNo,
      row.studentName,
      row.detail
    ]),
    `grading-history-${examId}.csv`
  )
}

const submitScore = async () => {
  if (!currentRecord.value) return
  grading.value = true
  try {
    const subjectiveItems = subjectiveList.value.map(item => ({
      answerId: item.answerId,
      givenScore: Number(item.givenScore || 0),
      teacherComment: String(item.teacherComment || '').trim()
    }))
    await request.post(`/teacher/records/${currentRecord.value.id}/grade`, {
      subjectiveScore: totalGiveScore.value,
      subjectiveItems
    })
    ElMessage.success('批阅成功')
    const currentId = currentRecord.value.id
    await fetchRecords()

    if (autoNext.value) {
      const next = pickNextPending(currentId)
      if (next) {
        await openMarkingDrawer(next)
        ElMessage.info(`已切换到下一份：${next.studentName}`)
        return
      }
    }
    drawerVisible.value = false
  } finally {
    grading.value = false
  }
}

onMounted(fetchRecords)
</script>

<style scoped>
.marking-container { height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }
.page-header { height: 60px; background: #fff; display: flex; justify-content: space-between; align-items: center; padding: 0 24px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
.page-header .left { cursor: pointer; color: #606266; font-size: 14px; display: flex; align-items: center; gap: 5px; }
.page-header .title { font-size: 16px; font-weight: 700; color: #303133; }
.main-card { margin: 20px; border: none; flex: 1; overflow: hidden; }
.summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.filter-bar { margin-bottom: 14px; display: flex; align-items: center; }
.score-text { font-weight: 700; }
.score-text.blue { color: #409eff; }
.score-text.green { color: #67c23a; }
.score-text.red { color: #f56c6c; }
.drawer-content { padding: 0 16px; }
.empty-text { text-align: center; color: #909399; margin-top: 40px; }
.q-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 16px; margin-top: 14px; background: #fafafa; }
.q-header { display: flex; justify-content: space-between; margin-bottom: 10px; }
.q-type { color: #409eff; background: #ecf5ff; padding: 4px 10px; border-radius: 4px; font-size: 13px; font-weight: 600; }
.q-score { color: #909399; font-size: 13px; }
.q-stem { margin-bottom: 10px; line-height: 1.7; }
.q-answer { margin-top: 10px; background: #fff; border: 1px solid #dcdfe6; border-radius: 6px; padding: 12px; }
.q-answer.standard { background: #f0f9eb; border-color: #e1f3d8; }
.ans-label { font-size: 12px; color: #909399; margin-bottom: 6px; }
.ans-content { color: #303133; }
.q-grading-box { display: flex; align-items: center; gap: 8px; margin-top: 12px; justify-content: flex-end; }
.comment-box { margin-top: 10px; }
.comment-actions { display: flex; gap: 8px; margin-bottom: 6px; }
.drawer-footer { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.score-red { color: #f56c6c; font-size: 18px; margin-left: 4px; }
.footer-actions { display: flex; align-items: center; gap: 10px; }
.history-toolbar { display: flex; gap: 8px; align-items: center; margin-bottom: 12px; flex-wrap: wrap; }
</style>
