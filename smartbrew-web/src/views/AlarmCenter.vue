<template>
  <div>
    <!-- 筛选栏 -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <el-form :inline="true">
        <el-form-item label="选择设备">
          <el-select v-model="filters.deviceId" placeholder="全部设备" clearable style="width: 240px">
            <el-option
              v-for="d in deviceList"
              :key="d.deviceId"
              :label="d.deviceName + ' (' + d.deviceId + ')'"
              :value="d.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="告警类型">
          <el-select v-model="filters.alarmType" placeholder="全部类型" clearable style="width: 180px">
            <el-option label="全部" value="" />
            <el-option label="高温告警" value="HIGH_TEMP" />
            <el-option label="低温告警" value="LOW_TEMP" />
            <el-option label="设备离线" value="DEVICE_OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.isCleared" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="全部" :value="undefined" />
            <el-option label="未清除" :value="0" />
            <el-option label="已清除" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 告警表格 -->
    <el-card shadow="never">
      <el-table
        :data="alarmList"
        v-loading="loading"
        stripe
        :row-class-name="rowClassName"
        style="width: 100%"
      >
        <el-table-column prop="deviceId" label="设备编号" width="160" />
        <el-table-column prop="alarmType" label="告警类型" width="130">
          <template #default="{ row }">
            <el-tag
              :type="alarmTypeTag(row.alarmType)"
              size="small"
              effect="dark"
            >
              {{ alarmTypeLabel(row.alarmType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmLevel" label="级别" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.alarmLevel === 'ERROR' ? 'danger' : 'warning'"
              size="small"
            >
              {{ row.alarmLevel === 'ERROR' ? '严重' : '警告' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmTitle" label="告警标题" width="150" show-overflow-tooltip />
        <el-table-column prop="alarmMessage" label="告警详情" min-width="220" show-overflow-tooltip />
        <el-table-column label="实际值/阈值" width="150">
          <template #default="{ row }">
            <span v-if="row.alarmValue != null">
              {{ row.alarmValue }} / {{ row.thresholdValue }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="告警时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isCleared === 1 ? 'success' : 'danger'" size="small">
              {{ row.isCleared === 1 ? '已清除' : '未清除' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.isCleared === 0"
              type="primary"
              size="small"
              link
              @click="handleClear(row.id)"
            >
              清除
            </el-button>
            <span v-else style="color: #909399; font-size: 12px">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 16px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadAlarms"
          @current-change="loadAlarms"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { getDeviceList, getAlarmList, clearAlarm } from '../api'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'

const deviceList = ref([])
const alarmList = ref([])
const loading = ref(false)
let timer = null

const filters = reactive({
  deviceId: '',
  alarmType: '',
  isCleared: undefined
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

function alarmTypeLabel(type) {
  const map = {
    HIGH_TEMP: '高温告警',
    LOW_TEMP: '低温告警',
    DEVICE_OFFLINE: '设备离线'
  }
  return map[type] || type
}

function alarmTypeTag(type) {
  const map = {
    HIGH_TEMP: 'danger',
    LOW_TEMP: 'warning',
    DEVICE_OFFLINE: 'info'
  }
  return map[type] || 'info'
}

function rowClassName({ row }) {
  return row.isCleared === 0 ? 'alarm-row-uncleared' : ''
}

async function loadDevices() {
  try {
    const page = await getDeviceList({ page: 1, size: 100 })
    deviceList.value = page.records || []
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
}

async function loadAlarms() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (filters.deviceId) params.deviceId = filters.deviceId
    if (filters.alarmType) params.alarmType = filters.alarmType
    if (filters.isCleared !== undefined && filters.isCleared !== null && filters.isCleared !== '') {
      params.isCleared = filters.isCleared
    }
    const result = await getAlarmList(params)
    alarmList.value = result.records || []
    pagination.total = result.total || 0
  } catch (e) {
    console.error('加载告警列表失败', e)
    alarmList.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.page = 1
  loadAlarms()
}

function reset() {
  filters.deviceId = ''
  filters.alarmType = ''
  filters.isCleared = undefined
  pagination.page = 1
  loadAlarms()
}

async function handleClear(alarmId) {
  try {
    await ElMessageBox.confirm('确认清除该告警？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await clearAlarm(alarmId)
    ElMessage.success('告警已清除')
    loadAlarms()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清除失败: ' + (e.message || '未知错误'))
    }
  }
}

onMounted(() => {
  loadDevices()
  loadAlarms()
  timer = setInterval(loadAlarms, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
:deep(.alarm-row-uncleared) {
  background-color: #fef0f0 !important;
  border-left: 3px solid #f56c6c;
}
</style>
