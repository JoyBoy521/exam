export const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

export const examRecordStatusMeta = (status) => {
  if (status === 'GRADED') return { text: '已出分', tag: 'success' }
  if (status === 'MARKING') return { text: '阅卷中', tag: 'warning' }
  return { text: status || '-', tag: 'info' }
}

export const makeupStatusMeta = (status) => {
  if (status === 'PENDING') return { text: '待审核', tag: 'warning' }
  if (status === 'APPROVED') return { text: '已批准', tag: 'success' }
  if (status === 'REJECTED') return { text: '已驳回', tag: 'danger' }
  return { text: status || '-', tag: 'info' }
}

export const riskLevelMeta = (level) => {
  if (level === 'HIGH') return { text: '高风险', tag: 'danger' }
  if (level === 'MEDIUM') return { text: '中风险', tag: 'warning' }
  if (level === 'LOW') return { text: '低风险', tag: 'success' }
  return { text: level || '-', tag: 'info' }
}

export const masteryMeta = (level) => {
  if (level === 'MASTERED') return { text: '已掌握', tag: 'success' }
  if (level === 'IMPROVING') return { text: '提升中', tag: 'warning' }
  if (level === 'NEEDS_REVIEW') return { text: '需巩固', tag: 'danger' }
  if (level === 'SUBJECTIVE') return { text: '主观题', tag: '' }
  return { text: '未练习', tag: 'info' }
}
