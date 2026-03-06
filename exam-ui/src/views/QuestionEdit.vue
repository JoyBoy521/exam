<template>
  <div class="edit-layout">
    <div class="dark-header">
      <div class="header-left" @click="$router.back()">
        <el-icon><Back /></el-icon> 返回题库
      </div>
      <div class="header-center">{{ isEdit ? '编辑题目' : '创建新题目' }}</div>
      <div class="header-right">
        <el-button plain @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSave">
          <el-icon style="margin-right: 4px"><Check /></el-icon> 保存题目
        </el-button>
      </div>
    </div>

    <div class="main-canvas" v-loading="loading">
      <div class="paper-card">
        
        <div class="type-selector">
          <span class="label">题型</span>
          <el-radio-group v-model="form.type" size="large" @change="handleTypeChange">
            <el-radio-button label="SINGLE_CHOICE">单选题</el-radio-button>
            <el-radio-button label="MULTIPLE_CHOICE">多选题</el-radio-button>
            <el-radio-button label="TRUE_FALSE">判断题</el-radio-button>
            <el-radio-button label="SHORT_ANSWER">简答题</el-radio-button>
          </el-radio-group>
        </div>

        <el-divider border-style="dashed" />

        <div class="question-body">
          <div class="title-label">题干内容</div>
          <el-input 
            v-model="form.stem" 
            type="textarea" 
            :rows="5" 
            placeholder="请输入题干内容..." 
            class="stem-input"
          />

          <div v-if="form.type === 'SINGLE_CHOICE' || form.type === 'MULTIPLE_CHOICE'" class="options-area">
            <div class="title-label" style="margin-top: 24px;">设置选项与正确答案</div>
            <div 
              class="option-item" 
              v-for="(opt, index) in form.options" 
              :key="index"
            >
              <el-radio v-if="form.type === 'SINGLE_CHOICE'" v-model="form.answer" :label="getLetter(index)" size="large">
                <span class="option-letter" :class="{ 'is-correct': form.answer === getLetter(index) }">
                  {{ getLetter(index) }}
                </span>
              </el-radio>
              <el-checkbox v-else v-model="form.answer" :label="getLetter(index)" size="large">
                <span class="option-letter" :class="{ 'is-correct': form.answer.includes(getLetter(index)) }">
                  {{ getLetter(index) }}
                </span>
              </el-checkbox>
              
              <el-input v-model="form.options[index]" placeholder="请输入选项内容" style="flex: 1; margin: 0 16px;" />
              
              <el-button type="danger" link @click="removeOption(index)" :disabled="form.options.length <= 2">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            
            <el-button link type="primary" @click="addOption" style="margin-top: 10px;">
              <el-icon><Plus /></el-icon> 添加一个选项
            </el-button>
          </div>

          <div v-if="form.type === 'TRUE_FALSE'" class="options-area">
            <div class="title-label" style="margin-top: 24px;">设置正确答案</div>
            <el-radio-group v-model="form.answer" size="large">
              <el-radio-button label="正确">正确 (True)</el-radio-button>
              <el-radio-button label="错误">错误 (False)</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="form.type === 'SHORT_ANSWER'" class="options-area">
            <div class="title-label" style="margin-top: 24px;">设置参考答案/评分要点</div>
            <el-input 
              v-model="form.answer" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入标准参考答案..." 
            />
          </div>

          <el-divider border-style="dashed" />

          <div class="settings-area">
            <div class="title-label">附加属性</div>
            <div class="setting-row">
              <span class="s-label">知识点</span>
              <el-select 
                v-model="form.knowledgePoints" 
                multiple 
                filterable 
                allow-create 
                default-first-option
                placeholder="输入知识点后按回车可创建" 
                style="width: 100%; max-width: 400px;"
              >
                <el-option label="Java基础" value="Java基础" />
                <el-option label="计算机网络" value="计算机网络" />
                <el-option label="Spring Boot" value="Spring Boot" />
              </el-select>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Back, Plus, Delete, Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const isEdit = ref(false) // 未来如果做编辑功能可以用到
const loading = ref(false)

// 响应式的表单数据
const form = ref({
  type: 'SINGLE_CHOICE',
  stem: '',
  options: ['', '', '', ''], // 默认 ABCD 4个空选项
  answer: '', // 最终的正确答案 (比如 'A'，或者判断题的 '正确')
  knowledgePoints: [] // 后端限制了 @NotEmpty，所以必须填
})

