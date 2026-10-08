import { useEffect } from 'react'
import './ConfirmModal.css'

function ConfirmModal({ title, message, confirmLabel = '확인', cancelLabel, danger, onConfirm, onCancel }) {
  const handleDismiss = onCancel ?? onConfirm

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        handleDismiss()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [handleDismiss])

  return (
    <div className="confirm-modal-backdrop" onClick={handleDismiss}>
      <div className="confirm-modal-panel" onClick={(e) => e.stopPropagation()}>
        {title && <div className="confirm-modal-title">{title}</div>}
        <p className="confirm-modal-message">{message}</p>

        <div className="confirm-modal-actions">
          {cancelLabel && (
            <button type="button" className="confirm-modal-cancel-btn" onClick={onCancel}>
              {cancelLabel}
            </button>
          )}
          <button
            type="button"
            className={danger ? 'confirm-modal-confirm-btn confirm-modal-confirm-btn--danger' : 'confirm-modal-confirm-btn'}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ConfirmModal
