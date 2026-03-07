<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <span class="title">补考申请记录</span>
        <div class="filters">
          <el-select v-model="filters.status" clearable placeholder="状态" style="width: 120px;" @change="onFilterChange">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已撤回" value="CANCELED" />
          </el-select>
          <el-input v-model="filters.keyword" clearable placeholder="搜索考试名/原因" style="width: 220px;" @clear="onFilterChange" />
          <el-button type="primary" plain @click="onFilterChange">筛选</el-button>
        </div>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="examTitle" label="考试" min-width="180" />
      <el-table-column prop="reason" label="申请原因" min-width="220" show-overflow-tooltip />
      <el-table-column prop="requestedAt" label="申请时间" width="170">
        <template #default="scope">{{ formatDateTime(scope.row.requestedAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag :type="makeupStatusMeta(scope.row.status).tag">{{ makeupStatusMeta(scope.row.status).text }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="teacherComment" label="审核意见" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button type="primary" link @click="openDetail(scope.row)">详情</el-button>
          <el-button v-if="scope.row.status === 'PENDING'" type="danger" link @click="cancel(scope.row.id)">撤回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pager.page"
      v-model:page-size="pager.size"
      :total="pager.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end;"
      @size-change="fetchRows"
      @current-change="fetchRows"
    />
  </el-card>

  <el-dialog v-model="detailVisible" title="补考申请详情" width="560px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="考试">{{ detailRow.examTitle }}</el-descriptions-item>
      <el-descriptions-item label="申请状态">
        <el-tag :type="makeupStatusMeta(detailRow.status).tag">{{ makeupStatusMeta(detailRow.status).text }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="申请原因">{{ detailRow.reason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审核意见">{{ detailRow.teacherComment || '-' }}</el-descriptions-item>
      <el-descriptions-item label="补时时长(分钟)">{{ detailRow.approvedExtraMinutes ?? '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-divider>处理时间线</el-divider>
    <el-timeline>
      <el-timeline-item :timestamp="formatDateTime(detailRow.requestedAt)" placement="top">
        学生提交补考申请
      </el-timeline-item>
      <el-timeline-item v-if="detailRow.reviewedAt" :timestamp="formatDateTime(detailRow.reviewedAt)" placement="top">
        {{ makeupStatusMeta(detailRow.status).text }}：{{ detailRow.teacherComment || '无附加意见' }}
      </el-timeline-item>
    </el-timeline>
  </el-dialog>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { formatDateTime, makeupStatusMeta } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const pager = ref({ page: 1, size: 10, total: 0 })
const filters = ref({ status: '', keyword: '' })
const detailVisible = ref(false)
const detailRow = ref({})

const fetchRows = async () => {
  loading.value = true
  try {
    const res = await request.get('/student/makeup-requests', {
      params: {
        page: pager.value.page,
        size: pager.value.size,
        status: filters.value.status || undefined,
        keyword: filters.value.keyword?.trim() || undefined
      }
    })
    rows.value = res.list || []
    pager.value.total = Number(res.total || 0)
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  pager.value.page = 1
  fetchRows()
}

const cancel = (id) => {
  ElMessageBox.confirm('确认撤回该补考申请吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/student/makeup-requests/${id}`)
    ElMessage.success('已撤回')
    await fetchRows()
  }).catch(() => {})
}

const openDetail = (row) => {
  detailRow.value = { ...row }
  detailVisible.value = true
}

onMounted(fetchRows)
</script>

<style scoped>
.title { font-weight: 600; }
.header { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.filters { display: flex; align-items: center; gap: 8px; }
@media (max-width: 900px) {
  .header { flex-direction: column; align-items: stretch; }
  .filters { flex-wrap: wrap; }
}
</style>
