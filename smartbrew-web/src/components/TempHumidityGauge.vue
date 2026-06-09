<template>
  <div ref="chartRef" style="width: 100%; height: 260px"></div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  value: { type: Number, default: 0 },
  max: { type: Number, default: 50 },
  unit: { type: String, default: '' },
  title: { type: String, default: '' }
})

const chartRef = ref(null)
let chart = null

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chart) return
  chart.setOption({
    series: [{
      type: 'gauge',
      min: 0,
      max: props.max,
      startAngle: 210,
      endAngle: -30,
      center: ['50%', '58%'],
      radius: '90%',
      axisLine: {
        lineStyle: {
          width: 16,
          color: [
            [0.3, '#67c23a'],
            [0.6, '#e6a23c'],
            [1, '#f56c6c']
          ]
        }
      },
      pointer: {
        length: '60%',
        width: 6,
        itemStyle: { color: '#303133' }
      },
      axisTick: {
        distance: -16,
        length: 6,
        lineStyle: { width: 1, color: '#999' }
      },
      splitLine: {
        distance: -22,
        length: 16,
        lineStyle: { width: 2, color: '#999' }
      },
      axisLabel: {
        distance: 24,
        fontSize: 12,
        color: '#666'
      },
      detail: {
        valueAnimation: true,
        fontSize: 28,
        fontWeight: 'bold',
        offsetCenter: [0, '65%'],
        formatter: '{value} ' + props.unit
      },
      title: {
        offsetCenter: [0, '32%'],
        fontSize: 14,
        color: '#666'
      },
      data: [{ value: props.value, name: props.title }]
    }]
  })
}

onMounted(() => {
  initChart()
})

watch(() => props.value, () => {
  updateChart()
})
</script>
