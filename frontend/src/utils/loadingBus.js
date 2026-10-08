// axios 인터셉터가 요청 시작·종료를 알리면, 구독한 컴포넌트(전역 로딩바)가 그 상태를 받아 렌더링
let activeCount = 0
const listeners = new Set()

export function startRequest() {
  activeCount += 1
  notify()
}

export function endRequest() {
  activeCount = Math.max(0, activeCount - 1)
  notify()
}

function notify() {
  const isLoading = activeCount > 0
  listeners.forEach((listener) => listener(isLoading))
}

export function subscribeLoading(listener) {
  listener(activeCount > 0)
  listeners.add(listener)
  return () => listeners.delete(listener)
}
