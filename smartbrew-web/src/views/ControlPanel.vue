<template>
  <div>
    <!-- 设备选择器 -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <el-form :inline="true">
        <el-form-item label="选择设备">
          <el-select v-model="selectedDeviceId" placeholder="请选择设备" @change="onDeviceChange" style="width: 300px">
            <el-option
              v-for="d in deviceList"
              :key="d.deviceId"
              :label="d.deviceName + ' (' + d.deviceId + ')'"
              :value="d.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-tag v-if="selectedDevice" :type="selectedDevice.status === 1 ? 'success' : 'danger'">
            {{ selectedDevice.status === 1 ? '在线' : '离线' }}
          </el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 控制面板 -->
    <el-row :gutter="20" v-if="selectedDeviceId">
      <!-- 风扇控制 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-size: 16px; font-weight: bold">
                <el-icon :size="20" style="margin-right: 6px; vertical-align: middle">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/>
                  </svg>
                </el-icon>
                风扇控制
              </span>
              <el-tag :type="fanOn ? 'success' : 'info'" size="small">
                {{ fanOn ? '运行中' : '已关闭' }}
              </el-tag>
            </div>
          </template>
          <div style="text-align: center; padding: 30px 0">
            <div style="font-size: 14px; color: #909399; margin-bottom: 20px">
              温度 &gt; 25℃ 时自动开启风扇降温
            </div>
            <el-switch
              v-model="fanOn"
              :loading="fanLoading"
              :disabled="!isOnline || fanLoading"
              active-text="开启"
              inactive-text="关闭"
              size="large"
              inline-prompt
              style="--el-switch-on-color: #67c23a; --el-switch-off-color: #909399"
              @change="(val) => handleControl('FAN', val ? 'ON' : 'OFF')"
            />
            <div style="margin-top: 16px; color: #909399; font-size: 13px">
              当前状态：<strong :style="{ color: fanOn ? '#67c23a' : '#909399' }">{{ fanStatusText }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 加热控制 -->
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-size: 16px; font-weight: bold">
                <el-icon :size="20" style="margin-right: 6px; vertical-align: middle">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M13.49 5.48c1.1 1.1 1.25 2.51.5 3.5-.75.99-2.25 1.25-3.35.75s-1.75-1.75-1.75-3c0-1.5 1-2.5 2.5-3.5.4-.27.8-.45 1.1-.37.3.08.45.32.45.62 0 .5-.4 1.2-.9 1.7.5-.2 1-.2 1.45.3zm2.4 8.52c-.3-1.5-1.5-3.5-3.5-5.5-2.23 2.27-3.1 4.4-3.1 6 0 3 2.5 5.5 5.5 5.5s5.5-2.5 5.5-5.5c0-1.1-.7-2.5-1.9-3.9-.7.7-1.7 1.7-2.5 3.4z"/>
                  </svg>
                </el-icon>
                加热控制
              </span>
              <el-tag :type="heaterOn ? 'danger' : 'info'" size="small">
                {{ heaterOn ? '加热中' : '已关闭' }}
              </el-tag>
            </div>
          </template>
          <div style="text-align: center; padding: 30px 0">
            <div style="font-size: 14px; color: #909399; margin-bottom: 20px">
              温度 &lt; 18℃ 时自动开启加热升温
            </div>
            <el-switch
              v-model="heaterOn"
              :loading="heaterLoading"
              :disabled="!isOnline || heaterLoading"
              active-text="开启"
              inactive-text="关闭"
              size="large"
              inline-prompt
              style="--el-switch-on-color: #f56c6c; --el-switch-off-color: #909399"
              @change="(val) => handleControl('HEATER', val ? 'ON' : 'OFF')"
            />
            <div style="margin-top: 16px; color: #909399; font-size: 13px">
              当前状态：<strong :style="{ color: heaterOn ? '#f56c6c' : '#909399' }">{{ heaterStatusText }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作日志 -->
    <el-card shadow="never" style="margin-top: 20px" v-if="selectedDeviceId">
      <template #header>
        <span style="font-weight: bold">最近操作记录</span>
      </template>
      <div style="color: #909399; font-size: 13px; text-align: center; padding: 20px 0" v-if="controlLog.length === 0">
        暂无操作记录
      </div>
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in controlLog"
          :key="item.id"
          :timestamp="formatTime(item.createTime)"
          placement="top"
          :color="item.command === 'ON' ? '#67c23a' : '#f56c6c'"
        >
          <span>{{ item.triggerReason || (item.controlTarget + ' ' + item.command) }}</span>
          <el-tag size="small" :type="statusTag(item.executeStatus)" style="margin-left: 8px">
            {{ statusText(item.executeStatus) }}
          </el-tag>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 未选择设备 -->
    <el-empty v-if="!selectedDeviceId" description="请选择一个在线设备进行控制" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDeviceList, sendControl } from '../api'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

const deviceList = ref([])
const selectedDeviceId = ref('')
const selectedDevice = ref(null)
const fanOn = ref(false)
const heaterOn = ref(false)
const fanLoading = ref(false)
const heaterLoading = ref(false)
const controlLog = ref([])

const isOnline = computed(() => selectedDevice.value?.status === 1)
const fanStatusText = computed(() => fanLoading.value ? '指令下发中...' : (fanOn.value ? '风扇运行中' : '风扇已关闭'))
const heaterStatusText = computed(() => heaterLoading.value ? '指令下发中...' : (heaterOn.value ? '加热模块运行中' : '加热模块已关闭'))

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

function statusTag(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'warning', 3: 'danger' }
  return map[status] || 'info'
}

function statusText(status) {
  const map = { 0: '已下发', 1: '执行成功', 2: '执行超时', 3: '执行失败' }
  return map[status] || '未知'
}

async function loadDeviceList() {
  try {
    const page = await getDeviceList({ page: 1, size: 100 })
    deviceList.value = page.records || []
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
}

function onDeviceChange() {
  selectedDevice.value = deviceList.value.find(d => d.deviceId === selectedDeviceId.value)
  fanOn.value = false
  heaterOn.value = false
}

async function handleControl(target, command) {
  const targetLabel = target === 'FAN' ? '风扇' : '加热'
  const commandLabel = command === 'ON' ? '开启' : '关闭'
  const loadingRef = target === 'FAN' ? fanLoading : heaterLoading
  const stateRef = target === 'FAN' ? fanOn : heaterOn

  loadingRef.value = true
  try {
    await sendControl(selectedDeviceId.value, { target, command })
    ElMessage.success(`${targetLabel}${commandLabel}指令已下发`)
    // 在日志中追加记录
    controlLog.value.unshift({
      id: Date.now(),
      controlTarget: target,
      command: command,
      triggerReason: `手动${commandLabel}${targetLabel}`,
      executeStatus: 0,
      createTime: new Date().toISOString()
    })
    if (controlLog.value.length > 20) controlLog.value.pop()
  } catch (e) {
    ElMessage.error(`${targetLabel}${commandLabel}失败: ${e.message || '未知错误'}`)
    // 恢复开关状态
    stateRef.value = !stateRef.value
  } finally {
    loadingRef.value = false
  }
}

onMounted(() => {
  loadDeviceList()
})
</script>
