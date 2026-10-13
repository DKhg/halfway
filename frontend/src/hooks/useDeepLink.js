import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Capacitor } from '@capacitor/core'
import { App as CapacitorApp } from '@capacitor/app'

// 네이티브 앱에서 App Links(예: 카카오톡 채팅방 링크 탭)로 열렸을 때, 전체 URL 대신 경로만 꺼내서
// SPA 라우팅으로 이어줌. 웹(PWA)에서는 이 이벤트 자체가 발생하지 않아 아무 영향 없음
export function useDeepLink() {
  const navigate = useNavigate()

  useEffect(() => {
    if (!Capacitor.isNativePlatform()) {
      return
    }

    const listenerHandle = CapacitorApp.addListener('appUrlOpen', ({ url }) => {
      try {
        const { pathname, search } = new URL(url)
        navigate(pathname + search)
      } catch {
        // 형식이 이상한 URL은 조용히 무시
      }
    })

    return () => {
      listenerHandle.then((listener) => listener.remove())
    }
  }, [navigate])
}
