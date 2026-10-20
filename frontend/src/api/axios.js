import axios from 'axios'
import { Capacitor } from '@capacitor/core'
import { getToken } from '../utils/authToken'
import { endRequest, startRequest } from '../utils/loadingBus'

// 네이티브 앱에서만 벌어지는 API 응답 문제를 진단하기 위해, 운영(prod) 빌드에서도
// 네이티브 플랫폼일 때는 로그를 찍음 — 원인 파악 후 제거할 임시 코드
const shouldLogApi = import.meta.env.DEV || Capacitor.isNativePlatform()

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  // 네이티브 앱(Capacitor)에서 응답이 파싱된 객체가 아니라 JSON 문자열 그대로 오는 경우가 있어서,
  // 문자열이면 한 번 더 파싱해줌 (이미 파싱된 경우는 그대로 통과)
  transformResponse: [(data) => {
    if (typeof data === 'string') {
      try {
        return JSON.parse(data)
      } catch {
        return data
      }
    }
    return data
  }],
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

if (shouldLogApi) {
  api.interceptors.request.use((config) => {
    console.log('[API REQ]', config.method, config.url, config.data)
    return config
  })

  api.interceptors.response.use(
    (response) => {
      console.log(
        '[API RES]', response.config.url,
        'content-type:', response.headers['content-type'],
        'typeof data:', typeof response.data,
        response.data,
      )
      return response
    },
    (error) => {
      console.error('[API ERR]', error.config?.url, error.response?.data || error.message)
      return Promise.reject(error)
    },
  )
}

export default api
