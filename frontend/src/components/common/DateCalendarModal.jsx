import { useState } from 'react'
import Modal from './Modal'
import './DateCalendarModal.css'

const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']

function toDateKey(year, month, day) {
  const mm = String(month + 1).padStart(2, '0')
  const dd = String(day).padStart(2, '0')
  return `${year}-${mm}-${dd}`
}

function DateCalendarModal({ initialDate, onSelect, onClear, onClose }) {
  const today = new Date()
  const base = initialDate ? new Date(initialDate) : today
  const [viewYear, setViewYear] = useState(base.getFullYear())
  const [viewMonth, setViewMonth] = useState(base.getMonth())
  const [openPicker, setOpenPicker] = useState(null) // 'year' | 'month' | null

  const firstWeekday = new Date(viewYear, viewMonth, 1).getDay()
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  const todayKey = toDateKey(today.getFullYear(), today.getMonth(), today.getDate())

  // 선택 가능 연도 범위: 오늘 기준 -1년~+5년, 이미 정해둔 날짜가 그 밖이면 포함되도록 넓힘
  const yearRangeStart = Math.min(today.getFullYear() - 1, base.getFullYear())
  const yearRangeEnd = Math.max(today.getFullYear() + 5, base.getFullYear())
  const yearOptions = []
  for (let y = yearRangeStart; y <= yearRangeEnd; y++) {
    yearOptions.push(y)
  }

  const cells = Array(firstWeekday).fill(null).concat(
    Array.from({ length: daysInMonth }, (_, i) => i + 1),
  )

  const goPrevMonth = () => {
    if (viewMonth === 0) {
      setViewYear((y) => y - 1)
      setViewMonth(11)
    } else {
      setViewMonth((m) => m - 1)
    }
  }

  const goNextMonth = () => {
    if (viewMonth === 11) {
      setViewYear((y) => y + 1)
      setViewMonth(0)
    } else {
      setViewMonth((m) => m + 1)
    }
  }

  const togglePicker = (picker) => {
    setOpenPicker((current) => (current === picker ? null : picker))
  }

  const selectYear = (y) => {
    setViewYear(y)
    setOpenPicker(null)
  }

  const selectMonth = (m) => {
    setViewMonth(m)
    setOpenPicker(null)
  }

  return (
    <Modal title="약속 날짜 선택" onClose={onClose}>
      <div className="date-calendar">
        <div className="date-calendar-nav">
          <button type="button" className="date-calendar-nav-btn" aria-label="이전 달" onClick={goPrevMonth}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M15 5l-7 7 7 7" />
            </svg>
          </button>
          <div className="date-calendar-label">
            <div className="date-calendar-dropdown">
              <button type="button" className="date-calendar-label-btn" onClick={() => togglePicker('year')}>
                {viewYear}년
              </button>
              {openPicker === 'year' && (
                <div className="date-calendar-dropdown-panel">
                  {yearOptions.map((y) => (
                    <button
                      key={y}
                      type="button"
                      className={'date-calendar-dropdown-item' + (y === viewYear ? ' date-calendar-dropdown-item--active' : '')}
                      onClick={() => selectYear(y)}
                    >
                      {y}년
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="date-calendar-dropdown">
              <button type="button" className="date-calendar-label-btn" onClick={() => togglePicker('month')}>
                {viewMonth + 1}월
              </button>
              {openPicker === 'month' && (
                <div className="date-calendar-dropdown-panel date-calendar-dropdown-panel--month">
                  {Array.from({ length: 12 }, (_, m) => (
                    <button
                      key={m}
                      type="button"
                      className={'date-calendar-dropdown-item' + (m === viewMonth ? ' date-calendar-dropdown-item--active' : '')}
                      onClick={() => selectMonth(m)}
                    >
                      {m + 1}월
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
          <button type="button" className="date-calendar-nav-btn" aria-label="다음 달" onClick={goNextMonth}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>

        <div className="date-calendar-weekdays">
          {WEEKDAY_LABELS.map((label) => (
            <span key={label} className="date-calendar-weekday">{label}</span>
          ))}
        </div>

        <div className="date-calendar-grid">
          {cells.map((day, idx) => {
            if (day === null) {
              return <span key={`empty-${idx}`} className="date-calendar-cell date-calendar-cell--empty" />
            }
            const key = toDateKey(viewYear, viewMonth, day)
            const isToday = key === todayKey
            const isSelected = key === initialDate
            return (
              <button
                key={key}
                type="button"
                className={
                  'date-calendar-cell' +
                  (isSelected ? ' date-calendar-cell--selected' : '') +
                  (isToday ? ' date-calendar-cell--today' : '')
                }
                onClick={() => onSelect(key)}
              >
                {day}
              </button>
            )
          })}
        </div>

        {initialDate && (
          <button type="button" className="date-calendar-clear-btn" onClick={onClear}>
            날짜 미정으로 두기
          </button>
        )}
      </div>
    </Modal>
  )
}

export default DateCalendarModal
