import { Route, Routes } from 'react-router-dom'
import { useAnonymousUser } from './hooks/useAnonymousUser'
import { usePwaUpdate } from './hooks/usePwaUpdate'
import GlobalLoadingBar from './components/common/GlobalLoadingBar'
import UpdateBanner from './components/common/UpdateBanner'
import PairingRestoreScreen from './components/common/PairingRestoreScreen'
import HomePage from './pages/HomePage'
import JoinPage from './pages/JoinPage'
import CreateRoomPage from './pages/CreateRoomPage'
import SettlementPage from './pages/SettlementPage'

function App() {
  const { isReady, error, needsPairingChoice, startFresh, applyRestoredToken } = useAnonymousUser()
  usePwaUpdate()

  if (error) {
    return <div>{error}</div>
  }

  // 홈 화면 PWA 최초 실행: 다른 브라우저 세션의 식별자를 코드로 이어받을지 먼저 물어봄
  if (needsPairingChoice) {
    return <PairingRestoreScreen onRestored={applyRestoredToken} onStartFresh={startFresh} />
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
      </Routes>
    </>
  )
}

export default App
