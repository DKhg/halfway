import { useEffect, useState } from 'react'
import { issuePairingCode } from '../../api/userApi'
import Modal from './Modal'
import './IosInstallModal.css'

function IosInstallModal({ onClose }) {
  const [code, setCode] = useState(null)
  const [error, setError] = useState(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    issuePairingCode()
      .then((data) => setCode(data.code))
      .catch(() => setError('코드를 발급하지 못했어요. 잠시 후 다시 시도해 주세요.'))
  }, [])

  const handleCopy = async () => {
    if (!code) {
      return
    }
    await navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <Modal title="홈 화면에 추가" onClose={onClose}>
      <p className="home-invite-lead">
        하단의 공유 아이콘(
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2"
          strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="ios-install-share-icon">
          <path d="M12 16V4M7 8l5-5 5 5M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6" />
        </svg>
        )을 누르세요. &apos;홈 화면에 추가&apos;가 바로 보이면 선택하고, 안 보이면 &apos;더보기&apos;를 눌러서 찾아 선택하세요.
      </p>

      <div className="ios-install-code-box">
        <span className="ios-install-code-label">이어하기 코드 (15분간 유효)</span>
        <span className="ios-install-code-value">{code ?? '----'}</span>
        <button type="button" className="home-invite-submit-btn" disabled={!code} onClick={handleCopy}>
          {copied ? '복사됨!' : '코드 복사'}
        </button>
      </div>

      {error && <div className="home-invite-error">{error}</div>}

      <p className="home-invite-lead">
        홈 화면에 추가한 뒤 앱을 처음 열면 이 코드를 입력해서, 지금 쓰던 내용(참여한 방·귀가역)을 그대로 이어갈 수 있어요.
      </p>
    </Modal>
  )
}

export default IosInstallModal
