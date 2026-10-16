import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMe, updateHomeStation, updateNickname, deleteMyAccount } from '../../api/userApi'
import { getAllStationNames } from '../../api/stationApi'
import { clearToken } from '../../utils/authToken'
import ConfirmModal from './ConfirmModal'
import Modal from './Modal'
import StationAutocompleteInput from './StationAutocompleteInput'
import './MyPageModal.css'

function MyPageModal({ onClose }) {
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState(null)

  const [nickname, setNickname] = useState('')
  const [isSavingNickname, setIsSavingNickname] = useState(false)
  const [nicknameSaved, setNicknameSaved] = useState(false)
  const [nicknameError, setNicknameError] = useState(null)

  const [homeStation, setHomeStation] = useState('')
  const [stationOptions, setStationOptions] = useState([])
  const [isSavingHomeStation, setIsSavingHomeStation] = useState(false)
  const [homeStationSaved, setHomeStationSaved] = useState(false)
  const [homeStationError, setHomeStationError] = useState(null)

  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [deleteError, setDeleteError] = useState(null)

  useEffect(() => {
    Promise.all([getMe(), getAllStationNames()])
      .then(([me, stations]) => {
        setNickname(me.nickname || '')
        setHomeStation(me.homeStationName || '')
        setStationOptions(stations)
      })
      .catch(() => setLoadError('내 정보를 불러오지 못했어요.'))
      .finally(() => setIsLoading(false))
  }, [])

  const handleSaveNickname = async () => {
    const trimmed = nickname.trim()
    if (!trimmed || isSavingNickname) {
      return
    }
    setIsSavingNickname(true)
    setNicknameError(null)
    try {
      await updateNickname(trimmed)
      setNicknameSaved(true)
      setTimeout(() => setNicknameSaved(false), 2000)
    } catch (error) {
      setNicknameError(error.response?.data?.message || '저장하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsSavingNickname(false)
    }
  }

  const handleSaveHomeStation = async () => {
    if (isSavingHomeStation) {
      return
    }
    setIsSavingHomeStation(true)
    setHomeStationError(null)
    try {
      await updateHomeStation(homeStation.trim())
      setHomeStationSaved(true)
      setTimeout(() => setHomeStationSaved(false), 2000)
    } catch (error) {
      setHomeStationError(
        error.response?.status === 400 ? '등록되지 않은 역 이름이에요.' : '저장하지 못했어요. 다시 시도해 주세요.',
      )
    } finally {
      setIsSavingHomeStation(false)
    }
  }

  // 계정 삭제 후에는 리액트 상태를 정리하는 대신 페이지 자체를 새로고침해서, 완전히 새 익명 계정으로 시작하게 함
  const handleConfirmDelete = async () => {
    if (isDeleting) {
      return
    }
    setIsDeleteConfirmOpen(false)
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await deleteMyAccount()
      clearToken()
      window.location.href = '/'
    } catch {
      setDeleteError('삭제하지 못했어요. 다시 시도해 주세요.')
      setIsDeleting(false)
    }
  }

  return (
    <Modal title="마이페이지" onClose={onClose}>
      {isLoading && <p className="my-page-status">불러오는 중...</p>}
      {loadError && <p className="my-page-status">{loadError}</p>}

      {!isLoading && !loadError && (
        <>
          <div className="my-page-profile-card">
            <div className="my-page-avatar">{nickname.trim().charAt(0) || '?'}</div>
            <div className="my-page-profile-text">
              <div className="my-page-profile-name">{nickname.trim() || '닉네임 미설정'}</div>
            </div>
          </div>

          <div className="my-page-section-label">자동 입력</div>
          <div className="my-page-list">
            <div className="my-page-list-row my-page-list-row--editable">
              <span className="my-page-list-row-icon my-page-list-row-icon--primary">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6" />
                </svg>
              </span>
              <div className="my-page-list-row-text">
                <span className="my-page-list-row-title">기본 닉네임</span>
                <span className="my-page-list-row-desc">방에 참여할 때 자동으로 채워져요</span>
                <div className="my-page-field-row">
                  <input
                    id="my-page-nickname-input"
                    type="text"
                    placeholder="닉네임"
                    className="my-page-input"
                    maxLength={20}
                    value={nickname}
                    onChange={(e) => {
                      setNickname(e.target.value)
                      setNicknameError(null)
                    }}
                  />
                  <button
                    type="button"
                    className="my-page-save-btn"
                    disabled={!nickname.trim() || isSavingNickname}
                    onClick={handleSaveNickname}
                  >
                    {isSavingNickname ? '저장 중' : nicknameSaved ? '저장됨' : '저장'}
                  </button>
                </div>
                {nicknameError && <div className="my-page-error">{nicknameError}</div>}
              </div>
            </div>

            <div className="my-page-list-divider" />

            <div className="my-page-list-row my-page-list-row--editable">
              <span className="my-page-list-row-icon my-page-list-row-icon--yellow">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <path d="M4 11 12 4l8 7" />
                  <path d="M5.5 10v10h13V10" />
                </svg>
              </span>
              <div className="my-page-list-row-text">
                <span className="my-page-list-row-title">귀가역</span>
                <span className="my-page-list-row-desc">막차·귀가 고려 계산에 쓰여요</span>
                <div className="my-page-field-row">
                  <StationAutocompleteInput
                    id="my-page-home-station-input"
                    placeholder="귀가역 또는 초성(ㄱㄴ)"
                    value={homeStation}
                    onChange={(value) => {
                      setHomeStation(value)
                      setHomeStationError(null)
                    }}
                    stationOptions={stationOptions}
                  />
                  <button
                    type="button"
                    className="my-page-save-btn"
                    disabled={isSavingHomeStation}
                    onClick={handleSaveHomeStation}
                  >
                    {isSavingHomeStation ? '저장 중' : homeStationSaved ? '저장됨' : '저장'}
                  </button>
                </div>
                {homeStationError && <div className="my-page-error">{homeStationError}</div>}
              </div>
            </div>
          </div>

          <div className="my-page-section-label">정보</div>
          <div className="my-page-list">
            <Link to="/privacy" className="my-page-list-row" onClick={onClose}>
              <span className="my-page-list-row-text">
                <span className="my-page-list-row-title">개인정보처리방침</span>
              </span>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                <path d="M9 6l6 6-6 6" />
              </svg>
            </Link>
          </div>

          <div className="my-page-danger-section">
            {deleteError && <div className="my-page-error">{deleteError}</div>}
            <button
              type="button"
              className="my-page-delete-btn"
              disabled={isDeleting}
              onClick={() => setIsDeleteConfirmOpen(true)}
            >
              {isDeleting ? '삭제하는 중...' : '계정 삭제'}
            </button>
            <p className="my-page-field-hint">익명 계정과 참여한 모든 약속 기록이 삭제되며 되돌릴 수 없어요</p>
          </div>
        </>
      )}

      {isDeleteConfirmOpen && (
        <ConfirmModal
          title="계정 삭제"
          message="삭제하면 내 정보와, 내가 만든 약속방 전체(다른 참여자 정보 포함)가 모두 사라져요. 정말 삭제할까요?"
          confirmLabel="삭제"
          cancelLabel="취소"
          danger
          onConfirm={handleConfirmDelete}
          onCancel={() => setIsDeleteConfirmOpen(false)}
        />
      )}
    </Modal>
  )
}

export default MyPageModal
