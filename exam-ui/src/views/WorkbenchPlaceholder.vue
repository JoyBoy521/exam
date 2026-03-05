<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span style="font-weight:600;">{{ title }}</span>
        <el-tag type="info">建设中</el-tag>
      </div>
    </template>
    <el-empty :description="description" />
  </el-card>
</template>

<script setup>
defineProps({
  title: { type: String, default: '功能页' },
  description: { type: String, default: '该模块正在建设中' }
})
</script>
