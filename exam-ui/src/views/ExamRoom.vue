<template>
  <div class="exam-room-container" v-loading.fullscreen.lock="loading">
    <header class="exam-header">
      <div class="exam-title">{{ examData.title || '正在加载试卷...' }}</div>
      <div class="timer-box">
        <el-icon class="is-loading" v-if="timeLeft > 0"><Timer /></el-icon>
        <span :class="{'time-warning': timeLeft < 300}">倒计时：{{ formatCountdown(timeLeft) }}</span>
      </div>
      <div class="save-status">草稿：{{ lastSavedAtLabel }}</div>
      <div class="actions">
        <el-button type="danger" round @click="handleManualSubmit" size="large">交卷</el-button>
      </div>
    </header>

    <div class="exam-main">
      <aside class="exam-sidebar">
        <div class="sidebar-title">答题卡</div>
        <div class="q-grid">
          <div
            v-for="(q, index) in examData.questions"
            :key="q.id"
            class="q-block"
            :class="{ 'is-answered': isAnswered(q) }"
            @click="scrollToQuestion(index)"
          >
            {{ index + 1 }}
          </div>
        </div>
        <div class="sidebar-legend">
          <span class="legend-item"><div class="block is-answered"></div>已答</span>
          <span class="legend-item"><div class="block"></div>未答</span>
        </div>
      </aside>

      <main class="exam-content" id="examContent">
        <div class="questions-wrapper">
          <el-card
            v-for="(q, index) in examData.questions"
            :key="q.id"
            class="question-card"
            :id="`question-${index}`"
            shadow="hover"
          >
            <div class="q-header">
              <el-tag effect="dark" size="small">{{ formatType(q.type) }}</el-tag>
              <span class="q-num">第 {{ index + 1 }} 题</span>
            </div>

            <div class="q-stem" v-html="q.stem"></div>

            <div v-if="q.type === 'SINGLE_CHOICE' || q.type === 'TRUE_FALSE'" class="q-options">
              <el-radio-group v-model="answers[q.id]" class="custom-radio-group">
                <el-radio
                  v-for="(opt, idx) in q.options"
                  :key="idx"
                  :value="getLetter(idx)"
                  border
                >
                  <span class="opt-label">{{ getLetter(idx) }}.</span> {{ opt }}
                </el-radio>
              </el-radio-group>
            </div>

            <div v-else-if="q.type === 'MULTIPLE_CHOICE'" class="q-options">
              <el-checkbox-group v-model="answers[q.id]" class="custom-check-group">
                <el-checkbox
                  v-for="(opt, idx) in q.options"
                  :key="idx"
                  :label="getLetter(idx)"
                  border
                >
                  <span class="opt-label">{{ getLetter(idx) }}.</span> {{ opt }}
                </el-checkbox>
              </el-checkbox-group>
            </div>

            <div v-else-if="q.type === 'SHORT_ANSWER'" class="q-answer-area">
              <el-input
                v-model="answers[q.id]"
                type="textarea"
                :rows="5"
                placeholder="请输入您的作答内容..."
              />
            </div>
          </el-card>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Timer } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const examData = ref({ title: '', endTime: '', questions: [] })
const answers = ref({})
const loading = ref(false)
const savingDraft = ref(false)

const timeLeft = ref(3600)
let timer = null
let autoSaveTimer = null
let heartbeatTimer = null

const antiCheatRule = ref({
  maxPageBlurCount: 3
})
const cheatCount = ref(0)
const forceSubmitted = ref(false)
const hiddenAtMs = ref(0)
const eventAtMap = ref({})
const lastSavedAt = ref('')
const lastSavedAtLabel = ref('未保存')

const formatType = (t) => ({ 'SINGLE_CHOICE': '单选题', 'MULTIPLE_CHOICE': '多选题', 'TRUE_FALSE': '判断题', 'SHORT_ANSWER': '简答题' }[t] || t)
const getLetter = (idx) => String.fromCharCode(65 + idx)
const isAnswered = (q) => {
  const val = answers.value[q.id]
  if (q.type === 'MULTIPLE_CHOICE') {
    return Array.isArray(val) && val.length > 0
  }
  return !!String(val || '').trim()
}

