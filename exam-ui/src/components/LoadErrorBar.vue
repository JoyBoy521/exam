<template>
  <el-alert
    v-if="message"
    type="error"
    :closable="false"
    show-icon
    style="margin-bottom: 12px;"
  >
    <template #title>
      <span>{{ message }}</span>
      <el-button size="small" text type="primary" style="margin-left: 8px;" @click="$emit('retry')">重试</el-button>
    </template>
  </el-alert>
</template>

<script setup>
defineProps({
  message: { type: String, default: '' }
})
defineEmits(['retry'])
</script>
