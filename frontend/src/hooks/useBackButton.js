import { useEffect } from 'react'
import { Capacitor } from '@capacitor/core'
import { App as CapacitorApp } from '@capacitor/app'

// 네이티브 앱은 하드웨어 뒤로가기를 직접 처리해줘야 함 — 더 갈 페이지가 없는데도
// 가만히 있으면 사용자가 뒤로가기를 아무리 눌러도 앱이 안 꺼지는 것처럼 보임
export function useBackButton() {
  useEffect(() => {
    if (!Capacitor.isNativePlatform()) {
      return
    }

    const listenerHandle = CapacitorApp.addListener('backButton', ({ canGoBack }) => {
      if (canGoBack) {
        window.history.back()
      } else {
        CapacitorApp.exitApp()
      }
    })

    return () => {
      listenerHandle.then((listener) => listener.remove())
    }
  }, [])
}
