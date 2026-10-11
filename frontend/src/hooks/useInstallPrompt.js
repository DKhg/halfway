import { useEffect } from 'react'
import { captureInstallPrompt } from '../utils/installPromptBus'

// StrictMode가 effect를 두 번 실행해도 리스너는 한 번만 등록하기 위한 모듈 스코프 플래그
let hasRegistered = false

export function useInstallPrompt() {
  useEffect(() => {
    if (hasRegistered) {
      return
    }
    hasRegistered = true
    window.addEventListener('beforeinstallprompt', captureInstallPrompt)
  }, [])
}
