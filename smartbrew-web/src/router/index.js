import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import HistoryChart from '../views/HistoryChart.vue'
import AlarmCenter from '../views/AlarmCenter.vue'
import ControlPanel from '../views/ControlPanel.vue'

const routes = [
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/history', name: 'HistoryChart', component: HistoryChart },
  { path: '/alarms', name: 'AlarmCenter', component: AlarmCenter },
  { path: '/control', name: 'ControlPanel', component: ControlPanel }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
