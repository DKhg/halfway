import api from './axios'

export function createAnonymousUser() {
  return api.post('/users').then((response) => response.data)
}

export function getMe() {
  return api.get('/users/me').then((response) => response.data)
}

export function updateHomeStation(homeStationName) {
  return api.patch('/users/me/home-station', { homeStationName })
}

// iOS 홈 화면 추가 전, 지금 쓰던 세션에서 4자리 이어하기 코드 발급
export function issuePairingCode() {
  return api.post('/users/me/pairing-code').then((response) => response.data)
}

// 홈 화면 PWA 최초 실행 때(토큰 없음) 코드로 기존 계정 토큰 복원
export function redeemPairingCode(code) {
  return api.post('/users/pairing-code/redeem', { code }).then((response) => response.data)
}
