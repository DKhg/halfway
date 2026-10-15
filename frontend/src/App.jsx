import { Route, Routes } from 'react-router-dom'
import { useAnonymousUser } from './hooks/useAnonymousUser'
import { useDeepLink } from './hooks/useDeepLink'
import { usePwaUpdate } from './hooks/usePwaUpdate'
import { useInstallPrompt } from './hooks/useInstallPrompt'
import GlobalLoadingBar from './components/common/GlobalLoadingBar'
import UpdateBanner from './components/common/UpdateBanner'
import HomePage from './pages/HomePage'
import JoinPage from './pages/JoinPage'
import CreateRoomPage from './pages/CreateRoomPage'
import SettlementPage from './pages/SettlementPage'
import PrivacyPolicyPage from './pages/PrivacyPolicyPage'

function App() {
  const { isReady, error } = useAnonymousUser()
  useDeepLink()
  usePwaUpdate()
  useInstallPrompt()

  if (error) {
    return <div>{error}</div>
  }

  // 방 생성 등 인증이 필요한 API를 부르기 전에 익명 사용자 토큰부터 준비
  if (!isReady) {
    return null
  }

  return (
    <>
      <GlobalLoadingBar />
      <UpdateBanner />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/rooms/new" element={<CreateRoomPage />} />
        <Route path="/rooms/:code" element={<JoinPage />} />
        <Route path="/rooms/:code/settlement" element={<SettlementPage />} />
        <Route path="/privacy" element={<PrivacyPolicyPage />} />
      </Routes>
    </>
  )
}

export default App
