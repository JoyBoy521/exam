<template>
  <div class="results-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">我的考试成绩单</span>
          <div class="filters">
            <el-select v-model="filters.status" clearable placeholder="状态" style="width: 120px;" @change="onFilterChange">
              <el-option label="已出分" value="GRADED" />
              <el-option label="阅卷中" value="MARKING" />
            </el-select>
            <el-input v-model="filters.keyword" clearable placeholder="搜索考试名称" style="width: 220px;" @clear="onFilterChange" />
            <el-button type="primary" plain @click="onFilterChange">筛选</el-button>
          </div>
        </div>
      </template>
      <LoadErrorBar :message="loadError" @retry="fetchRecords" />

        <el-table :data="recordList" style="width: 100%" v-loading="loading">
          <el-table-column prop="examTitle" label="考试名称" min-width="250" />
          <el-table-column prop="submitTime" label="提交时间" width="200">
            <template #default="scope">{{ formatDateTime(scope.row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="recordStatus(scope.row.status).tag">
                {{ recordStatus(scope.row.status).text }}
              </el-tag>
            </template>
          </el-table-column>
        <el-table-column label="得分" width="120" align="center">
          <template #default="scope">
            <span class="score-num" v-if="scope.row.status === 'GRADED'">{{ scope.row.totalScore }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="scope">
            <el-button
              type="primary"
              link
              :disabled="scope.row.status !== 'GRADED'"
              @click="viewDetail(scope.row.id)"
            >查看解析</el-button>
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
        @size-change="fetchRecords"
        @current-change="fetchRecords"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import LoadErrorBar from '../components/LoadErrorBar.vue'
import { examRecordStatusMeta, formatDateTime } from '../utils/format'

const router = useRouter()
const route = useRoute()
const recordList = ref([])
const loading = ref(false)
const loadError = ref('')
const pager = ref({ page: 1, size: 10, total: 0 })
const filters = ref({ status: '', keyword: '' })

const fetchRecords = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get('/student/exams/my-records', {
      params: {
        page: pager.value.page,
        size: pager.value.size,
        courseId: route.query.courseId ? Number(route.query.courseId) : undefined,
        status: filters.value.status || undefined,
        keyword: filters.value.keyword?.trim() || undefined
      }
    })
    recordList.value = res.list || []
    pager.value.total = Number(res.total || 0)
  } catch (e) {
    loadError.value = e?.response?.data?.error || '加载成绩单失败'
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  pager.value.page = 1
  fetchRecords()
}

const viewDetail = (id) => {
  router.push(`/student/result-detail/${id}`)
}

const recordStatus = (status) => examRecordStatusMeta(status)

onMounted(fetchRecords)
</script>

<style scoped>
.title { font-size: 18px; font-weight: bold; color: #4D58B5; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.filters { display: flex; align-items: center; gap: 8px; }
.score-num { font-size: 20px; font-weight: bold; color: #f56c6c; }
</style>
