import { useEffect } from 'react'
import './Modal.css'

function Modal({ title, subtitle, icon, variant = 'center', onClose, children }) {
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  const isSheet = variant === 'sheet'

  return (
    <div className={`modal-backdrop${isSheet ? ' modal-backdrop--sheet' : ''}`} onClick={onClose}>
      <div className={`modal-panel${isSheet ? ' modal-panel--sheet' : ''}`} onClick={(e) => e.stopPropagation()}>
        {isSheet && <div className="modal-sheet-handle" aria-hidden="true" />}
        <div className="modal-header">
          {icon ? (
            <>
              <span className="modal-header-icon">{icon}</span>
              <div className="modal-header-text">
                <h2 className="modal-title">{title}</h2>
                {subtitle && <span className="modal-subtitle">{subtitle}</span>}
              </div>
            </>
          ) : (
            <h2 className="modal-title">{title}</h2>
          )}
          <button type="button" className="modal-close-btn" aria-label="닫기" onClick={onClose}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M6 6l12 12M18 6L6 18" />
            </svg>
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

export default Modal
