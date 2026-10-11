import { isInstallPromptAvailable } from './installPromptBus'

const SEEN_KEY = 'halfway_a2hs_prompt_seen'

export function isStandalone() {
  return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true
}

export function isIOS() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream
}

export function isAndroid() {
  return /Android/.test(navigator.userAgent)
}

// 방 만들기 직전에 홈 화면 추가를 제안할지 여부
// - 이미 홈 화면 앱으로 실행 중이거나 이 기기에서 전에 본 적 있으면 다시 안 띄움
// - 안드로이드는 네이티브 설치창을 실제로 띄울 수 있을 때만(beforeinstallprompt 캡처됨) 의미가 있음
export function shouldPromptAddToHomeScreen() {
  if (isStandalone() || localStorage.getItem(SEEN_KEY) === 'true') {
    return false
  }
  if (isIOS()) {
    return true
  }
  if (isAndroid()) {
    return isInstallPromptAvailable()
  }
  return false
}

export function markAddToHomeScreenPromptSeen() {
  localStorage.setItem(SEEN_KEY, 'true')
}
