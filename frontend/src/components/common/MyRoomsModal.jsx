import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyRooms } from '../../api/roomApi'
import { formatMeetingDate } from '../../utils/date'
import Modal from './Modal'
import './MyRoomsModal.css'

const STATUS_LABEL = {
  COLLECTING: '모으는 중',
  VOTING: '투표 중',
  CONFIRMED: '확정',
  CLOSED: '종료',
}

// "10월 17" 처럼 날짜 배지에 쓸 월·일만 뽑아냄 (요일·연도는 메타 줄에 이미 나옴)
function splitMonthDay(meetingDate) {
  if (!meetingDate) {
    return null
  }
  const [, month, day] = meetingDate.split('-').map(Number)
  return { month, day }
}

function MyRoomsModal({ onClose }) {
  const navigate = useNavigate()
  const [rooms, setRooms] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    const fetchRooms = () => {
      getMyRooms()
        .then(setRooms)
        .catch(() => setError('약속 목록을 불러오지 못했어요.'))
    }
    fetchRooms()

    // Safari 뒤로가기는 bfcache로 페이지를 그대로 복원해 마운트가 다시 안 일어남 - 그새 참여한 방이 빠진 옛 목록이 남아있어서, 다시 보이는 순간 재조회
    const handleVisible = () => {
      if (document.visibilityState === 'visible') {
        fetchRooms()
      }
    }
    document.addEventListener('visibilitychange', handleVisible)
    return () => document.removeEventListener('visibilitychange', handleVisible)
  }, [])

  const handleSelect = (code) => {
    onClose()
    navigate(`/rooms/${code}`)
  }

  return (
    <Modal title="내 약속" onClose={onClose}>
      {error && <p className="my-rooms-status">{error}</p>}
      {!error && rooms === null && <p className="my-rooms-status">불러오는 중...</p>}
      {!error && rooms !== null && rooms.length === 0 && (
        <p className="my-rooms-status">아직 참여한 약속이 없어요.</p>
      )}

      {!error && rooms !== null && rooms.length > 0 && (
        <ul className="my-rooms-list">
          {rooms.map((room) => {
            const monthDay = splitMonthDay(room.meetingDate)
            return (
              <li key={room.code}>
                <button type="button" className="my-rooms-item" onClick={() => handleSelect(room.code)}>
                  {room.status === 'CONFIRMED' ? (
                    <span className="my-rooms-item-badge my-rooms-item-badge--done" aria-hidden="true">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M5 12.5l4.5 4.5L19 7" />
                      </svg>
                    </span>
                  ) : monthDay ? (
                    <span className="my-rooms-item-badge my-rooms-item-badge--date" aria-hidden="true">
                      <span className="my-rooms-item-badge-month">{monthDay.month}월</span>
                      <span className="my-rooms-item-badge-day">{monthDay.day}</span>
                    </span>
                  ) : (
                    <span className="my-rooms-item-badge my-rooms-item-badge--undated" aria-hidden="true">미정</span>
                  )}

                  <span className="my-rooms-item-body">
                    <span className="my-rooms-item-top">
                      <span className="my-rooms-item-name">{room.name}</span>
                      {room.owner && <span className="my-rooms-owner-badge">방장</span>}
                      {room.status === 'CONFIRMED' && <span className="my-rooms-confirmed-badge">확정</span>}
                    </span>
                    <span className="my-rooms-item-meta">
                      <span className={`my-rooms-item-status my-rooms-item-status--${room.status.toLowerCase()}`}>
                        {STATUS_LABEL[room.status]}
                      </span>
                      <span>·</span>
                      <span>참여자 {room.participantCount}명</span>
                      {room.meetingDate && (
                        <>
                          <span>·</span>
                          <span>{formatMeetingDate(room.meetingDate)}</span>
                        </>
                      )}
                    </span>
                  </span>
                  <svg className="my-rooms-item-chevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                    <path d="M9 6l6 6-6 6" />
                  </svg>
                </button>
              </li>
            )
          })}
        </ul>
      )}

      {!error && rooms !== null && (
        <button type="button" className="my-rooms-new-btn" onClick={() => { onClose(); navigate('/rooms/new') }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          새 약속
        </button>
      )}
    </Modal>
  )
}

export default MyRoomsModal
