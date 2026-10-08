import { useEffect, useRef, useState } from 'react'
import './BankSelect.css'

function BankSelect({ value, onChange, options }) {
  const [isOpen, setIsOpen] = useState(false)
  const wrapperRef = useRef(null)

  // 드롭다운 바깥을 클릭하면 닫음
  useEffect(() => {
    if (!isOpen) {
      return undefined
    }
    const handleOutsideClick = (e) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleOutsideClick)
    return () => document.removeEventListener('mousedown', handleOutsideClick)
  }, [isOpen])

  return (
    <div className="bank-select" ref={wrapperRef}>
      <button
        type="button"
        className="bank-select-trigger"
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <span className={value ? '' : 'bank-select-placeholder'}>{value || '은행 선택'}</span>
        <span className="bank-select-arrow" aria-hidden="true">{isOpen ? '▲' : '▼'}</span>
      </button>

      {isOpen && (
        <ul className="bank-select-list">
          {options.map((bank) => (
            <li key={bank}>
              <button
                type="button"
                className="bank-select-option-btn"
                onClick={() => {
                  onChange(bank)
                  setIsOpen(false)
                }}
              >
                {bank}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default BankSelect
