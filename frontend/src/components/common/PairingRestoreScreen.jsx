import { useState } from 'react'
import { redeemPairingCode } from '../../api/userApi'
import './PairingRestoreScreen.css'

function PairingRestoreScreen({ onRestored, onStartFresh }) {
  const [mode, setMode] = useState('choice') // 'choice' | 'code'
  const [code, setCode] = useState('')
  const [error, setError] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleRedeem = async () => {
    if (code.trim().length !== 4 || isSubmitting) {
      return
    }
    setIsSubmitting(true)
    setError(null)
    try {
      const data = await redeemPairingCode(code.trim())
      onRestored(data.token)
    } catch (err) {
      setError(
        err.response?.status === 429
          ? '너무 많이 시도했어요. 잠시 후 다시 시도해 주세요.'
          : '코드가 올바르지 않거나 만료됐어요.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="pairing-restore-page">
      <span className="home-logo">어디서봄</span>

      <div className="pairing-restore-body">
        <h1 className="pairing-restore-title">처음 실행이네요!</h1>

        {mode === 'choice' && (
          <>
            <p className="home-invite-lead">
              다른 브라우저에서 쓰던 내용을 이어받을까요? 설치 안내에서 받은 4자리 코드가 있으면 입력해 주세요.
            </p>
            <button type="button" className="home-cta-primary" onClick={() => setMode('code')}>
              코드로 이어하기
            </button>
            <button type="button" className="home-cta-secondary" onClick={onStartFresh}>
              새로 시작하기
            </button>
          </>
        )}

        {mode === 'code' && (
          <>
            <input
              type="text"
              inputMode="numeric"
              maxLength={4}
              placeholder="4자리 코드"
              className="home-invite-input pairing-restore-code-input"
              autoFocus
              value={code}
              onChange={(e) => {
                setCode(e.target.value.replace(/\D/g, ''))
                setError(null)
              }}
              onKeyDown={(e) => e.key === 'Enter' && handleRedeem()}
            />
            {error && <div className="home-invite-error">{error}</div>}
            <button
              type="button"
              className="home-invite-submit-btn"
              disabled={code.trim().length !== 4 || isSubmitting}
              onClick={handleRedeem}
            >
              {isSubmitting ? '확인하는 중...' : '이어하기'}
            </button>
            <button type="button" className="home-cta-secondary" onClick={onStartFresh}>
              그냥 새로 시작하기
            </button>
          </>
        )}
      </div>
    </div>
  )
}

export default PairingRestoreScreen
