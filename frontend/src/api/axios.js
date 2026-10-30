import axios from 'axios'
import { clearToken, getToken } from '../utils/authToken'
import { endRequest, startRequest } from '../utils/loadingBus'

const shouldLogApi = import.meta.env.DEV

// 저장된 토큰이 서버에 없는(폰에만 남아있던 옛 토큰 등) 경우 401이 한 번 나면 이후 모든 인증 요청이 계속 막히므로,
// 세션당 한 번만 토큰을 지우고 새로 고쳐서 재발급을 유도. 재발급 후에도 또 401이면 루프 돌지 않고 그대로 둠
const TOKEN_RECOVERY_FLAG = 'halfway_token_recovery_attempted'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  // 예전에 CapacitorHttp(네이티브 HTTP 브릿지)가 응답을 파싱된 객체가 아니라 JSON 문자열째로
  // 넘겨주는 버그가 있어서 넣은 방어 코드. capacitor.config.json에서 CapacitorHttp를 꺼서
  // 지금은 안 터지는 경로지만, 혹시 나중에 다시 켤 경우를 대비해 남겨둠
  // (문자열이면 한 번 더 파싱, 이미 파싱된 경우는 그대로 통과)
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
    if (error.response?.status === 401 && getToken() && !sessionStorage.getItem(TOKEN_RECOVERY_FLAG)) {
      sessionStorage.setItem(TOKEN_RECOVERY_FLAG, '1')
      clearToken()
      window.location.reload()
    }
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
