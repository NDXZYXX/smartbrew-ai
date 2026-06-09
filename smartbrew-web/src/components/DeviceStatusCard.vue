<template>
  <el-card shadow="hover">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>设备信息</span>
        <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
      </div>
    </template>
    <el-descriptions :column="1" size="small" border>
      <el-descriptions-item label="设备名称">
        {{ device.deviceName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="设备 ID">
        {{ device.deviceId || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="IP 地址">
        {{ device.ipAddress || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="固件版本">
        {{ device.firmwareVersion || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="最近心跳">
        {{ device.lastHeartbeatTime || '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  device: { type: Object, default: () => ({}) }
})

const statusTagType = computed(() => (props.device.status === 1 ? 'success' : 'danger'))
const statusText = computed(() => (props.device.status === 1 ? '在线' : '离线'))
</script>
