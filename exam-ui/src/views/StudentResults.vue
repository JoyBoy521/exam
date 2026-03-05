<template>
  <div class="results-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">我的考试成绩单</span>
        </div>
      </template>

      <el-table :data="recordList" style="width: 100%" v-loading="loading">
        <el-table-column prop="examTitle" label="考试名称" min-width="250" />
        <el-table-column prop="submitTime" label="提交时间" width="200" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'GRADED' ? 'success' : 'warning'">
              {{ scope.row.status === 'GRADED' ? '已出分' : '阅卷中' }}
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const recordList = ref([])
const loading = ref(false)

const fetchRecords = async () => {
  loading.value = true
  try {
    recordList.value = await request.get('/student/exams/my-records')
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/student/result-detail/${id}`)
}

onMounted(fetchRecords)
</script>

<style scoped>
.title { font-size: 18px; font-weight: bold; color: #4D58B5; }
.score-num { font-size: 20px; font-weight: bold; color: #f56c6c; }
</style>
