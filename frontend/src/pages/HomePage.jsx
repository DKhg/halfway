import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getRoom } from '../api/roomApi'
import Modal from '../components/common/Modal'
import MyRoomsModal from '../components/common/MyRoomsModal'
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
  const [isMyRoomsOpen, setIsMyRoomsOpen] = useState(false)
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false)
  const [inviteInput, setInviteInput] = useState('')
  const [inviteError, setInviteError] = useState(null)
  const [isCheckingInvite, setIsCheckingInvite] = useState(false)

  const handleOpenInviteModal = () => {
    setInviteInput('')
    setInviteError(null)
    setIsInviteModalOpen(true)
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
        <button type="button" className="home-my-rooms-btn" onClick={() => setIsMyRoomsOpen(true)}>
          내 약속
        </button>
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
