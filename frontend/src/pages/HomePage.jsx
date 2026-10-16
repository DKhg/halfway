import { useState } from 'react'
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
    title: '출발역 넣기',
    desc: '각자 출발지를 넣어요',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <rect x="7" y="3" width="10" height="18" rx="2.5" />
        <circle cx="12" cy="10" r="1.6" fill="currentColor" stroke="none" />
        <path d="M10 15.5h4" />
      </svg>
    ),
  },
  {
    number: 2,
    accent: 'primary-soft',
    title: '중간지점 찾기',
    desc: '투표로 장소를 정해요',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 21s7-7.1 7-12a7 7 0 0 0-14 0c0 4.9 7 12 7 12z" />
        <circle cx="12" cy="9" r="2.5" />
      </svg>
    ),
  },
  {
    number: 3,
    accent: 'primary',
    title: '약속 확정하기',
    desc: '막차·정산까지 한 번에',
    icon: (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M4 11 12 4l8 7" />
        <path d="M5.5 10v10h13V10" />
        <path d="M10 20v-6h4v6" />
      </svg>
    ),
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
        <span className="home-logo">
          어디서<span className="home-logo-accent">봄?</span>
        </span>
        <div className="home-header-actions">
          <button type="button" className="home-my-rooms-btn" onClick={() => setIsMyRoomsOpen(true)}>
            내 약속
          </button>
          <button type="button" aria-label="마이페이지" className="home-icon-btn" onClick={() => setIsMyPageOpen(true)}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6" />
            </svg>
          </button>
        </div>
      </header>

      <div className="home-hero">
        <span className="home-badge">
          <span className="home-badge-dot" />
          링크 한 방이면 끝
        </span>
        <h1>
          어디서 볼래?
          <br />
          이제 <span className="home-hero-highlight">딱 중간</span>에서.
        </h1>
        <p className="home-lead">
          친구들이 각자 출발지를 넣으면 모두에게 공평한 곳을 골라줘요. 단톡방 논쟁은 이제 그만.
        </p>
      </div>

      <div className="home-steps-section">
        {STEPS.map((step, index) => (
          <div className={`home-step-row${index < STEPS.length - 1 ? ' home-step-row--divided' : ''}`} key={step.number}>
            <div className={`home-step-icon home-step-icon--${step.accent}`}>{step.icon}</div>
            <div>
              <div className="home-step-title">{step.title}</div>
              <div className="home-step-desc">{step.desc}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="home-cta-group">
        <Link to="/rooms/new" className="home-cta-primary" onClick={handleCreateRoomClick}>
          약속 방 만들기
        </Link>
        <button type="button" className="home-cta-secondary" onClick={handleOpenInviteModal}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
          </svg>
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
