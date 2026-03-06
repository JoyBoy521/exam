import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import StudentLayout from '../layout/StudentLayout.vue'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },

  {
    path: '/',
    component: Layout,
    redirect: '/exam',
    children: [
      { path: 'chapter', component: () => import('../views/WorkbenchPlaceholder.vue'), props: { title: '章节', description: '章节体系与教学计划模块建设中' } },
      { path: 'data', component: () => import('../views/WorkbenchPlaceholder.vue'), props: { title: '资料', description: '课程资料上传与分发模块建设中' } },
      { path: 'notice', component: () => import('../views/WorkbenchPlaceholder.vue'), props: { title: '通知', description: '课程通知发布模块建设中' } },
      { path: 'discuss', component: () => import('../views/WorkbenchPlaceholder.vue'), props: { title: '讨论', description: '课程讨论区模块建设中' } },
      { path: 'homework', component: () => import('../views/WorkbenchPlaceholder.vue'), props: { title: '作业', description: '作业布置与批改模块建设中' } },
      { path: 'question', component: () => import('../views/Question.vue') },
      { path: 'question/edit', component: () => import('../views/QuestionEdit.vue') },
      { path: 'paper', component: () => import('../views/Paper.vue') },
      { path: 'paper/edit', component: () => import('../views/PaperEdit.vue') },
      { path: 'exam', component: () => import('../views/Exam.vue') },
      { path: 'monitor', component: () => import('../views/Monitor.vue') },
      { path: 'marking/:examId', component: () => import('../views/Marking.vue') },
      { path: 'class', component: () => import('../views/ClassManage.vue') },
      { path: 'stat', component: () => import('../views/Stat.vue') },
      { path: 'manage', component: () => import('../views/Manage.vue') }
    ]
  },

  {
    path: '/student',
    component: StudentLayout,
    redirect: '/student/exam',
    children: [
      { path: 'exam', component: () => import('../views/StudentExam.vue') },
      { path: 'results', component: () => import('../views/StudentResults.vue') },
      { path: 'result-detail/:recordId', component: () => import('../views/StudentResultDetail.vue') },
      { path: 'wrong-books', component: () => import('../views/WrongBooks.vue') },
      { path: 'notices', component: () => import('../views/StudentNotices.vue') }
    ]
  },

  { path: '/exam-room/:id', component: () => import('../views/ExamRoom.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.path === '/login') return true
  if (!token) return '/login'

  if (role === 'STUDENT') {
    if (to.path.startsWith('/student') || to.path.startsWith('/exam-room')) {
      return true
    }
    return '/student/exam'
  }

  if (to.path.startsWith('/student') || to.path.startsWith('/exam-room')) {
    ElMessage.error('教师账号请使用管理端')
    return '/exam'
  }
  return true
})

export default router
