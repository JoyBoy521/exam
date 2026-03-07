<template>
  <div class="student-container">
    <header class="header">
      <div class="logo-area">
        <h3 title="南阳师范学院">南阳师范学院</h3>
      </div>
      <div class="header-right">
        <div class="user-info">
          <img class="avatar" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" alt="头像">
          <span class="username">{{ studentName }}</span>
          <el-dropdown @command="handleCommand">
            <span class="el-dropdown-link">
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div class="main-body">
      <aside class="sidebar">
        <div class="user-block">
          <img class="head-img" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png">
          <p class="user-name-label">{{ studentName }}</p>
        </div>
        <nav class="menu-list">
          <ul>
            <li v-for="item in menuItems" :key="item.path" 
                :class="{ active: currentPath.includes(item.path) }"
                @click="navigateTo(item.path)">
              <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
              <span class="menu-text">{{ item.name }}</span>
            </li>
          </ul>
        </nav>
      </aside>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Monitor, Document, Edit, Reading, Bell, ArrowDown, Collection } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 这里的姓名建议从 localStorage 获取
const studentName = ref(localStorage.getItem('username') || '学生')
const currentPath = computed(() => route.path)

// 侧边栏功能精简：只保留核心考试业务
const menuItems = [
  { name: '课程中心', path: '/student/courses', icon: Collection },
  { name: '考试中心', path: '/student/exam', icon: Monitor },
  { name: '我的成绩', path: '/student/results', icon: Document },
  { name: '错题本', path: '/student/wrong-books', icon: Edit },
  { name: '错题重练', path: '/student/wrong-practice', icon: Reading },
  { name: '补考申请', path: '/student/makeup', icon: Document },
  { name: '系统通知', path: '/student/notices', icon: Bell },
]

const navigateTo = (path) => {
  router.push(path)
}

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/student/profile')
    return
  }
  if (command === 'logout') {
    localStorage.clear()
    router.push('/login')
  }
}

onMounted(() => {
  const key = 'onboarding_student_layout_v1'
  if (localStorage.getItem(key)) return
  ElMessage.info('学生端菜单：考试中心 -> 我的成绩 -> 错题本 -> 错题重练。')
  localStorage.setItem(key, '1')
})
</script>

<style scoped>
.student-container { height: 100vh; display: flex; flex-direction: column; overflow: hidden; font-family: "PingFang SC", "Microsoft YaHei", sans-serif; }

/* Header 样式 - 参考你提供的配色 #4D58B5 */
.header { height: 60px; background: #4D58B5; color: #fff; display: flex; justify-content: space-between; align-items: center; padding: 0 30px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
.logo-area h3 { font-size: 20px; font-weight: bold; letter-spacing: 1px; }
.user-info { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.avatar { width: 32px; height: 32px; border-radius: 50%; border: 2px solid rgba(255,255,255,0.3); }
.username { font-size: 15px; }
.el-dropdown-link { color: #fff; display: flex; align-items: center; margin-left: 5px; }

.main-body { flex: 1; display: flex; overflow: hidden; }

/* Sidebar 样式 */
.sidebar { width: 220px; background: #4D58B5; display: flex; flex-direction: column; border-top: 1px solid rgba(255,255,255,0.1); }
.user-block { padding: 30px 0; text-align: center; }
.head-img { width: 70px; height: 70px; border-radius: 50%; border: 3px solid rgba(255,255,255,0.2); margin-bottom: 12px; }
.user-name-label { color: #fff; font-size: 16px; font-weight: 500; }

.menu-list { flex: 1; }
.menu-list ul { list-style: none; padding: 0; margin: 0; }
.menu-list li { height: 50px; display: flex; align-items: center; padding-left: 30px; color: #fff; cursor: pointer; transition: all 0.3s; opacity: 0.8; }
.menu-list li:hover { background: #2439A9; opacity: 1; }
.menu-list li.active { background: #5965C9; opacity: 1; position: relative; }
.menu-list li.active::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 4px; background: #fff; }
.menu-icon { font-size: 18px; margin-right: 12px; }
.menu-text { font-size: 15px; }

/* 内容区样式 */
.content { flex: 1; background: #f0f2f5; padding: 20px; overflow-y: auto; }
</style>
