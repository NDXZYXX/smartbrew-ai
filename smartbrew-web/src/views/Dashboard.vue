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

    <el-row :gutter="20" v-if="selectedDeviceId">
      <!-- 设备状态卡片 -->
      <el-col :span="8">
        <DeviceStatusCard :device="selectedDevice || {}" />
      </el-col>

      <!-- 仪表盘 -->
      <el-col :span="16">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-card shadow="hover">
              <TempHumidityGauge
                :value="latestData.tankTemperature || 0"
                :max="50"
                unit="°C"
                title="桶内温度"
              />
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <TempHumidityGauge
                :value="latestData.envHumidity || 0"
                :max="100"
                unit="%RH"
                title="环境湿度"
              />
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>

    <!-- 数据卡片 -->
    <el-row :gutter="20" style="margin-top: 20px" v-if="selectedDeviceId && latestData.createTime">
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align: center; padding: 10px 0">
            <div style="color: #909399; font-size: 14px; margin-bottom: 8px">桶内温度</div>
            <div style="font-size: 32px; font-weight: bold; color: #e6a23c">
              {{ latestData.tankTemperature ?? '-' }} °C
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align: center; padding: 10px 0">
            <div style="color: #909399; font-size: 14px; margin-bottom: 8px">环境温度</div>
            <div style="font-size: 32px; font-weight: bold; color: #409eff">
              {{ latestData.envTemperature ?? '-' }} °C
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align: center; padding: 10px 0">
            <div style="color: #909399; font-size: 14px; margin-bottom: 8px">环境湿度</div>
            <div style="font-size: 32px; font-weight: bold; color: #67c23a">
              {{ latestData.envHumidity ?? '-' }} %RH
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div style="text-align: center; padding: 10px 0">
            <div style="color: #909399; font-size: 14px; margin-bottom: 8px">更新时间</div>
            <div style="font-size: 18px; font-weight: bold; color: #303133">
              {{ formatTime(latestData.createTime) }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 未选择设备时的提示 -->
    <el-empty v-if="!selectedDeviceId" description="请选择一个设备查看实时数据" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getDeviceList, getDeviceLatest } from '../api'
import DeviceStatusCard from '../components/DeviceStatusCard.vue'
import TempHumidityGauge from '../components/TempHumidityGauge.vue'
import dayjs from 'dayjs'

const deviceList = ref([])
const selectedDeviceId = ref('')
const selectedDevice = ref(null)
const latestData = ref({})
let timer = null

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('HH:mm:ss')
}

async function loadDeviceList() {
  try {
    const page = await getDeviceList({ page: 1, size: 100 })
    deviceList.value = page.records || []
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
}

async function loadLatestData() {
  if (!selectedDeviceId.value) return
  try {
    const data = await getDeviceLatest(selectedDeviceId.value)
    latestData.value = data || {}
  } catch (e) {
    console.error('加载最新数据失败', e)
    latestData.value = {}
  }
}

function onDeviceChange() {
  selectedDevice.value = deviceList.value.find(d => d.deviceId === selectedDeviceId.value)
  loadLatestData()
}

onMounted(() => {
  loadDeviceList()
  timer = setInterval(loadLatestData, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
