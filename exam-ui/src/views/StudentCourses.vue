<template>
  <div class="course-home" v-loading="loading">
    <el-card shadow="never" class="hero">
      <div class="hero-title">课程学习中心</div>
      <div class="hero-sub">先选课程，再进入该课程的考试、练习与错题复盘。</div>
    </el-card>

    <el-empty v-if="courses.length === 0" description="当前没有可学习课程" />

    <el-row v-else :gutter="16" style="margin-top: 12px;">
      <el-col v-for="c in courses" :key="c.id" :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" class="course-card" @click="openCourse(c)">
          <div class="cover" :style="{ backgroundImage: coverStyle(c.coverUrl) }">
            <el-tag size="small" :type="c.status === 'ACTIVE' ? 'success' : 'info'">
              {{ c.status === 'ACTIVE' ? '进行中' : c.status }}
            </el-tag>
          </div>
          <h3 class="title" :title="c.title">{{ c.title }}</h3>
          <p class="desc">{{ c.description || '暂无课程简介' }}</p>
          <div class="kpis">
            <div>进行中考试 <b>{{ c.ongoingExamCount || 0 }}</b></div>
            <div>待开考 <b>{{ c.upcomingExamCount || 0 }}</b></div>
            <div>课程错题 <b>{{ c.wrongCount || 0 }}</b></div>
          </div>
          <el-button type="primary" style="width:100%;margin-top:10px;">进入课程</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const courses = ref([])

const coverStyle = (url) => {
  if (url && String(url).trim()) return `url(${url})`
  return 'linear-gradient(135deg,#204f8f 0%,#0d2447 100%)'
}

const fetchCourses = async () => {
  loading.value = true
  try {
    courses.value = await request.get('/student/courses')
  } finally {
    loading.value = false
  }
}

const openCourse = (course) => {
  if (!course?.id) return
  router.push(`/student/course/${course.id}`)
}

onMounted(fetchCourses)
</script>

<style scoped>
.course-home { padding: 10px; }
.hero { border: none; }
.hero-title { font-size: 24px; font-weight: 700; color: #1b3158; }
.hero-sub { color: #5f6b7b; margin-top: 4px; }
.course-card { border: none; cursor: pointer; margin-bottom: 12px; }
.cover {
  height: 120px;
  border-radius: 10px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 10px;
}
.title { margin: 12px 0 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.desc { margin: 0; color: #7b8697; min-height: 36px; line-height: 1.4; }
.kpis { margin-top: 10px; display: grid; gap: 6px; font-size: 13px; color: #637083; }
.kpis b { color: #1f2a3d; }
</style>
