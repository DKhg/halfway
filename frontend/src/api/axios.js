import axios from 'axios'
import { getToken } from '../utils/authToken'
import { endRequest, startRequest } from '../utils/loadingBus'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 공통 로딩바: 요청이 하나라도 떠 있으면 표시, 전부 끝나면 숨김
api.interceptors.request.use((config) => {
  startRequest()
  return config
})
api.interceptors.response.use(
  (response) => {
    endRequest()
    return response
  },
  (error) => {
    endRequest()
    return Promise.reject(error)
  },
)

if (import.meta.env.DEV) {
  api.interceptors.request.use((config) => {
    console.log('[API REQ]', config.method, config.url, config.data)
    return config
  })

  api.interceptors.response.use(
    (response) => {
      console.log('[API RES]', response.config.url, response.data)
      return response
    },
    (error) => {
      console.error('[API ERR]', error.config?.url, error.response?.data || error.message)
      return Promise.reject(error)
    },
  )
}

export default api
