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
        <el-form-item label="分析类型">
          <el-select v-model="filters.analysisType" placeholder="全部类型" clearable style="width: 180px">
            <el-option label="全部" value="" />
            <el-option label="状态分析" value="STATUS_ANALYSIS" />
            <el-option label="周期预测" value="CYCLE_PREDICT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="openTriggerDialog">触发分析</el-button>
          <el-button @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 分析记录表格 -->
    <el-card shadow="never">
      <el-table :data="analysisList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="deviceId" label="设备编号" width="140" />
        <el-table-column prop="analysisType" label="分析类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.analysisType === 'STATUS_ANALYSIS' ? 'primary' : 'success'" size="small" effect="dark">
              {{ row.analysisType === 'STATUS_ANALYSIS' ? '状态分析' : '周期预测' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="statusAssessment" label="状态评估" min-width="180" show-overflow-tooltip />
        <el-table-column prop="riskWarning" label="风险提示" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.riskWarning && row.riskWarning !== '无'" type="danger" size="small" effect="dark">
              {{ row.riskWarning }}
            </el-tag>
            <span v-else style="color: #67c23a">无风险</span>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="建议" min-width="200" show-overflow-tooltip />
        <el-table-column label="预计完成时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.predictedEndTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="aiModel" label="模型" width="130" />
        <el-table-column label="响应时间" width="100">
          <template #default="{ row }">
            {{ row.responseTimeMs != null ? row.responseTimeMs + 'ms' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showDetail(row)">详情</el-button>
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
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 触发分析 Dialog -->
    <el-dialog v-model="triggerDialogVisible" title="触发 AI 分析" width="480px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="选择设备">
          <el-select v-model="triggerForm.deviceId" placeholder="请选择设备" style="width: 100%">
            <el-option
              v-for="d in deviceList"
              :key="d.deviceId"
              :label="d.deviceName + ' (' + d.deviceId + ')'"
              :value="d.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分析类型">
          <el-select v-model="triggerForm.analysisType" style="width: 100%">
            <el-option label="状态分析" value="STATUS_ANALYSIS" />
            <el-option label="周期预测" value="CYCLE_PREDICT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="triggering" @click="doTrigger">开始分析</el-button>
      </template>
    </el-dialog>

    <!-- 详情 Dialog -->
    <el-dialog v-model="detailDialogVisible" title="AI 分析详情" width="720px" :close-on-click-modal="false">
      <template v-if="detailRow">
        <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="设备编号">{{ detailRow.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="分析类型">
            <el-tag :type="detailRow.analysisType === 'STATUS_ANALYSIS' ? 'primary' : 'success'" size="small" effect="dark">
              {{ detailRow.analysisType === 'STATUS_ANALYSIS' ? '状态分析' : '周期预测' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态评估">{{ detailRow.statusAssessment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="风险提示">{{ detailRow.riskWarning || '-' }}</el-descriptions-item>
          <el-descriptions-item label="调整建议">{{ detailRow.suggestion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计完成时间">{{ formatTime(detailRow.predictedEndTime) }}</el-descriptions-item>
          <el-descriptions-item label="AI 模型">{{ detailRow.aiModel }}</el-descriptions-item>
          <el-descriptions-item label="响应时间">{{ detailRow.responseTimeMs != null ? detailRow.responseTimeMs + 'ms' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detailRow.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">AI 提示词 (Prompt)</el-divider>
        <el-input
          v-model="detailRow.prompt"
          type="textarea"
          :rows="6"
          readonly
          style="margin-bottom: 16px"
        />

        <el-divider content-position="left">AI 原始响应</el-divider>
        <el-input
          v-model="detailRow.analysisResult"
          type="textarea"
          :rows="8"
          readonly
        />
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { getDeviceList, getAiAnalysisList, triggerAiAnalysis, getAiAnalysisDetail } from '../api'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'

const deviceList = ref([])
const analysisList = ref([])
const loading = ref(false)
let timer = null

const filters = reactive({
  deviceId: '',
  analysisType: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 触发分析
const triggerDialogVisible = ref(false)
const triggering = ref(false)
const triggerForm = reactive({
  deviceId: '',
  analysisType: 'STATUS_ANALYSIS'
})

// 详情
const detailDialogVisible = ref(false)
const detailRow = ref(null)

function formatTime(time) {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

async function loadDevices() {
  try {
    const page = await getDeviceList({ page: 1, size: 100 })
    deviceList.value = page.records || []
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (filters.deviceId) params.deviceId = filters.deviceId
    if (filters.analysisType) params.analysisType = filters.analysisType
    const result = await getAiAnalysisList(params)
    analysisList.value = result.records || []
    pagination.total = result.total || 0
  } catch (e) {
    console.error('加载AI分析列表失败', e)
    analysisList.value = []
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.page = 1
  loadList()
}

function reset() {
  filters.deviceId = ''
  filters.analysisType = ''
  pagination.page = 1
  loadList()
}

function openTriggerDialog() {
  triggerForm.deviceId = deviceList.value.length > 0 ? deviceList.value[0].deviceId : ''
  triggerForm.analysisType = 'STATUS_ANALYSIS'
  triggerDialogVisible.value = true
}

async function doTrigger() {
  if (!triggerForm.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }
  triggering.value = true
  try {
    await triggerAiAnalysis({
      deviceId: triggerForm.deviceId,
      analysisType: triggerForm.analysisType
    })
    ElMessage.success('AI 分析完成')
    triggerDialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('AI 分析失败: ' + (e.message || '未知错误'))
  } finally {
    triggering.value = false
  }
}

async function showDetail(row) {
  try {
    detailRow.value = await getAiAnalysisDetail(row.id)
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败: ' + (e.message || '未知错误'))
  }
}

onMounted(() => {
  loadDevices()
  loadList()
  timer = setInterval(loadList, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
