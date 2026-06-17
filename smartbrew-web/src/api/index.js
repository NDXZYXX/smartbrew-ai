import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code === 200) {
      return body.data
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (err) => {
    return Promise.reject(err)
  }
)

export const getDeviceList = (params) => http.get('/device/list', { params })
export const getDeviceLatest = (deviceId) => http.get('/device/latest', { params: { deviceId } })
export const getDeviceHistory = (params) => http.get('/device/history', { params })
export const getDeviceDetail = (deviceId) => http.get(`/device/${deviceId}`)

export const getAlarmList = (params) => http.get('/alarm/list', { params })
export const clearAlarm = (alarmId) => http.put(`/alarm/${alarmId}/clear`)
