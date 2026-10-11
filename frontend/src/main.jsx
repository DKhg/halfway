import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { isKakaoTalkInAppBrowser, openInSystemBrowser } from './utils/kakaoInAppBrowser'

// 카카오톡 인앱 브라우저면 익명 토큰이 저장될 틈도 없이 바로 시스템 브라우저로 튕겨내고, 앱은 렌더링하지 않음
if (isKakaoTalkInAppBrowser()) {
  openInSystemBrowser()
} else {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  )
}
