<template>
  <el-container class="chaoxing-layout">
    <el-aside width="220px" class="aside">
      <div class="course-header">
        <a href="javascript:void(0);" class="course-cover">
          <img src="http://p.ananas.chaoxing.com/star3/138_78c/3a7c4e2929905646967168696f78ff01.png" alt="course-cover">
          <div class="cover-mask">
            <span class="portal-text">课程门户</span>
            <span class="portal-link">链接</span>
          </div>
        </a>
        <div class="course-title" title="智能考试系统">智能考试系统</div>
      </div>

      <el-menu
        :default-active="$route.path"
        class="chaoxing-menu"
        router
      >
        <el-menu-item index="/class">
          <el-icon><UserFilled /></el-icon>
          <template #title>班级管理</template>
        </el-menu-item>
        <el-menu-item index="/chapter">
          <el-icon><Reading /></el-icon>
          <template #title>章节</template>
        </el-menu-item>
        <el-menu-item index="/data">
          <el-icon><Folder /></el-icon>
          <template #title>资料</template>
        </el-menu-item>
        <el-menu-item index="/notice">
          <el-icon><Bell /></el-icon>
          <template #title>通知</template>
        </el-menu-item>
        <el-menu-item index="/discuss">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>讨论</template>
        </el-menu-item>
        <el-menu-item index="/homework">
          <el-icon><Edit /></el-icon>
          <template #title>作业</template>
        </el-menu-item>

        <el-menu-item index="/exam">
          <el-icon><Monitor /></el-icon>
          <template #title>考试</template>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><Monitor /></el-icon>
          <template #title>监考台</template>
        </el-menu-item>
        <el-menu-item index="/question">
          <el-icon><Files /></el-icon>
          <template #title>题库</template>
        </el-menu-item>
        <el-menu-item index="/paper">
          <el-icon><Document /></el-icon>
          <template #title>试卷</template>
        </el-menu-item>

        <el-menu-item index="/stat">
          <el-icon><DataLine /></el-icon>
          <template #title>统计</template>
        </el-menu-item>
        <el-menu-item index="/manage">
          <el-icon><Setting /></el-icon>
          <template #title>管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>南阳师范学院</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-trigger">
              <el-avatar size="small" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span class="user-name-text">{{ currentUsername }}</span>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router' // 引入 useRouter 用于跳转
import { 
  Reading, Folder, Bell, ChatDotRound, Edit, 
  Monitor, Files, Document, DataLine, Setting, UserFilled, ArrowDown
} from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const currentUsername = ref(localStorage.getItem('username') || '教师')

// 处理下拉菜单点击事件
const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    ElMessage.info('个人信息功能开发中...')
  }
}

// 【核心退出逻辑】
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出管理后台吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.post('/auth/logout').finally(() => {
      localStorage.clear()
      ElMessage.success('已成功安全退出')
      router.push('/login')
    })
  }).catch(() => {
    // 点击取消则不做任何操作
  })
}

const currentRouteName = computed(() => {
  const map = {
    '/chapter': '章节管理',
    '/data': '资料管理',
    '/notice': '通知管理',
    '/discuss': '讨论管理',
    '/homework': '作业管理',
    '/question': '题库管理',
    '/paper': '试卷管理',
    '/exam': '考试发布',
    '/monitor': '教师监考台',
    '/class': '班级管理',
    '/stat': '统计分析',
    '/manage': '管理中心'
  }
  return map[route.path] || '课程管理'
})
</script>

<style scoped>
.chaoxing-layout {
  height: 100vh;
  background-color: #f0f2f5;
}

.aside {
  background-color: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.course-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}
.course-cover {
  display: block;
  position: relative;
  width: 100%;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 15px;
}
.course-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-mask {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 12px;
  padding: 4px 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.portal-link {
  background: rgba(255,255,255,0.2);
  padding: 2px 6px;
  border-radius: 4px;
}
.course-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chaoxing-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}
.chaoxing-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 4px;
  border-left: 4px solid transparent;
}
.chaoxing-menu :deep(.el-menu-item:hover) {
  color: #3A8BFF;
  background-color: #F5F7FA;
}
.chaoxing-menu :deep(.el-menu-item.is-active) {
  color: #3A8BFF;
  background-color: #EBF3FF;
  border-left: 4px solid #3A8BFF;
  font-weight: bold;
}

.main-container {
  display: flex;
  flex-direction: column;
}
.header {
  background-color: #fff;
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0,21,41,.05);
  z-index: 10;
  padding: 0 24px;
}

/* 用户下拉区域样式 */
.user-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.3s;
}
.user-trigger:hover {
  background: #f5f7fa;
}
.user-name-text {
  margin-left: 8px;
  font-size: 14px;
  color: #333;
}

.main-content {
  padding: 20px;
  overflow-y: auto;
}
</style>
