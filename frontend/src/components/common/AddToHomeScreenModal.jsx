import { useState } from 'react'
import Modal from './Modal'
import { isAndroid } from '../../utils/addToHomeScreen'
import { triggerInstallPrompt } from '../../utils/installPromptBus'
import './AddToHomeScreenModal.css'

// 방 만들기 직전에만 띄우는 모달. 안드로이드는 네이티브 설치창을 바로 띄우고,
// iOS 사파리는 설치 API가 없어서 "공유 → 홈 화면에 추가" 안내만 보여줌
// "그냥 만들기"·설치 완료는 onProceed로 방 만들기까지 이어지고, X·바깥 탭(onClose)은 그냥 닫히기만 함
function AddToHomeScreenModal({ onProceed, onClose }) {
  const [showIosInstructions, setShowIosInstructions] = useState(false)

  const handleAndroidInstall = async () => {
    await triggerInstallPrompt()
    onProceed()
  }

  if (showIosInstructions) {
    return (
      <Modal title="홈 화면에 추가하는 법" onClose={onClose}>
        <div className="a2hs-instructions">
          <img src="/ios-share-icon.png" alt="사파리 공유 버튼" className="a2hs-share-icon" />
          <p className="a2hs-instruction-step">① 하단 공유 버튼을 탭하세요</p>
          <p className="a2hs-instruction-step">② '홈 화면에 추가'를 선택하세요</p>
        </div>
        <button type="button" className="a2hs-primary-btn" onClick={() => setShowIosInstructions(false)}>
          확인
        </button>
      </Modal>
    )
  }

  return (
    <Modal title="홈 화면에 추가하고 시작할까요?" onClose={onClose}>
      <p className="a2hs-subtitle">나중에 추가하면 이 모임을 다시 못 가져와요</p>
      {isAndroid() ? (
        <button type="button" className="a2hs-primary-btn" onClick={handleAndroidInstall}>
          추가하기
        </button>
      ) : (
        <button type="button" className="a2hs-primary-btn" onClick={() => setShowIosInstructions(true)}>
          추가하는 법 보기
        </button>
      )}
      <button type="button" className="a2hs-skip-btn" onClick={onProceed}>
        그냥 만들기
      </button>
    </Modal>
  )
}

export default AddToHomeScreenModal
