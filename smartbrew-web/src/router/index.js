import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import HistoryChart from '../views/HistoryChart.vue'

const routes = [
  { path: '/', name: 'Dashboard', component: Dashboard },
  { path: '/history', name: 'HistoryChart', component: HistoryChart }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
