import { useEffect, useState } from 'react'

const DISMISS_STORAGE_KEY = 'halfway_install_dismissed_at'
const DISMISS_COOLDOWN_MS = 14 * 24 * 60 * 60 * 1000 // 설치 거부하면 2주간 다시 안 띄움

// 안드로이드 Chrome 등에서 제공하는 네이티브 설치 배너(beforeinstallprompt)를 가로채서 직접 띄움
export function useInstallPrompt() {
  const [deferredPrompt, setDeferredPrompt] = useState(null)

  useEffect(() => {
    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault()
      const dismissedAt = Number(localStorage.getItem(DISMISS_STORAGE_KEY) || 0)
      if (Date.now() - dismissedAt < DISMISS_COOLDOWN_MS) {
        return
      }
      setDeferredPrompt(e)
    }
    const handleAppInstalled = () => setDeferredPrompt(null)

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
    window.addEventListener('appinstalled', handleAppInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt)
      window.removeEventListener('appinstalled', handleAppInstalled)
    }
  }, [])

  const promptInstall = async () => {
    if (!deferredPrompt) {
      return
    }
    deferredPrompt.prompt()
    const { outcome } = await deferredPrompt.userChoice
    if (outcome === 'dismissed') {
      localStorage.setItem(DISMISS_STORAGE_KEY, String(Date.now()))
    }
    setDeferredPrompt(null)
  }

  return { canInstall: Boolean(deferredPrompt), promptInstall }
}
