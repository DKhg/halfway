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
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <rect x="6" y="3" width="12" height="15" rx="3" />
        <path d="M9 21l1.5-3M15 21l-1.5-3M6 12h12" />
        <circle cx="9.5" cy="15" r="0.6" fill="currentColor" stroke="none" />
        <circle cx="14.5" cy="15" r="0.6" fill="currentColor" stroke="none" />
      </svg>
    ),
  },
  {
    number: 2,
    accent: 'primary-soft',
    title: '중간지점 찾기',
    desc: '투표로 장소를 정해요',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M12 21s-6-5.3-6-11a6 6 0 0 1 12 0c0 5.7-6 11-6 11z" />
        <circle cx="12" cy="10" r="2.2" />
      </svg>
    ),
  },
  {
    number: 3,
    accent: 'primary',
    title: '약속 확정하기',
    desc: '막차·정산까지 한 번에',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M5 12.5l4.5 4.5L19 7.5" />
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
        <div className="home-logo-group">
          <div className="home-logo-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M12 21s-6-5.3-6-11a6 6 0 0 1 12 0c0 5.7-6 11-6 11z" />
              <circle cx="12" cy="10" r="2.2" />
            </svg>
          </div>
          <span className="home-logo">
            어디서<span className="home-logo-accent">봄?</span>
          </span>
        </div>
        <div className="home-header-actions">
          <button type="button" className="home-my-rooms-btn" onClick={() => setIsMyRoomsOpen(true)}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <rect x="3.5" y="5" width="17" height="15" rx="3" />
              <path d="M3.5 10h17M8 3v4M16 3v4" />
            </svg>
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
        <div className="home-illustration" role="img" aria-label="세 친구의 출발지에서 가운데 지점으로 모이는 지도 그림">
          <svg width="100%" height="100%" viewBox="0 0 350 232" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
            <g className="home-illustration-road home-illustration-road--thick" strokeWidth="10" strokeLinecap="round" fill="none">
              <path d="M-10 70 H360" />
              <path d="M-10 160 C80 150 140 175 360 150" />
              <path d="M110 -10 V250" />
              <path d="M250 -10 C240 80 262 150 245 250" />
            </g>
            <g className="home-illustration-road home-illustration-road--thin" strokeWidth="5" strokeLinecap="round" fill="none">
              <path d="M-10 115 H360" />
              <path d="M40 -10 V250" />
              <path d="M315 -10 V250" />
              <path d="M180 -10 V40" />
              <path d="M180 190 V250" />
            </g>
            <g className="home-illustration-connector" strokeWidth="2.5" strokeDasharray="2 7" strokeLinecap="round" fill="none">
              <path d="M58 52 C95 60 140 85 172 112" />
              <path d="M298 64 C260 70 215 92 188 112" />
              <path d="M78 192 C110 170 150 150 174 128" />
            </g>
            <circle className="home-illustration-glow home-illustration-glow--outer" cx="180" cy="122" r="38" />
            <circle className="home-illustration-glow home-illustration-glow--inner" cx="180" cy="122" r="24" />
            <g>
              <path className="home-illustration-pin" d="M180 128 c-12-13-18-21-18-30 a18 18 0 0 1 36 0 c0 9-6 17-18 30z" />
              <circle className="home-illustration-pin-dot" cx="180" cy="98" r="6.5" />
              <ellipse className="home-illustration-pin-shadow" cx="180" cy="131" rx="7" ry="2.5" />
            </g>
            <g>
              <rect className="home-illustration-chip" x="196" y="58" width="78" height="28" rx="14" />
              <text className="home-illustration-chip-text" x="235" y="77" textAnchor="middle" fontSize="12.5" fontWeight="700">여기서 봄!</text>
            </g>
            <g fontSize="14" fontWeight="800" textAnchor="middle">
              <circle className="home-illustration-avatar-ring" cx="50" cy="48" r="21" />
              <circle className="home-illustration-avatar-bg home-illustration-avatar-bg--1" cx="50" cy="48" r="17" />
              <text className="home-illustration-avatar-text home-illustration-avatar-text--1" x="50" y="53">나</text>
              <circle className="home-illustration-avatar-ring" cx="304" cy="60" r="21" />
              <circle className="home-illustration-avatar-bg home-illustration-avatar-bg--2" cx="304" cy="60" r="17" />
              <text className="home-illustration-avatar-text home-illustration-avatar-text--2" x="304" y="65">민</text>
              <circle className="home-illustration-avatar-ring" cx="70" cy="196" r="21" />
              <circle className="home-illustration-avatar-bg home-illustration-avatar-bg--3" cx="70" cy="196" r="17" />
              <text className="home-illustration-avatar-text home-illustration-avatar-text--3" x="70" y="201">지</text>
            </g>
          </svg>
          <div className="home-illustration-badge">
            <span className="home-illustration-badge-dot" />
            링크 한 방이면 끝
          </div>
        </div>
        <div className="home-hero-text">
          <h1>
            어디서 볼래?
            <br />
            이제 <span className="home-hero-highlight">딱 중간</span>에서.
          </h1>
          <p className="home-lead">
            출발지만 넣으면 모두에게 공평한 곳을 골라줘요.
            <br />
            단톡방 논쟁은 이제 그만.
          </p>
        </div>
      </div>

      <div className="home-steps-grid">
        {STEPS.map((step) => (
          <div className="home-step-card" key={step.number}>
            <div className={`home-step-icon home-step-icon--${step.accent}`}>{step.icon}</div>
            <div className="home-step-card-text">
              <span className="home-step-label">STEP {step.number}</span>
              <span className="home-step-title">{step.title}</span>
              <span className="home-step-desc">{step.desc}</span>
            </div>
          </div>
        ))}
      </div>

      <div className="home-cta-group">
        <Link to="/rooms/new" className="home-cta-primary" onClick={handleCreateRoomClick}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
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
