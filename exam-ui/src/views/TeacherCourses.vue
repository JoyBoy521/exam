<template>
  <div class="teacher-courses" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>课程看板</span>
          <el-button size="small" @click="fetchData">刷新</el-button>
        </div>
      </template>
      <el-empty v-if="rows.length === 0" description="暂无课程数据" />
      <el-row v-else :gutter="16">
        <el-col v-for="c in rows" :key="c.id" :xs="24" :sm="12" :lg="8">
          <el-card shadow="hover" class="card">
            <h3>{{ c.title }}</h3>
            <p class="desc">{{ c.description || '暂无简介' }}</p>
            <div class="kpi-grid">
              <div>班级 <b>{{ c.classCount || 0 }}</b></div>
              <div>学生规模 <b>{{ c.studentCount || 0 }}</b></div>
              <div>考试 <b>{{ c.examCount || 0 }}</b></div>
              <div>进行中 <b>{{ c.ongoingExamCount || 0 }}</b></div>
              <div>待批阅 <b>{{ c.pendingMarkingCount || 0 }}</b></div>
              <div>近7天风险 <b>{{ c.riskEvents7d || 0 }}</b></div>
            </div>
            <div class="actions">
              <el-button type="primary" link @click="goExam(c)">考试管理</el-button>
              <el-button type="primary" link @click="router.push('/monitor')">监考台</el-button>
              <el-button type="primary" link @click="router.push('/stat')">统计分析</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const rows = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    rows.value = await request.get('/teacher/courses')
  } finally {
    loading.value = false
  }
}

const goExam = (course) => {
  router.push(`/exam?courseId=${course.id}`)
}

onMounted(fetchData)
</script>

<style scoped>
.teacher-courses { padding: 8px; }
.head { display: flex; align-items: center; justify-content: space-between; }
.card { margin-bottom: 12px; }
.desc { color: #7b8797; min-height: 36px; }
.kpi-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; font-size: 13px; color: #617083; }
.kpi-grid b { color: #202a39; }
.actions { margin-top: 10px; }
</style>
