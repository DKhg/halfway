// usePwaUpdate 훅이 새 버전 감지·적용 함수를 등록하면, 구독한 컴포넌트(업데이트 배너)가 그 상태를 받아 렌더링
let needsRefresh = false
let applyUpdate = null
const listeners = new Set()

export function notifyNeedRefresh() {
  needsRefresh = true
  listeners.forEach((listener) => listener(true))
}

export function setApplyUpdate(updateFn) {
  applyUpdate = updateFn
}

export function applyPwaUpdate() {
  applyUpdate?.(true)
}

export function subscribePwaUpdate(listener) {
  listener(needsRefresh)
  listeners.add(listener)
  return () => listeners.delete(listener)
}
