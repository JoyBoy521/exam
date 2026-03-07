export const downloadBlob = (blob, filename) => {
  const safeName = filename || 'download.dat'
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = safeName
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

const escapeCsv = (value) => {
  const text = String(value ?? '')
  if (text.includes(',') || text.includes('"') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

export const exportCsvRows = (headers, rows, filename) => {
  const lines = [headers.map(escapeCsv).join(',')]
  for (const row of rows) {
    lines.push(row.map(escapeCsv).join(','))
  }
  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  downloadBlob(blob, filename || 'export.csv')
}
