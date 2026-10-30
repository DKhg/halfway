import { useState } from 'react'
import { Capacitor } from '@capacitor/core'
import './OpenInAppBanner.css'

const DISMISSED_KEY = 'halfway_open_in_app_dismissed'
const APP_STORE_URL = 'https://apps.apple.com/kr/app/id6825186177'
const APP_SWITCH_TIMEOUT_MS = 1000

function isIOSSafari() {
  return /iPhone|iPad|iPod/.test(navigator.userAgent)
}

// iOS 유니버설 링크가 아직 안 뚫려서, Safari로 들어온 사용자가 커스텀 URL 스킴으로
// 수동으로나마 앱으로 넘어갈 수 있게 하는 배너. 안드로이드는 App Links가 이미 정상 동작해서 노출 안 함
function OpenInAppBanner({ code }) {
  const [isDismissed, setIsDismissed] = useState(() => localStorage.getItem(DISMISSED_KEY) === '1')

  if (Capacitor.isNativePlatform() || !isIOSSafari() || isDismissed) {
    return null
  }

  const handleOpen = () => {
    const fallbackTimer = setTimeout(() => {
      window.location.href = APP_STORE_URL
    }, APP_SWITCH_TIMEOUT_MS)

    // 앱으로 전환되면 페이지가 백그라운드로 가므로, 그 시점에 스토어 이동을 취소
    document.addEventListener(
      'visibilitychange',
      () => {
        if (document.hidden) {
          clearTimeout(fallbackTimer)
        }
      },
      { once: true },
    )

    window.location.href = `halfway:///rooms/${code}`
  }

  const handleDismiss = () => {
    localStorage.setItem(DISMISSED_KEY, '1')
    setIsDismissed(true)
  }

  return (
    <div className="open-in-app-banner" role="status">
      <span className="open-in-app-banner-text">어디서봄 앱에서 보기</span>
      <div className="open-in-app-banner-actions">
        <button type="button" className="open-in-app-banner-open-btn" onClick={handleOpen}>
          열기
        </button>
        <button type="button" className="open-in-app-banner-close-btn" aria-label="닫기" onClick={handleDismiss}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
            <path d="M6 6l12 12M18 6L6 18" />
          </svg>
        </button>
      </div>
    </div>
  )
}

export default OpenInAppBanner
