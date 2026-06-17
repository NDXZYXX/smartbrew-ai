import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import HistoryChart from '../views/HistoryChart.vue'
import AlarmCenter from '../views/AlarmCenter.vue'
import ControlPanel from '../views/ControlPanel.vue'
import AiAnalysis from '../views/AiAnalysis.vue'
import KnowledgeBase from '../views/KnowledgeBase.vue'

const routes = [
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/history', name: 'HistoryChart', component: HistoryChart },
  { path: '/alarms', name: 'AlarmCenter', component: AlarmCenter },
  { path: '/control', name: 'ControlPanel', component: ControlPanel },
  { path: '/ai', name: 'AiAnalysis', component: AiAnalysis },
  { path: '/knowledge', name: 'KnowledgeBase', component: KnowledgeBase }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
