<template>
  <div class="student-exam-container">
    <div class="welcome-banner">
      <div class="user-info">
        <el-avatar :size="60" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
        <div class="text">
          <h2>你好，{{ currentUsername }} 同学！</h2>
          <p>今天共有 {{ ongoingExams.length }} 场考试正在进行中，请注意时间，准时参加。</p>
        </div>
      </div>
    </div>

    <div class="exam-grid">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="exam in tableData" :key="exam.id">
          <el-card class="exam-card" :class="exam.status" shadow="hover">
            <div class="card-tag">{{ formatStatus(exam.status) }}</div>
            <h3 class="exam-title" :title="exam.title">{{ exam.title }}</h3>
            
            <div class="exam-info">
              <p><el-icon color="#409EFF"><Calendar /></el-icon> <span>开始：{{ formatTime(exam.startTime) }}</span></p>
              <p><el-icon color="#F56C6C"><Timer /></el-icon> <span>结束：{{ formatTime(exam.endTime) }}</span></p>
            </div>
            
            <div class="card-footer">
              <el-button 
                type="primary" 
                size="large"
                class="enter-btn"
                :disabled="exam.status !== 'ONGOING'"
                @click="enterExam(exam.id)"
              >
                {{ exam.status === 'ONGOING' ? '立即进入考场' : (exam.status === 'NOT_STARTED' ? '等待开考' : '考试已结束') }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-empty v-if="tableData.length === 0" description="近期暂无考试安排，去复习一下错题本吧！" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Timer } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const currentUsername = ref(localStorage.getItem('username') || '学生')
const tableData = ref([])
const loading = ref(false)

const ongoingExams = computed(() => tableData.value.filter(e => e.status === 'ONGOING'))
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '-'
const formatStatus = (s) => ({ 'NOT_STARTED': '未开始', 'ONGOING': '考试中', 'FINISHED': '已结束' }[s] || s)

const fetchExams = async () => {
  loading.value = true
  try {
    // 请求我们在后端刚加的 list 接口
    const res = await request.get('/student/exams/list', {
      params: { studentName: currentUsername.value }
    })
    tableData.value = res
  } finally {
    loading.value = false
  }
}

// 跳转到考场
const enterExam = (examId) => {
  // 必须与 router/index.js 中配置的考场路径一致
  router.push(`/exam-room/${examId}`) 
}

onMounted(fetchExams)
</script>

<style scoped>
.student-exam-container { padding: 20px; }
.welcome-banner { 
  background: linear-gradient(135deg, #4D58B5 0%, #303f9f 100%); 
  padding: 30px 40px; 
  border-radius: 12px; 
  color: #fff; 
  margin-bottom: 30px; 
  box-shadow: 0 4px 12px rgba(77, 88, 181, 0.2);
}
.user-info { display: flex; align-items: center; gap: 20px; }
.text h2 { margin: 0 0 10px 0; font-size: 24px; font-weight: 600; letter-spacing: 1px; }
.text p { margin: 0; font-size: 14px; opacity: 0.9; }

.exam-grid { padding: 10px 0; }
.exam-card { 
  border-radius: 12px; 
  margin-bottom: 20px; 
  position: relative; 
  overflow: hidden; 
  transition: all 0.3s ease; 
  border: none;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}
.exam-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.exam-card.ONGOING { border-top: 4px solid #67c23a; }
.exam-card.NOT_STARTED { border-top: 4px solid #e6a23c; }
.exam-card.FINISHED { border-top: 4px solid #909399; opacity: 0.8; }

.card-tag { 
  position: absolute; right: -30px; top: 15px; 
  color: #fff; transform: rotate(45deg); 
  width: 120px; text-align: center; font-size: 12px; padding: 4px 0; font-weight: bold;
}
.ONGOING .card-tag { background: #67c23a; }
.NOT_STARTED .card-tag { background: #e6a23c; }
.FINISHED .card-tag { background: #909399; }

.exam-title { margin: 10px 0 20px; font-size: 18px; color: #303133; height: 26px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.exam-info p { margin: 10px 0; font-size: 14px; color: #606266; display: flex; align-items: center; gap: 8px; }
.card-footer { margin-top: 25px; text-align: center; }
.enter-btn { width: 100%; border-radius: 8px; font-weight: bold; letter-spacing: 1px; }
</style>