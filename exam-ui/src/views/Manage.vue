<template>
  <el-card shadow="never">
    <template #header>
      <span style="font-weight:600;">管理中心</span>
    </template>

    <el-tabs v-model="active">
      <el-tab-pane label="补考审批" name="makeup">
        <el-table :data="makeupList" v-loading="loadingMakeup">
          <el-table-column prop="examTitle" label="考试" min-width="180" />
          <el-table-column prop="studentNo" label="学号" width="140" />
          <el-table-column prop="studentName" label="姓名" width="120" />
          <el-table-column prop="reason" label="申请原因" min-width="220" />
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <el-button size="small" type="success" @click="review(scope.row, 'APPROVED')" :disabled="scope.row.status !== 'PENDING'">批准</el-button>
              <el-button size="small" type="danger" @click="review(scope.row, 'REJECTED')" :disabled="scope.row.status !== 'PENDING'">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="平行组" name="parallel">
        <el-table :data="parallelGroups" v-loading="loadingGroup">
          <el-table-column prop="id" label="ID" width="100" />
          <el-table-column prop="name" label="组名" min-width="200" />
          <el-table-column label="试卷ID" min-width="220">
            <template #default="scope">{{ (scope.row.paperIds || []).join(', ') }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const active = ref('makeup')
const makeupList = ref([])
const parallelGroups = ref([])
const loadingMakeup = ref(false)
const loadingGroup = ref(false)

const fetchMakeup = async () => {
  loadingMakeup.value = true
  try {
    makeupList.value = await request.get('/teacher/makeup-requests')
  } finally {
    loadingMakeup.value = false
  }
}

const fetchGroups = async () => {
  loadingGroup.value = true
  try {
    parallelGroups.value = await request.get('/teacher/parallel-groups')
  } finally {
    loadingGroup.value = false
  }
}

const review = async (row, status) => {
  await request.post(`/teacher/makeup-requests/${row.id}/review`, {
    status,
    teacherComment: status === 'APPROVED' ? '审核通过' : '审核未通过',
    approvedExtraMinutes: status === 'APPROVED' ? 20 : null
  })
  ElMessage.success('已处理')
  fetchMakeup()
}

onMounted(() => {
  fetchMakeup()
  fetchGroups()
})
</script>
