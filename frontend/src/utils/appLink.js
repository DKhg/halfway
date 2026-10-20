// 커스텀 스킴(토스·카카오톡 등) 딥링크를 열어보고, 해당 앱이 없어서 실패한 것 같으면 onFail 콜백 실행
// 페이지를 벗어나면(다른 앱으로 전환) visibilitychange가 hidden으로 바뀌는 걸로 성공 여부를 추정
export function openAppOrFallback(url, onFail, timeoutMs = 1500) {
  let settled = false

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'hidden') {
      settled = true
      cleanup()
    }
  }

  const cleanup = () => {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  }

  document.addEventListener('visibilitychange', handleVisibilityChange)

  setTimeout(() => {
    cleanup()
    if (!settled) {
      onFail()
    }
  }, timeoutMs)

  window.location.href = url
}