// 工具函数：根据索引 0,1,2 生成字母 A,B,C
const getLetter = (index) => String.fromCharCode(65 + index)

// 切换题型时，清空之前填写的答案，防止数据串线
const handleTypeChange = () => {
  form.value.answer = form.value.type === 'MULTIPLE_CHOICE' ? [] : ''
}

// 增加选项
const addOption = () => {
  form.value.options.push('')
}

// 删除选项
const removeOption = (index) => {
  const removedLetter = getLetter(index)
  form.value.options.splice(index, 1)
  if (form.value.type === 'MULTIPLE_CHOICE') {
    const selected = Array.isArray(form.value.answer) ? form.value.answer : []
    form.value.answer = selected
      .filter(letter => letter !== removedLetter)
      .map(letter => {
        const oldCode = letter.charCodeAt(0)
        const removedCode = removedLetter.charCodeAt(0)
        return oldCode > removedCode ? String.fromCharCode(oldCode - 1) : letter
      })
  } else if (form.value.answer === removedLetter) {
    form.value.answer = ''
  }
}

// 【核心】提交保存到后端的逻辑
const handleSave = async () => {
  // 1. 严格的前端校验
  if (!form.value.stem.trim()) return ElMessage.warning('题干内容不能为空！')
  if (form.value.type === 'MULTIPLE_CHOICE') {
    if (!Array.isArray(form.value.answer) || form.value.answer.length === 0) {
      return ElMessage.warning('请至少选择一个正确答案！')
    }
  } else if (!form.value.answer) {
    return ElMessage.warning('请务必设置/选择一个正确答案！')
  }
  if (form.value.knowledgePoints.length === 0) return ElMessage.warning('请至少关联一个知识点！')

  // 2. 组装发给后端的 payload (严格对齐你的 CreateQuestionRequest DTO)
  const normalizedAnswer = form.value.type === 'MULTIPLE_CHOICE'
    ? [...new Set(form.value.answer)].sort().join(',')
    : form.value.answer
  const payload = {
    type: form.value.type,
    stem: form.value.stem,
    knowledgePoints: form.value.knowledgePoints,
    answer: normalizedAnswer,
    options: []
  }

  // 选择题需要额外校验并组装 options
  if (form.value.type === 'SINGLE_CHOICE' || form.value.type === 'MULTIPLE_CHOICE') {
    // 过滤掉完全没写字的空选项
    payload.options = form.value.options.filter(opt => opt.trim() !== '')
    if (payload.options.length < 2) return ElMessage.warning('选择题至少需要 2 个有效选项！')
  }

  // 3. 发送真实请求
  loading.value = true
  try {
    await request.post('/teacher/questions', payload)
    ElMessage.success('题目保存成功！')
    
    // 保存成功后，自动返回题库列表页
    router.push('/question')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.edit-layout {
  min-height: 100vh;
  background-color: #f0f2f5;
}
.dark-header {
  height: 56px;
  background-color: #38465e;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  color: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-left {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}
.header-left:hover { color: #409eff; }
.header-center {
  font-size: 16px;
  letter-spacing: 1px;
}
.main-canvas {
  padding: 30px;
  display: flex;
  justify-content: center;
}
.paper-card {
  width: 900px;
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}
.type-selector {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
.type-selector .label {
  color: #909399;
  margin-right: 20px;
  font-size: 14px;
}
.title-label {
  font-weight: bold;
  margin-bottom: 16px;
  color: #303133;
  font-size: 15px;
}
.stem-input :deep(.el-textarea__inner) {
  font-size: 15px;
  padding: 12px;
  line-height: 1.6;
}
.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  background: #fafafa;
  padding: 12px 16px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
}
.option-item:hover {
  border-color: #c6e2ff;
  box-shadow: 0 2px 8px rgba(64,158,255,0.1);
}
.option-letter {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  font-weight: bold;
  color: #606266;
}
.option-letter.is-correct {
  background-color: #67c23a;
  border-color: #67c23a;
  color: #fff;
}
.settings-area {
  margin-top: 40px;
  background: #f8f9fa;
  padding: 20px;
  border-radius: 6px;
}
.setting-row {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.s-label {
  width: 80px;
  color: #606266;
  font-size: 14px;
}
</style>
