// 홈 화면에 설치되어 standalone(PWA)으로 실행 중인지 판별
export function isStandalone() {
  return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true
}

// iOS 판별. iPadOS 13+는 데스크톱 모드라 UA에 "ipad"가 안 찍혀서 터치 포인트로 추가 보정
export function isIOS() {
  const userAgent = window.navigator.userAgent.toLowerCase()
  const isAppleMobileUA = /iphone|ipad|ipod/.test(userAgent)
  const isIPadDesktopMode = window.navigator.platform === 'MacIntel' && window.navigator.maxTouchPoints > 1
  return isAppleMobileUA || isIPadDesktopMode
}
