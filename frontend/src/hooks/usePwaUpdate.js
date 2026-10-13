import { useEffect } from 'react'
import { registerSW } from 'virtual:pwa-register'
import { Capacitor } from '@capacitor/core'
import { notifyNeedRefresh, setApplyUpdate } from '../utils/pwaUpdateBus'

const UPDATE_CHECK_INTERVAL_MS = 60 * 60 * 1000 // 1시간마다 백그라운드에서 새 버전 체크

// 모듈 스코프 플래그: StrictMode가 effect를 두 번 실행해도 서비스워커 등록은 한 번만 하기 위함
let hasRegistered = false

export function usePwaUpdate() {
  useEffect(() => {
    // 네이티브 앱은 업데이트를 플레이스토어로 받으므로 서비스워커 갱신 배너 자체가 의미 없음
    if (hasRegistered || Capacitor.isNativePlatform()) {
      return
    }
    hasRegistered = true

    const updateSW = registerSW({
      onNeedRefresh() {
        notifyNeedRefresh()
      },
      onRegisteredSW(_swUrl, registration) {
        if (!registration) {
          return
        }

        const checkForUpdate = () => registration.update().catch(() => {})
        setInterval(checkForUpdate, UPDATE_CHECK_INTERVAL_MS)

        // 홈 화면 아이콘에서 재개(백그라운드 → 포그라운드)될 때마다 새 버전 체크
        document.addEventListener('visibilitychange', () => {
          if (document.visibilityState === 'visible') {
            checkForUpdate()
          }
        })
        window.addEventListener('focus', checkForUpdate)
      },
    })

    setApplyUpdate(updateSW)
  }, [])
}
