const WEEKDAY_LABELS = ['일', '월', '화', '수', '목', '금', '토']

// 'YYYY-MM-DD' 날짜 문자열을 "2026년 11월 3일 (화)" 형태로 변환. 값이 없으면 null
export function formatMeetingDate(dateStr) {
  if (!dateStr) {
    return null
  }
  const [year, month, day] = dateStr.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return `${year}년 ${month}월 ${day}일 (${WEEKDAY_LABELS[date.getDay()]})`
}
