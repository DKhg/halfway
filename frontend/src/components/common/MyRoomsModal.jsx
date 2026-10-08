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

function MyRoomsModal({ onClose }) {
  const navigate = useNavigate()
  const [rooms, setRooms] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    getMyRooms()
      .then(setRooms)
      .catch(() => setError('약속 목록을 불러오지 못했어요.'))
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
          {rooms.map((room) => (
            <li key={room.code}>
              <button type="button" className="my-rooms-item" onClick={() => handleSelect(room.code)}>
                <div className="my-rooms-item-top">
                  <span className="my-rooms-item-name">{room.name}</span>
                  {room.owner && <span className="my-rooms-owner-badge">방장</span>}
                </div>
                <div className="my-rooms-item-meta">
                  <span>{STATUS_LABEL[room.status]}</span>
                  <span>참여자 {room.participantCount}명</span>
                  <span>{formatMeetingDate(room.meetingDate) ?? '날짜 미정'}</span>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}
    </Modal>
  )
}

export default MyRoomsModal
