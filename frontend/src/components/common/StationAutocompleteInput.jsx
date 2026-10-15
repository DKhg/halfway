import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { matchesStationQuery } from '../../utils/hangul'
import { getLineColor, getLineLabel } from '../../utils/subwayLine'
import './StationAutocompleteInput.css'

function StationAutocompleteInput({ id, value, onChange, stationOptions, placeholder, disabled }) {
  const [isOpen, setIsOpen] = useState(false)
  const [dropdownRect, setDropdownRect] = useState(null)
  const wrapperRef = useRef(null)
  const dropdownRef = useRef(null)

  const trimmed = value.trim()
  const suggestions = trimmed
    ? stationOptions.filter((option) => matchesStationQuery(option.name, trimmed)).slice(0, 10)
    : []

  const renderLineBadges = (name) => {
    const lines = stationOptions.find((option) => option.name === name)?.lines ?? []
    const deduped = [...new Map(lines.map((line) => [getLineLabel(line), line])).entries()]
    return deduped.map(([label, line]) => (
      <span key={label} className="station-autocomplete-badge" style={{ backgroundColor: getLineColor(line) }}>
        {label}
      </span>
    ))
  }

  // 역 검색창이 모달(overflow:auto) 안에 있으면 드롭다운이 잘려서 document.body에 직접 그리고 위치만 계산
  // getBoundingClientRect는 visualViewport(화면에 실제 보이는 영역) 기준인데, position:fixed는
  // layoutViewport 기준이라 모바일에서 키보드 뜬 동안 둘이 어긋날 수 있어 그 차이(offset)를 보정
  const updateDropdownRect = () => {
    if (!wrapperRef.current) {
      return
    }
    const rect = wrapperRef.current.getBoundingClientRect()
    const visualViewport = window.visualViewport
    const offsetLeft = visualViewport?.offsetLeft ?? 0
    const offsetTop = visualViewport?.offsetTop ?? 0
    setDropdownRect({
      top: rect.bottom + offsetTop + 6,
      left: rect.left + offsetLeft,
      width: rect.width,
    })
  }

  const openDropdown = () => {
    setIsOpen(true)
    updateDropdownRect()
  }

  // 모바일에서 포커스 직후 키보드가 올라오며 화면이 자동 스크롤되면 입력칸 위치가 바뀌는데,
  // 열려있는 동안 스크롤·리사이즈를 다시 들어서 위치를 따라가게 함 (안 그러면 포커스 시점 위치에 고정돼버림)
  useEffect(() => {
    if (!isOpen) {
      return
    }
    // 드롭다운 목록 자체를 스크롤하는 것도 capture 단계에선 window까지 올라와서 잡힘 -
    // 그건 입력칸 위치가 안 바뀐 거니까 무시하고, 그 외(페이지·모달 스크롤)만 위치 재계산
    const handleScroll = (e) => {
      if (dropdownRef.current && dropdownRef.current.contains(e.target)) {
        return
      }
      updateDropdownRect()
    }
    window.addEventListener('scroll', handleScroll, true)
    window.addEventListener('resize', updateDropdownRect)

    // 모바일 키보드가 레이아웃을 줄이지 않고 화면 위에 덮어씌우기만 하는 경우(iOS Safari 등),
    // window.innerHeight가 안 바뀌어서 위 리스너가 전혀 안 울림 - 이 경우엔 visualViewport만 반응함
    const visualViewport = window.visualViewport
    visualViewport?.addEventListener('resize', updateDropdownRect)
    visualViewport?.addEventListener('scroll', updateDropdownRect)

    return () => {
      window.removeEventListener('scroll', handleScroll, true)
      window.removeEventListener('resize', updateDropdownRect)
      visualViewport?.removeEventListener('resize', updateDropdownRect)
      visualViewport?.removeEventListener('scroll', updateDropdownRect)
    }
  }, [isOpen])

  return (
    <div className="station-autocomplete" ref={wrapperRef}>
      <input
        id={id}
        type="text"
        placeholder={placeholder}
        className="station-autocomplete-input"
        autoComplete="off"
        value={value}
        disabled={disabled}
        onChange={(e) => {
          onChange(e.target.value)
          openDropdown()
        }}
        onFocus={openDropdown}
        onBlur={() => setTimeout(() => setIsOpen(false), 150)}
      />
      {isOpen && suggestions.length > 0 && dropdownRect && createPortal(
        <ul
          ref={dropdownRef}
          className="station-autocomplete-suggestions station-autocomplete-suggestions--portal"
          style={{ top: dropdownRect.top, left: dropdownRect.left, width: dropdownRect.width }}
        >
          {suggestions.map((option) => (
            <li key={option.name}>
              <button
                type="button"
                className="station-autocomplete-suggestion-btn"
                onMouseDown={() => {
                  onChange(option.name)
                  setIsOpen(false)
                }}
              >
                <span>{option.name}</span>
                <span className="station-autocomplete-suggestion-lines">{renderLineBadges(option.name)}</span>
              </button>
            </li>
          ))}
        </ul>,
        document.body,
      )}
    </div>
  )
}

export default StationAutocompleteInput
