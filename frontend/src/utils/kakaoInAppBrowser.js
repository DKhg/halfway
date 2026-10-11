// 카카오톡 인앱 브라우저는 사파리·크롬과 저장공간(localStorage)이 분리돼 있어서,
// 같은 사람이어도 인앱으로 들어오면 익명 토큰이 안 이어지고 매번 새 사용자로 취급됨.
// 그래서 열리자마자 시스템 기본 브라우저로 튕겨서 저장공간을 하나로 통일시킴
export function isKakaoTalkInAppBrowser() {
  return /KAKAOTALK/i.test(navigator.userAgent)
}

export function openInSystemBrowser() {
  location.href = `kakaotalk://web/openExternal?url=${encodeURIComponent(location.href)}`
}
