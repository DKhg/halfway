import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getRoom } from '../api/roomApi'
import { getMe, updateHomeStation } from '../api/userApi'
import { getAllStationNames } from '../api/stationApi'
import { useInstallPrompt } from '../hooks/useInstallPrompt'
import { isIOS, isStandalone } from '../utils/platform'
import Modal from '../components/common/Modal'
import MyRoomsModal from '../components/common/MyRoomsModal'
import StationAutocompleteInput from '../components/common/StationAutocompleteInput'
import IosInstallModal from '../components/common/IosInstallModal'
import './HomePage.css'

const STEPS = [
  { number: 1, label: '출발역 넣으면 공평한 중간지점 찾아주고', accent: 'yellow' },
  { number: 2, label: '투표로 장소 정하고 가게까지 확정하고', accent: 'green' },
  { number: 3, label: '막차 확인하고 정산까지', accent: 'pink' },
]

// 전체 초대 링크를 붙여넣어도, 방 코드만 입력해도 되게 둘 다 받아줌
function extractRoomCode(input) {
  const trimmed = input.trim()
  const match = trimmed.match(/\/rooms\/([^/?#]+)/)
  return match ? match[1] : trimmed
}

function HomePage() {
  const navigate = useNavigate()
  const { canInstall, promptInstall } = useInstallPrompt()
  const [isIosInstallOpen, setIsIosInstallOpen] = useState(false)
  const [isMyRoomsOpen, setIsMyRoomsOpen] = useState(false)
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false)
  const [inviteInput, setInviteInput] = useState('')
  const [inviteError, setInviteError] = useState(null)
  const [isCheckingInvite, setIsCheckingInvite] = useState(false)
  const [isHomeStationModalOpen, setIsHomeStationModalOpen] = useState(false)
  const [homeStationInput, setHomeStationInput] = useState('')
  const [stationOptions, setStationOptions] = useState([])
  const [isSavingHomeStation, setIsSavingHomeStation] = useState(false)
  const [homeStationError, setHomeStationError] = useState(null)
  const [homeStationSaved, setHomeStationSaved] = useState(false)

  const handleOpenInviteModal = () => {
    setInviteInput('')
    setInviteError(null)
    setIsInviteModalOpen(true)
  }

  const handleOpenHomeStationModal = async () => {
    setHomeStationError(null)
    setHomeStationSaved(false)
    setIsHomeStationModalOpen(true)
    try {
      const [stations, me] = await Promise.all([getAllStationNames(), getMe()])
      setStationOptions(stations)
      setHomeStationInput(me.homeStationName || '')
    } catch {
      setHomeStationError('귀가역 정보를 불러오지 못했어요.')
    }
  }

  const handleSaveHomeStation = async () => {
    if (isSavingHomeStation) {
      return
    }
    setIsSavingHomeStation(true)
    setHomeStationError(null)
    try {
      await updateHomeStation(homeStationInput.trim())
      setHomeStationSaved(true)
      setTimeout(() => setHomeStationSaved(false), 2000)
    } catch (error) {
      setHomeStationError(
        error.response?.status === 400 ? '등록되지 않은 역 이름이에요.' : '저장하지 못했어요. 다시 시도해 주세요.',
      )
    } finally {
      setIsSavingHomeStation(false)
    }
  }

  // 방 존재 여부를 먼저 확인하고, 있을 때만 이동. 없으면 모달 안에 에러만 띄우고 페이지는 안 넘어감
  const handleJoinByInvite = async () => {
    const code = extractRoomCode(inviteInput)
    if (!code || isCheckingInvite) {
      return
    }
    setIsCheckingInvite(true)
    setInviteError(null)
    try {
      await getRoom(code)
      navigate(`/rooms/${code}`)
    } catch (error) {
      setInviteError(error.response?.status === 404 ? '존재하지 않는 방이에요.' : '방을 확인하지 못했어요. 다시 시도해 주세요.')
      setIsCheckingInvite(false)
    }
  }

  return (
    <div className="home-page">
      <header className="home-header">
        <span className="home-logo">어디서봄</span>
        <div className="home-header-actions">
          <button type="button" className="home-my-rooms-btn" onClick={() => setIsMyRoomsOpen(true)}>
            내 약속
          </button>
          <button type="button" className="home-my-rooms-btn home-set-station-btn" onClick={handleOpenHomeStationModal}>
            귀가역 설정
          </button>
          {!isStandalone() && canInstall && (
            <button type="button" className="home-my-rooms-btn home-install-btn" onClick={promptInstall}>
              앱 설치하기
            </button>
          )}
          {!isStandalone() && !canInstall && isIOS() && (
            <button type="button" className="home-my-rooms-btn home-install-btn" onClick={() => setIsIosInstallOpen(true)}>
              홈 화면에 추가
            </button>
          )}
        </div>
      </header>

      <div className="home-hero">
        <h1>{'어디서\n만나?'}</h1>
        <div className="home-badge-group">
          <span className="home-badge home-badge--pink">딱 중간!</span>
          <span className="home-badge home-badge--blue">링크 한 방이면 끝</span>
        </div>
      </div>

      <p className="home-lead">
        친구들이 각자 출발지를 넣으면 모두에게 공평한 곳을 딱 골라줘요. 단톡방 논쟁은 이제 그만.
      </p>

      <ol className="home-steps">
        {STEPS.map((step) => (
          <li key={step.number} className={`home-step home-step--${step.accent}`}>
            <span className="home-step-number">{step.number}</span>
            <span>{step.label}</span>
          </li>
        ))}
      </ol>

      <div className="home-cta-group">
        <Link to="/rooms/new" className="home-cta-primary">
          약속 방 만들기
        </Link>
        <button type="button" className="home-cta-secondary" onClick={handleOpenInviteModal}>
          초대 링크 받았어요
        </button>
      </div>

      {isMyRoomsOpen && <MyRoomsModal onClose={() => setIsMyRoomsOpen(false)} />}

      {isIosInstallOpen && <IosInstallModal onClose={() => setIsIosInstallOpen(false)} />}

      {isInviteModalOpen && (
        <Modal title="초대 링크로 참여" onClose={() => setIsInviteModalOpen(false)}>
          <input
            type="text"
            placeholder="받은 링크 또는 방 코드를 붙여넣어 주세요"
            className="home-invite-input"
            autoFocus
            value={inviteInput}
            onChange={(e) => {
              setInviteInput(e.target.value)
              setInviteError(null)
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                handleJoinByInvite()
              }
            }}
          />
          {inviteError && <div className="home-invite-error">{inviteError}</div>}
          <button
            type="button"
            className="home-invite-submit-btn"
            disabled={!extractRoomCode(inviteInput) || isCheckingInvite}
            onClick={handleJoinByInvite}
          >
            {isCheckingInvite ? '확인하는 중...' : '참여하기'}
          </button>
        </Modal>
      )}

      {isHomeStationModalOpen && (
        <Modal title="귀가역 설정" onClose={() => setIsHomeStationModalOpen(false)}>
          <p className="home-invite-lead">
            자주 가는 귀가역을 저장해두면 막차 계산할 때 매번 안 입력해도 돼요.
          </p>
          <StationAutocompleteInput
            placeholder="귀가역 또는 초성(ㄱㄴ)"
            value={homeStationInput}
            onChange={(value) => {
              setHomeStationInput(value)
              setHomeStationError(null)
            }}
            stationOptions={stationOptions}
          />
          {homeStationError && <div className="home-invite-error">{homeStationError}</div>}
          <button
            type="button"
            className="home-invite-submit-btn"
            disabled={isSavingHomeStation}
            onClick={handleSaveHomeStation}
          >
            {isSavingHomeStation ? '저장하는 중...' : homeStationSaved ? '저장됨!' : '저장'}
          </button>
        </Modal>
      )}
    </div>
  )
}

export default HomePage
