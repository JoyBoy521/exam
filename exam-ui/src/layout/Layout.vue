<template>
  <el-container class="chaoxing-layout">
    <el-aside width="250px" class="aside">
      <div class="aside-inner">
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
          <el-menu-item index="/courses">
            <el-icon><Document /></el-icon>
            <template #title>课程看板</template>
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
      </div>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Monitor, Files, Document, DataLine, Setting, UserFilled, ArrowDown
} from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const currentUsername = ref(localStorage.getItem('username') || '教师')

const handleCommand = (command) => {
  if (command === 'logout') {
    handleLogout()
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

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
  }).catch(() => {})
}

const currentRouteName = computed(() => {
  const map = {
    '/question': '题库管理',
    '/paper': '试卷管理',
    '/exam': '考试发布',
    '/monitor': '教师监考台',
    '/class': '班级管理',
    '/courses': '课程看板',
    '/stat': '统计分析',
    '/profile': '个人中心',
    '/manage': '管理中心'
  }
  return map[route.path] || '课程管理'
})

onMounted(() => {
  const key = 'onboarding_teacher_layout_v1'
  if (localStorage.getItem(key)) return
  ElMessage.info('教师端建议流程：题库 -> 试卷 -> 发布考试 -> 监考台 -> 阅卷 -> 统计。')
  localStorage.setItem(key, '1')
})
</script>

<style scoped>
.chaoxing-layout {
  height: 100vh;
  background-color: var(--app-bg-color); 
}

/* 侧边栏改造：去除右侧边框，融入背景 */
.aside {
  background-color: transparent;
  border-right: none;
  display: flex;
  flex-direction: column;
  padding: 24px 0 24px 24px; /* 让侧边栏浮起来一点，四周留白 */
}

/* 侧边栏内部的主体容器 */
.aside-inner {
  background: #ffffff;
  border-radius: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-shadow: var(--soft-shadow);
  overflow: hidden;
}

.course-header {
  padding: 30px 20px 20px;
  text-align: center;
}
.course-cover {
  display: block;
  position: relative;
  width: 100%;
  height: 100px;
  border-radius: 12px;
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
  font-size: 20px;
  font-weight: 800;
  color: #2b3674;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 菜单项胶囊化改造 */
.chaoxing-menu {
  border-right: none;
  padding: 0 16px;
  overflow-y: auto;
}
.chaoxing-menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  color: #a3aed1;
  font-size: 15px;
  font-weight: 600;
  border-radius: 16px; /* 胶囊圆角 */
  margin-bottom: 8px;
  border-left: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.chaoxing-menu :deep(.el-menu-item:hover) {
  color: var(--el-color-primary);
  background-color: #f4f7fe;
  transform: translateX(4px); /* hover时轻微右移 */
}
.chaoxing-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background-color: var(--el-color-primary);
  box-shadow: 0 8px 16px rgba(67, 24, 255, 0.2);
}

.main-container {
  display: flex;
  flex-direction: column;
}

.header {
  background-color: transparent;
  height: 80px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
}

/* 面包屑文字加大加粗 */
:deep(.el-breadcrumb__inner) {
  font-size: 16px;
  font-weight: 700;
  color: #2b3674;
}
:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--el-color-primary);
}

.user-trigger {
  display: flex;
  align-items: center;
  cursor: pointer;
  background: #ffffff;
  padding: 6px 16px 6px 6px;
  border-radius: 30px;
  box-shadow: var(--soft-shadow);
  transition: transform 0.2s;
}
.user-trigger:hover {
  transform: translateY(-2px);
}
.user-name-text {
  margin-left: 12px;
  margin-right: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #2b3674;
}

.main-content {
  padding: 0 40px 40px 40px; /* 顶部不需要padding，因为header有足够空间 */
  overflow-y: auto;
}
</style>
