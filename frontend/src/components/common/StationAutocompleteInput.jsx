import { useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { matchesStationQuery } from '../../utils/hangul'
import { getLineColor, getLineLabel } from '../../utils/subwayLine'
import './StationAutocompleteInput.css'

function StationAutocompleteInput({ id, value, onChange, stationOptions, placeholder }) {
  const [isOpen, setIsOpen] = useState(false)
  const [dropdownRect, setDropdownRect] = useState(null)
  const wrapperRef = useRef(null)

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
  const openDropdown = () => {
    setIsOpen(true)
    if (wrapperRef.current) {
      const rect = wrapperRef.current.getBoundingClientRect()
      setDropdownRect({ top: rect.bottom + 6, left: rect.left, width: rect.width })
    }
  }

  return (
    <div className="station-autocomplete" ref={wrapperRef}>
      <input
        id={id}
        type="text"
        placeholder={placeholder}
        className="station-autocomplete-input"
        autoComplete="off"
        value={value}
        onChange={(e) => {
          onChange(e.target.value)
          openDropdown()
        }}
        onFocus={openDropdown}
        onBlur={() => setTimeout(() => setIsOpen(false), 150)}
      />
      {isOpen && suggestions.length > 0 && dropdownRect && createPortal(
        <ul
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
