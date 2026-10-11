// beforeinstallprompt는 페이지 로드 초기에만 발생하고 한 번 쓰면 재사용 불가라, 앱 시작 시점부터 잡아뒀다가
// 나중에(예: 방 만들기 직전 모달) 사용자 클릭에 반응해 꺼내 쓰기 위한 모듈
let deferredPrompt = null

export function captureInstallPrompt(event) {
  event.preventDefault()
  deferredPrompt = event
}

export function isInstallPromptAvailable() {
  return deferredPrompt !== null
}

// 반드시 버튼 클릭 같은 사용자 제스처 안에서 호출해야 네이티브 설치창이 뜸
export async function triggerInstallPrompt() {
  if (!deferredPrompt) {
    return
  }
  const event = deferredPrompt
  deferredPrompt = null
  event.prompt()
  await event.userChoice
}