const formatCountdown = (seconds) => {
  const safe = Math.max(0, Number(seconds || 0))
  const h = Math.floor(safe / 3600).toString().padStart(2, '0')
  const m = Math.floor((safe % 3600) / 60).toString().padStart(2, '0')
  const s = (safe % 60).toString().padStart(2, '0')
  return `${h}:${m}:${s}`
}

const refreshCountdown = () => {
  if (!examData.value.endTime) {
    timeLeft.value = 3600
    return
  }
  const end = new Date(examData.value.endTime).getTime()
  const now = Date.now()
  timeLeft.value = Math.max(0, Math.floor((end - now) / 1000))
}

const fetchAntiCheatRule = async () => {
  try {
    antiCheatRule.value = await request.get(`/student/exams/${route.params.id}/anti-cheat-rule`)
  } catch (_) {
    antiCheatRule.value = { maxPageBlurCount: 3 }
  }
}

const fetchExamData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/student/exams/detail/${route.params.id}`)
    examData.value = res
    res.questions.forEach(q => {
      answers.value[q.id] = q.type === 'MULTIPLE_CHOICE' ? [] : ''
    })
    refreshCountdown()
  } catch (error) {
    ElMessage.error('无法加载试卷')
  } finally {
    loading.value = false
  }
}

const applyLoadedAnswers = (loaded) => {
  for (const q of examData.value.questions) {
    const key = String(q.id)
    if (!(key in loaded)) continue
    const val = loaded[key]
    if (q.type === 'MULTIPLE_CHOICE') {
      answers.value[q.id] = String(val || '')
        .split(',')
        .map(x => x.trim())
        .filter(Boolean)
    } else {
      answers.value[q.id] = String(val || '')
    }
  }
}

const loadProgress = async () => {
  try {
    const res = await request.get(`/student/exams/${route.params.id}/progress/load`)
    const loaded = res?.answers || {}
    if (Object.keys(loaded).length > 0) {
      applyLoadedAnswers(loaded)
      if (res.savedAt) {
        lastSavedAt.value = res.savedAt
        lastSavedAtLabel.value = formatDateTime(res.savedAt)
      } else {
        lastSavedAtLabel.value = '已恢复'
      }
      ElMessage.success('已恢复上次作答进度')
    }
  } catch (_) {
    // 恢复失败不影响考试
  }
}

const buildNormalizedAnswers = () => {
  const normalizedAnswers = {}
  for (const q of examData.value.questions) {
    const raw = answers.value[q.id]
    if (q.type === 'MULTIPLE_CHOICE') {
      normalizedAnswers[q.id] = Array.isArray(raw)
        ? [...new Set(raw)].sort().join(',')
        : ''
    } else {
      normalizedAnswers[q.id] = String(raw || '').trim()
    }
  }
  return normalizedAnswers
}

const saveProgress = async (silent = true) => {
  if (!examData.value?.id || forceSubmitted.value || loading.value || savingDraft.value) return
  savingDraft.value = true
  try {
    const res = await request.post(`/student/exams/${route.params.id}/progress/save`, {
      answers: buildNormalizedAnswers()
    })
    if (res?.savedAt) {
      lastSavedAt.value = res.savedAt
      lastSavedAtLabel.value = formatDateTime(res.savedAt)
    } else {
      lastSavedAtLabel.value = formatDateTime(new Date().toISOString())
    }
    if (!silent) {
      ElMessage.success('草稿已保存')
    }
  } catch (_) {
    if (!silent) {
      ElMessage.warning('草稿保存失败')
    }
  } finally {
    savingDraft.value = false
  }
}

const sendHeartbeat = async () => {
  if (!examData.value?.id || forceSubmitted.value) return
  try {
    const totalCount = examData.value.questions?.length || 0
    const answeredCount = examData.value.questions.filter(q => isAnswered(q)).length
    await request.post(`/student/exams/${route.params.id}/heartbeat`, {
      answeredCount,
      totalCount,
      timeLeftSeconds: timeLeft.value
    })
  } catch (_) {
    // 心跳失败不打断考试
  }
}

const scrollToQuestion = (index) => {
  const el = document.getElementById(`question-${index}`)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const submitExam = async (isAuto = false) => {
  if (loading.value) return
  loading.value = true
  try {
    await saveProgress(true)
    await request.post(`/student/exams/${route.params.id}/submit`, {
      answers: buildNormalizedAnswers()
    })
    ElMessage.success(isAuto ? '时间到，已自动为您交卷！' : '试卷提交成功！')
    router.replace('/student/results')
  } catch (e) {
    ElMessage.error('交卷失败，请重试！')
    loading.value = false
  }
}

const handleManualSubmit = () => {
  const unAnswered = examData.value.questions.filter(q => !isAnswered(q)).length
  const msg = unAnswered > 0
    ? `您还有 <strong style="color:red">${unAnswered}</strong> 道题未作答，确定要提前交卷吗？`
    : '您已完成所有题目，确定要交卷吗？'

  ElMessageBox.confirm(msg, '交卷确认', {
    dangerouslyUseHTMLString: true,
    confirmButtonText: '确定交卷',
    cancelButtonText: '继续答题',
    type: 'warning'
  }).then(() => {
    submitExam(false)
  }).catch(() => {})
}

const reportCheat = async (type, seconds, detail) => {
  const now = Date.now()
  const lastAt = eventAtMap.value[type] || 0
  if (now - lastAt < 1500) return
  eventAtMap.value[type] = now

  try {
    await request.post(`/student/exams/${route.params.id}/cheat-events`, {
      type,
      durationSeconds: Math.max(1, Number(seconds || 1)),
      detail
    })
  } catch (_) {
    // 风险上报失败不打断考试
  }
}

const forceSubmitIfExceeded = () => {
  if (forceSubmitted.value) return
  const maxCheat = Number(antiCheatRule.value?.maxPageBlurCount || 3)
  if (cheatCount.value >= maxCheat) {
    forceSubmitted.value = true
    ElMessageBox.alert('系统检测到多次离开考试页面，已触发强制交卷。', '违规警告', {
      confirmButtonText: '确定',
      callback: () => submitExam(true)
    })
  } else {
    ElMessage.warning(`请勿离开考试页面，剩余容忍次数：${Math.max(maxCheat - cheatCount.value, 0)} 次`)
  }
}

const handleVisibilityChange = () => {
  if (document.hidden) {
    saveProgress(true)
    hiddenAtMs.value = Date.now()
    cheatCount.value++
    forceSubmitIfExceeded()
    return
  }
  if (hiddenAtMs.value > 0) {
    const duration = Math.floor((Date.now() - hiddenAtMs.value) / 1000)
    hiddenAtMs.value = 0
    reportCheat('PAGE_BLUR', duration, 'visibilitychange')
  }
}

const handleWindowBlur = () => {
  if (!document.hidden) {
    reportCheat('WINDOW_SWITCH', 1, 'window blur')
  }
}

const handleCopyLikeEvent = (event) => {
  event.preventDefault()
  reportCheat('COPY_PASTE', 1, event.type)
  ElMessage.warning('考试中已禁用复制/粘贴/右键')
}

const handleOffline = () => {
  reportCheat('NETWORK_DISCONNECT', 3, 'browser offline')
}

const handleFullscreenChange = () => {
  if (!document.fullscreenElement) {
    reportCheat('FULLSCREEN_EXIT', 1, 'fullscreen exited')
  }
}

const tryEnterFullscreen = async () => {
  try {
    if (!document.fullscreenElement && document.documentElement.requestFullscreen) {
      await document.documentElement.requestFullscreen()
    }
  } catch (_) {
    // 浏览器策略可能限制自动进入全屏
  }
}

const formatDateTime = (t) => {
  if (!t) return '未保存'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t)
  const hh = d.getHours().toString().padStart(2, '0')
  const mm = d.getMinutes().toString().padStart(2, '0')
  const ss = d.getSeconds().toString().padStart(2, '0')
  return `${hh}:${mm}:${ss}`
}

onMounted(async () => {
  await fetchExamData()
  await fetchAntiCheatRule()
  await loadProgress()
  tryEnterFullscreen()

  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('blur', handleWindowBlur)
  window.addEventListener('offline', handleOffline)
  document.addEventListener('copy', handleCopyLikeEvent)
  document.addEventListener('paste', handleCopyLikeEvent)
  document.addEventListener('contextmenu', handleCopyLikeEvent)
  document.addEventListener('fullscreenchange', handleFullscreenChange)

  timer = setInterval(() => {
    refreshCountdown()
    if (timeLeft.value <= 0) {
      clearInterval(timer)
      submitExam(true)
    }
  }, 1000)
  autoSaveTimer = setInterval(() => {
    saveProgress(true)
  }, 20000)
  heartbeatTimer = setInterval(() => {
    sendHeartbeat()
  }, 30000)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('blur', handleWindowBlur)
  window.removeEventListener('offline', handleOffline)
  document.removeEventListener('copy', handleCopyLikeEvent)
  document.removeEventListener('paste', handleCopyLikeEvent)
  document.removeEventListener('contextmenu', handleCopyLikeEvent)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  if (timer) clearInterval(timer)
  if (autoSaveTimer) clearInterval(autoSaveTimer)
  if (heartbeatTimer) clearInterval(heartbeatTimer)
})
</script>

<style scoped>
.exam-room-container { height: 100vh; display: flex; flex-direction: column; background: #f0f2f5; font-family: "PingFang SC", sans-serif; }

.exam-header {
  height: 64px; background: #fff; padding: 0 30px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08); z-index: 10;
}
.exam-title { font-size: 20px; font-weight: bold; color: #4D58B5; }
.timer-box { font-size: 22px; font-weight: bold; color: #303133; display: flex; align-items: center; gap: 10px; }
.save-status { font-size: 13px; color: #909399; }
.time-warning { color: #f56c6c; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.5; } }

.exam-main { flex: 1; display: flex; overflow: hidden; }

.exam-sidebar { width: 260px; background: #fff; border-right: 1px solid #ebeef5; padding: 20px; display: flex; flex-direction: column; }
.sidebar-title { font-size: 16px; font-weight: bold; border-bottom: 1px solid #eee; padding-bottom: 10px; margin-bottom: 20px; }
.q-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; flex: 1; align-content: flex-start; }
.q-block {
  aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
  border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; color: #606266; transition: 0.2s;
}
.q-block:hover { border-color: #4D58B5; color: #4D58B5; }
.q-block.is-answered { background: #4D58B5; color: #fff; border-color: #4D58B5; }
.sidebar-legend { margin-top: auto; display: flex; justify-content: space-around; padding-top: 20px; border-top: 1px solid #eee; }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 12px; color: #909399; }
.legend-item .block { width: 12px; height: 12px; border: 1px solid #dcdfe6; border-radius: 2px; }
.legend-item .block.is-answered { background: #4D58B5; border-color: #4D58B5; }

.exam-content { flex: 1; padding: 30px; overflow-y: auto; scroll-behavior: smooth; }
.questions-wrapper { max-width: 850px; margin: 0 auto; }
.question-card { margin-bottom: 20px; border-radius: 8px; border: none; }
.q-header { display: flex; align-items: center; gap: 12px; margin-bottom: 15px; }
.q-num { font-weight: bold; color: #303133; }
.q-stem { font-size: 16px; line-height: 1.8; color: #333; margin-bottom: 20px; }

.custom-radio-group { display: flex; flex-direction: column; gap: 15px; width: 100%; align-items: flex-start; }
.custom-radio-group .el-radio {
  margin: 0; width: 100%; border-radius: 8px; padding: 0 20px; height: auto; min-height: 50px;
  display: flex; align-items: center; white-space: normal; line-height: 1.5;
}
.custom-radio-group .el-radio.is-checked { background: #f0f4ff; border-color: #4D58B5; }
.custom-check-group { display: flex; flex-direction: column; gap: 15px; width: 100%; align-items: flex-start; }
.custom-check-group .el-checkbox {
  margin: 0;
  width: 100%;
  border-radius: 8px;
  padding: 0 20px;
  min-height: 50px;
  height: auto;
  display: flex;
  align-items: center;
  white-space: normal;
  line-height: 1.5;
}
.custom-check-group .el-checkbox.is-checked { background: #f0f4ff; border-color: #4D58B5; }
.opt-label { font-weight: bold; margin-right: 5px; font-size: 16px; }

.q-answer-area { margin-top: 10px; }
</style>
