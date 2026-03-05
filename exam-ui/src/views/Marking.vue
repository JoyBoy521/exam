<template>
  <div class="marking-container">
    <div class="page-header">
      <div class="left" @click="$router.back()">
        <el-icon><Back /></el-icon> 返回考试列表
      </div>
      <div class="title">批阅中心</div>
      <div class="right-stats">
        <el-tag type="success" effect="dark" round>状态总览</el-tag>
      </div>
    </div>

    <el-card class="main-card" shadow="never">
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" size="small">
          <el-radio-button label="">全部记录</el-radio-button>
          <el-radio-button label="MARKING">待批阅主观题</el-radio-button>
          <el-radio-button label="GRADED">已完成批阅</el-radio-button>
        </el-radio-group>
        <el-input placeholder="搜索学生姓名/学号" prefix-icon="Search" style="width: 200px; margin-left: 20px;" />
      </div>

      <el-table :data="recordList" class="custom-table" stripe>
        <el-table-column label="排名" type="index" width="80" align="center" />
        <el-table-column prop="studentNo" label="学号" width="150" />
        <el-table-column prop="studentName" label="姓名" width="120">
          <template #default="scope">
            <span style="font-weight: bold;">{{ scope.row.studentName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" width="180" />
        <el-table-column label="客观题(系统判分)" width="150" align="center">
          <template #default="scope">
            <span class="score-text blue">{{ scope.row.objectiveScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="主观题(人工判分)" width="150" align="center">
          <template #default="scope">
            <span v-if="scope.row.status === 'GRADED'" class="score-text green">{{ scope.row.subjectiveScore }}</span>
            <el-tag v-else type="warning" size="small">待打分</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="120" align="center">
          <template #default="scope">
            <span class="score-text red" v-if="scope.row.status === 'GRADED'">
              {{ scope.row.totalScore }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="120" align="center">
          <template #default="scope">
            <el-button 
              type="primary" 
              link 
              @click="openMarkingDrawer(scope.row)"
            >
              {{ scope.row.status === 'GRADED' ? '重新批阅' : '去批阅' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer
      v-model="drawerVisible"
      :title="`正在批阅：${currentRecord.studentName} 的试卷`"
      size="50%"
      destroy-on-close
    >
      <div class="drawer-content" v-if="currentRecord">
        <el-alert title="客观题已由系统自动判分，您只需为简答题等主观题打分。" type="success" show-icon style="margin-bottom: 20px;" />

        <div v-if="subjectiveList.length === 0" style="text-align: center; color: #909399; margin-top: 50px;">
          该试卷没有需要人工批阅的主观题
        </div>

        <div class="q-card" v-for="(item, index) in subjectiveList" :key="item.answerId">
          <div class="q-header">
            <span class="q-type">简答题 第 {{ index + 1 }} 题</span>
            <span class="q-score">满分：{{ item.maxScore }} 分</span>
          </div>
          <div class="q-stem">{{ item.stem }}</div>
          
          <div class="q-student-answer">
            <div class="ans-label">学生作答：</div>
            <div class="ans-content">{{ item.studentAnswer || '未作答' }}</div>
          </div>

          <div class="q-standard-answer">
            <div class="ans-label">参考答案：</div>
            <div class="ans-content">{{ item.standardAnswer || '暂无参考答案' }}</div>
          </div>

          <div class="q-grading-box">
            <span style="font-weight: bold; margin-right: 15px;">本题打分：</span>
            <el-input-number v-model="item.givenScore" :min="0" :max="item.maxScore" :step="1" size="large" />
            <span style="margin-left: 10px; color: #909399;">/ {{ item.maxScore }} 分</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="color: #606266; font-size: 14px;">当前主观题得分总计：<b style="color: #f56c6c; font-size: 18px;">{{ totalGiveScore }}</b> 分</span>
          <div>
            <el-button @click="drawerVisible = false">取消</el-button>
            <el-button type="primary" @click="submitScore">提交判分</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Back, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const examId = route.params.examId

const statusFilter = ref('')
const drawerVisible = ref(false)
const currentRecord = ref({})
const recordList = ref([])
const subjectiveList = ref([]) // 存放真实的主观题列表

// 计算属性：自动把老师给每道题打的分数加起来
const totalGiveScore = computed(() => {
  return subjectiveList.value.reduce((sum, item) => sum + (item.givenScore || 0), 0)
})

// 1. 获取真实交卷记录
const fetchRecords = async () => {
  try {
    recordList.value = await request.get(`/teacher/records/exam/${examId}`)
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchRecords()
})

// 打开批阅抽屉并加载该学生的主观题答题记录
const openMarkingDrawer = async (row) => {
  currentRecord.value = row
  drawerVisible.value = true
  subjectiveList.value = [] // 每次打开先清空
  
  try {
    // 请求新的后端接口，拿到所有简答题详情
    const res = await request.get(`/teacher/records/${row.id}/subjective-answers`)
    subjectiveList.value = res 
  } catch (error) {
    ElMessage.error('获取学生答卷详情失败')
  }
}

// 2. 提交真实批阅分数
const submitScore = async () => {
  try {
    // 将计算好的总分提交给后端
    await request.post(`/teacher/records/${currentRecord.value.id}/grade`, {
      subjectiveScore: totalGiveScore.value
    })
    ElMessage.success('批阅成功！分数已记录入库。')
    drawerVisible.value = false
    fetchRecords() // 刷新列表，更新状态和总分
  } catch (error) {
    console.error(error)
  }
}
</script>

<style scoped>
.marking-container { height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; }
.page-header { height: 60px; background: #fff; display: flex; justify-content: space-between; align-items: center; padding: 0 24px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); z-index: 1; }
.page-header .left { cursor: pointer; color: #606266; font-size: 14px; display: flex; align-items: center; gap: 5px; }
.page-header .left:hover { color: #409eff; }
.page-header .title { font-size: 16px; font-weight: bold; color: #303133; }
.main-card { margin: 20px; border-radius: 8px; border: none; flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.filter-bar { margin-bottom: 20px; display: flex; align-items: center; }
.score-text { font-weight: bold; font-size: 16px; }
.score-text.blue { color: #409eff; }
.score-text.green { color: #67c23a; }
.score-text.red { color: #f56c6c; }

/* 阅卷抽屉里的样式 */
.drawer-content { padding: 0 20px; }
.q-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 24px; margin-bottom: 20px; background: #fafafa; }
.q-header { display: flex; justify-content: space-between; margin-bottom: 15px; }
.q-type { font-weight: bold; color: #409eff; background: #ecf5ff; padding: 4px 10px; border-radius: 4px; font-size: 13px; }
.q-score { color: #909399; font-size: 13px; }
.q-stem { font-size: 16px; color: #303133; margin-bottom: 20px; font-weight: 500; }
.ans-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.q-student-answer, .q-standard-answer { margin-bottom: 20px; background: #fff; padding: 15px; border-radius: 6px; border: 1px solid #dcdfe6; }
.q-student-answer .ans-content { color: #303133; font-size: 15px; }
.q-standard-answer { background: #f0f9eb; border-color: #e1f3d8; }
.q-standard-answer .ans-content { color: #67c23a; font-size: 14px; }
.q-grading-box { display: flex; align-items: center; justify-content: flex-end; padding-top: 20px; border-top: 1px dashed #dcdfe6; }
</style>