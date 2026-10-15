import { Fragment, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getRoom } from '../api/roomApi'
import AddToHomeScreenModal from '../components/common/AddToHomeScreenModal'
import Modal from '../components/common/Modal'
import MyPageModal from '../components/common/MyPageModal'
import MyRoomsModal from '../components/common/MyRoomsModal'
import { markAddToHomeScreenPromptSeen, shouldPromptAddToHomeScreen } from '../utils/addToHomeScreen'
import './HomePage.css'

const STEPS = [
  {
    number: 1,
    accent: 'yellow',
    icon: '/home-step-1.png',
    title: '출발역 넣기',
    desc: '친구들이 각자\n출발지를 넣어요.',
  },
  {
    number: 2,
    accent: 'green',
    icon: '/home-step-2.png',
    title: '중간지점 찾기',
    desc: '투표로 장소를 정하고\n가게까지 확정해요.',
  },
  {
    number: 3,
    accent: 'pink',
    icon: '/home-step-3.png',
    title: '약속 확정하기',
    desc: '막차까지 확인하고\n정산까지 한 번에!',
  },
]

// 전체 초대 링크를 붙여넣어도, 방 코드만 입력해도 되게 둘 다 받아줌
function extractRoomCode(input) {
  const trimmed = input.trim()
  const match = trimmed.match(/\/rooms\/([^/?#]+)/)
  return match ? match[1] : trimmed
}

function HomePage() {
  const navigate = useNavigate()
  const [isMyRoomsOpen, setIsMyRoomsOpen] = useState(false)
  const [isMyPageOpen, setIsMyPageOpen] = useState(false)
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false)
  const [inviteInput, setInviteInput] = useState('')
  const [inviteError, setInviteError] = useState(null)
  const [isCheckingInvite, setIsCheckingInvite] = useState(false)
  const [isAddToHomeScreenModalOpen, setIsAddToHomeScreenModalOpen] = useState(false)

  const handleOpenInviteModal = () => {
    setInviteInput('')
    setInviteError(null)
    setIsInviteModalOpen(true)
  }

  // 방을 만들고 난 뒤에 홈 화면에 추가하면 그 방으로 못 돌아오기 때문에, 방 만들기 폼으로 들어가기 전에 먼저 끼워 넣음
  const handleCreateRoomClick = (e) => {
    if (shouldPromptAddToHomeScreen()) {
      e.preventDefault()
      setIsAddToHomeScreenModalOpen(true)
    }
  }

  const handleAddToHomeScreenProceed = () => {
    markAddToHomeScreenPromptSeen()
    setIsAddToHomeScreenModalOpen(false)
    navigate('/rooms/new')
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
        <img src="/pwa-192x192.png" alt="어디서봄" className="home-logo-icon" />
        <div className="home-header-actions">
          <button type="button" className="home-my-rooms-btn" onClick={() => setIsMyRoomsOpen(true)}>
            내 약속
          </button>
          <button type="button" aria-label="마이페이지" className="home-icon-btn" onClick={() => setIsMyPageOpen(true)}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </button>
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

      <div className="home-steps-heading">
        <h2>이렇게 사용해요</h2>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className="home-steps-heading-spark">
          <path d="M13 2 4 14h6l-1 8 9-12h-6l1-8z" />
        </svg>
      </div>

      <div className="home-steps-row">
        {STEPS.map((step, index) => (
          <Fragment key={step.number}>
            <div className={`home-step-card home-step-card--${step.accent}`}>
              <span className="home-step-card-number">{step.number}</span>
              <img src={step.icon} alt="" className="home-step-card-icon" />
              <div className="home-step-card-title">{step.title}</div>
              <div className="home-step-card-desc">{step.desc}</div>
            </div>
            {index < STEPS.length - 1 && (
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="3"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
                className="home-step-arrow"
              >
                <path d="M9 6l6 6-6 6" />
              </svg>
            )}
          </Fragment>
        ))}
      </div>

      <div className="home-cta-group">
        <Link to="/rooms/new" className="home-cta-primary" onClick={handleCreateRoomClick}>
          약속 방 만들기
        </Link>
        <button type="button" className="home-cta-secondary" onClick={handleOpenInviteModal}>
          초대 링크 받았어요
        </button>
      </div>

      {isMyRoomsOpen && <MyRoomsModal onClose={() => setIsMyRoomsOpen(false)} />}

      {isMyPageOpen && <MyPageModal onClose={() => setIsMyPageOpen(false)} />}

      {isAddToHomeScreenModalOpen && (
        <AddToHomeScreenModal
          onProceed={handleAddToHomeScreenProceed}
          onClose={() => setIsAddToHomeScreenModalOpen(false)}
        />
      )}

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
    </div>
  )
}

export default HomePage
