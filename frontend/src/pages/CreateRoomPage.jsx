import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRoom } from '../api/roomApi'
import { getAllStationNames } from '../api/stationApi'
import DateCalendarModal from '../components/common/DateCalendarModal'
import StationAutocompleteInput from '../components/common/StationAutocompleteInput'
import { formatMeetingDate } from '../utils/date'
import { getPublicOrigin } from '../utils/publicUrl'
import './CreateRoomPage.css'

function CreateRoomPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [fairnessType, setFairnessType] = useState('MAX_MIN')
  const [mode, setMode] = useState('MIDPOINT')
  const [fixedStationName, setFixedStationName] = useState('')
  const [includesFood, setIncludesFood] = useState(true)
  const [includesCafe, setIncludesCafe] = useState(true)
  const [meetingDate, setMeetingDate] = useState(null)
  const [isDateModalOpen, setIsDateModalOpen] = useState(false)
  const [stationOptions, setStationOptions] = useState([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState(null)
  const [room, setRoom] = useState(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    getAllStationNames().then(setStationOptions).catch(() => {})
  }, [])

  const trimmedStation = fixedStationName.trim()
  const isFixedStationValid = mode !== 'FIXED' || trimmedStation.length > 0
  const hasCategory = mode === 'SETTLEMENT_ONLY' || includesFood || includesCafe
  const canSubmit = name.trim().length > 0 && isFixedStationValid && hasCategory && !isLoading

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!canSubmit) {
      return
    }

    setIsLoading(true)
    setError(null)
    try {
      const created = await createRoom({
        name: name.trim(),
        mode,
        fairnessType,
        fixedStationName: mode === 'FIXED' ? trimmedStation : undefined,
        includesFood,
        includesCafe,
        meetingDate,
      })
      setRoom(created)
    } catch (error) {
      setError(error.response?.data?.message || (mode === 'FIXED'
        ? '방을 만들지 못했어요. 역 이름을 확인하고 다시 시도해 주세요.'
        : '방을 만들지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setIsLoading(false)
    }
  }

  const shareLink = room ? `${getPublicOrigin()}/rooms/${room.code}` : ''

  const handleShare = async () => {
    if (navigator.share) {
      try {
        await navigator.share({ title: room.name, url: shareLink })
      } catch {
        // 사용자가 공유 시트를 취소한 경우, 별도 처리 없음
      }
      return
    }
    try {
      await navigator.clipboard.writeText(shareLink)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      setCopied(false)
    }
  }

  if (room) {
    return (
      <div className="create-room-page">
        <header className="create-room-header">
          <button type="button" aria-label="뒤로" className="create-room-back-btn" onClick={() => navigate('/')}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M15 5l-7 7 7 7" />
            </svg>
          </button>
        </header>

        <div className="create-room-success-card">
          <div className="create-room-success-title">방이 만들어졌어요!</div>
          <div className="create-room-success-name">{room.name}</div>
          <div className="create-room-link-row">
            <span className="create-room-link-text">{shareLink}</span>
          </div>
          <div className="create-room-success-actions">
            <button type="button" className="create-room-share-btn" onClick={handleShare}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M12 3v12" />
                <path d="M8 7l4-4 4 4" />
                <path d="M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7" />
              </svg>
              {copied ? '복사됨!' : '공유하기'}
            </button>
            <button type="button" className="create-room-enter-btn" onClick={() => navigate(`/rooms/${room.code}`)}>
              방으로 들어가기
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="create-room-page">
      <header className="create-room-header">
        <button type="button" aria-label="뒤로" className="create-room-back-btn" onClick={() => navigate('/')}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M15 5l-7 7 7 7" />
          </svg>
        </button>
      </header>

      <h1 className="create-room-title">{'어떤 약속 방을\n만들까요?'}</h1>

      <form className="create-room-form" onSubmit={handleSubmit}>
        <label htmlFor="create-room-name-input" className="create-room-field-label">
          방 이름
        </label>
        <input
          id="create-room-name-input"
          type="text"
          placeholder="금요일 3인방 모임"
          className="create-room-name-input"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />

        <div className="create-room-field-label">약속 날짜 · 선택</div>
        <button type="button" className="create-room-date-btn" onClick={() => setIsDateModalOpen(true)}>
          <span>{meetingDate ? formatMeetingDate(meetingDate) : '날짜 선택'}</span>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <rect x="3.5" y="5" width="17" height="15" rx="2.5" />
            <path d="M3.5 10h17M8 3v4M16 3v4" />
          </svg>
        </button>

        <div className="create-room-field-label">장소 정하는 방식</div>
        <div className="create-room-mode-stack">
          <button
            type="button"
            aria-pressed={mode === 'MIDPOINT'}
            className="create-room-fairness-btn"
            onClick={() => setMode('MIDPOINT')}
          >
            <span className="create-room-fairness-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 21s-6-5.3-6-11a6 6 0 0 1 12 0c0 5.7-6 11-6 11z" />
                <circle cx="12" cy="10" r="2.2" />
              </svg>
            </span>
            <span className="create-room-fairness-text">
              <span className="create-room-fairness-title-row">
                <span className="create-room-fairness-title">중간지점 자동 계산</span>
                <span className="create-room-fairness-recommend">추천</span>
              </span>
              <span className="create-room-fairness-desc">출발역만 넣으면 공평한 역을 찾아줘요</span>
            </span>
            <span className="create-room-fairness-radio" aria-hidden="true" />
          </button>
          <button
            type="button"
            aria-pressed={mode === 'FIXED'}
            className="create-room-fairness-btn"
            onClick={() => setMode('FIXED')}
          >
            <span className="create-room-fairness-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="7" y="3" width="10" height="18" rx="2.5" />
                <circle cx="12" cy="10" r="1.6" fill="currentColor" stroke="none" />
                <path d="M10 15.5h4" />
              </svg>
            </span>
            <span className="create-room-fairness-text">
              <span className="create-room-fairness-title-row">
                <span className="create-room-fairness-title">만날 역은 이미 정했어요</span>
              </span>
              <span className="create-room-fairness-desc">바로 장소 고르기로 넘어가요</span>
            </span>
            <span className="create-room-fairness-radio" aria-hidden="true" />
          </button>
          <button
            type="button"
            aria-pressed={mode === 'SETTLEMENT_ONLY'}
            className="create-room-fairness-btn"
            onClick={() => setMode('SETTLEMENT_ONLY')}
          >
            <span className="create-room-fairness-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="5" width="18" height="14" rx="3" />
                <path d="M3 10h18M7 15h4" />
              </svg>
            </span>
            <span className="create-room-fairness-text">
              <span className="create-room-fairness-title-row">
                <span className="create-room-fairness-title">정산만 빠르게 하기</span>
              </span>
              <span className="create-room-fairness-desc">쓴 돈만 나눠서 계산해요</span>
            </span>
            <span className="create-room-fairness-radio" aria-hidden="true" />
          </button>
        </div>

        {mode === 'FIXED' && (
          <>
            <label htmlFor="create-room-station-input" className="create-room-field-label">
              역 이름
            </label>
            <StationAutocompleteInput
              id="create-room-station-input"
              placeholder="강남역 또는 초성(ㄱㄴ)"
              value={fixedStationName}
              onChange={setFixedStationName}
              stationOptions={stationOptions}
            />
          </>
        )}

        {/* 일단 무조건 "모두 공평하게"로 고정. "다 같이 빨리"(SUM_MIN) 선택 UI는 나중에 다시 쓸 수 있게 주석으로만 남겨둠
        {mode === 'MIDPOINT' && (
          <>
            <div className="create-room-field-label">정렬 기준</div>
            <div className="create-room-fairness-grid">
              <button
                type="button"
                aria-pressed={fairnessType === 'MAX_MIN'}
                className="create-room-fairness-btn"
                onClick={() => setFairnessType('MAX_MIN')}
              >
                <span className="create-room-fairness-title">모두 공평하게</span>
              </button>
              <button
                type="button"
                aria-pressed={fairnessType === 'SUM_MIN'}
                className="create-room-fairness-btn"
                onClick={() => setFairnessType('SUM_MIN')}
              >
                <span className="create-room-fairness-title">다 같이 빨리</span>
              </button>
            </div>
          </>
        )}
        */}

        {mode !== 'SETTLEMENT_ONLY' && (
          <>
            <div className="create-room-field-label">필요한 카테고리 · 최소 1개</div>
            <div className="create-room-category-row">
              <button
                type="button"
                aria-pressed={includesFood}
                className="create-room-category-chip"
                onClick={() => setIncludesFood((prev) => !prev)}
              >
                {includesFood && (
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M5 12.5l4.5 4.5L19 7" />
                  </svg>
                )}
                음식점
              </button>
              <button
                type="button"
                aria-pressed={includesCafe}
                className="create-room-category-chip"
                onClick={() => setIncludesCafe((prev) => !prev)}
              >
                {includesCafe && (
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                    <path d="M5 12.5l4.5 4.5L19 7" />
                  </svg>
                )}
                카페
              </button>
            </div>
          </>
        )}

        {error && <div className="create-room-error">{error}</div>}

        <button type="submit" className="create-room-submit-cta" disabled={!canSubmit}>
          {isLoading ? '만드는 중...' : '방 만들기'}
        </button>
      </form>

      {isDateModalOpen && (
        <DateCalendarModal
          initialDate={meetingDate}
          onSelect={(dateKey) => {
            setMeetingDate(dateKey)
            setIsDateModalOpen(false)
          }}
          onClear={() => {
            setMeetingDate(null)
            setIsDateModalOpen(false)
          }}
          onClose={() => setIsDateModalOpen(false)}
        />
      )}
    </div>
  )
}

export default CreateRoomPage
