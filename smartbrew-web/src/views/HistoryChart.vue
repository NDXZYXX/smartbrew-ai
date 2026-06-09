<template>
  <div>
    <!-- 查询条件 -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <el-form :inline="true">
        <el-form-item label="选择设备">
          <el-select v-model="selectedDeviceId" placeholder="请选择设备" style="width: 280px">
            <el-option
              v-for="d in deviceList"
              :key="d.deviceId"
              :label="d.deviceName + ' (' + d.deviceId + ')'"
              :value="d.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 400px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch" :disabled="!selectedDeviceId">
            查询
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图表 -->
    <el-card shadow="hover" v-if="chartData.length > 0">
      <div ref="chartRef" style="width: 100%; height: 500px"></div>
    </el-card>
    <el-empty v-else description="请选择设备和时间范围后查询" />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getDeviceList, getDeviceHistory } from '../api'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const deviceList = ref([])
const selectedDeviceId = ref('')
const dateRange = ref([])
const chartData = ref([])
const chartRef = ref(null)
let chart = null

// 默认时间范围：最近24小时
function getDefaultDateRange() {
  const end = dayjs()
  const start = end.subtract(24, 'hour')
  return [start.format('YYYY-MM-DDTHH:mm:ss'), end.format('YYYY-MM-DDTHH:mm:ss')]
}

async function loadDeviceList() {
  try {
    const page = await getDeviceList({ page: 1, size: 100 })
    deviceList.value = page.records || []
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
}

async function doSearch() {
  if (!selectedDeviceId.value || !dateRange.value || dateRange.value.length !== 2) return
  try {
    const page = await getDeviceHistory({
      deviceId: selectedDeviceId.value,
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
      page: 1,
      size: 500
    })
    // 倒序排列，图表 X 轴按时间升序
    const records = (page.records || []).slice().reverse()
    chartData.value = records
    await nextTick()
    renderChart()
  } catch (e) {
    console.error('查询历史数据失败', e)
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const times = chartData.value.map(r => dayjs(r.createTime).format('MM-DD HH:mm'))
  const tankTemps = chartData.value.map(r => Number(r.tankTemperature) || 0)
  const envTemps = chartData.value.map(r => Number(r.envTemperature) || 0)
  const envHumidity = chartData.value.map(r => Number(r.envHumidity) || 0)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        let html = '<div style="font-weight:bold;margin-bottom:4px">' + params[0].axisValue + '</div>'
        params.forEach(p => {
          html += '<div>' + p.marker + ' ' + p.seriesName + ': ' + p.value + ' ' + p.unit + '</div>'
        })
        return html
      }
    },
    legend: {
      data: ['桶内温度', '环境温度', '环境湿度']
    },
    grid: {
      left: 60,
      right: 60,
      top: 60,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      data: times,
      axisLabel: { rotate: 45, fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '温度 (°C)',
        min: 0,
        max: 60,
        axisLabel: { formatter: '{value} °C' }
      },
      {
        type: 'value',
        name: '湿度 (%RH)',
        min: 0,
        max: 100,
        axisLabel: { formatter: '{value} %RH' }
      }
    ],
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, height: 24, bottom: 6 }
    ],
    series: [
      {
        name: '桶内温度',
        type: 'line',
        data: tankTemps,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#e6a23c', width: 2 },
        unit: '°C'
      },
      {
        name: '环境温度',
        type: 'line',
        data: envTemps,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#409eff', width: 2 },
        unit: '°C'
      },
      {
        name: '环境湿度',
        type: 'line',
        data: envHumidity,
        yAxisIndex: 1,
        smooth: true,
        symbol: 'none',
        lineStyle: { color: '#67c23a', width: 2 },
        unit: '%RH'
      }
    ]
  })
}

onMounted(() => {
  loadDeviceList()
  dateRange.value = getDefaultDateRange()
})
</script>
