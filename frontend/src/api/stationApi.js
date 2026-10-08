import api from './axios'

let cachedNamesPromise = null

// 역 이름은 배치 적재 때만 바뀌므로 앱 세션 동안 한 번만 요청
export function getAllStationNames() {
  if (!cachedNamesPromise) {
    cachedNamesPromise = api.get('/stations/names').then((response) => response.data)
  }
  return cachedNamesPromise
}
