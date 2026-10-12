import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getRoom } from '../api/roomApi'
import {
  addSettlementItem,
  cancelSettlement,
  deleteSettlementItem,
  getSettlement,
  scanSettlementReceipt,
  startSettlement,
  updateSettlementAccount,
  updateSettlementItem,
} from '../api/settlementApi'
import BankSelect from '../components/common/BankSelect'
import ConfirmModal from '../components/common/ConfirmModal'
import Modal from '../components/common/Modal'
import StepIndicator from '../components/common/StepIndicator'
import { ROOM_FLOW_STEPS } from '../utils/roomSteps'
import './SettlementPage.css'

const AUTO_REFRESH_INTERVAL_MS = 10000

// 토스 송금 딥링크가 인식하는 축약형 은행명 그대로 사용 (드롭박스라 오타·표기 차이로 안 열리는 문제 자체를 없앰)
const BANK_OPTIONS = [
  '카카오뱅크', '토스뱅크', '케이뱅크',
  '국민', '신한', '우리', '하나', '기업', '농협', '수협', 'SC제일', '씨티',
  '대구', '부산', '광주', '전북', '경남', '제주',
  '새마을금고', '신협', '우체국',
]

function SettlementPage() {
  const navigate = useNavigate()
  const { code } = useParams()

  const [room, setRoom] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const isLoading = room === null && !loadError

  const [settlement, setSettlement] = useState(null)
  const [isSettlementLoading, setIsSettlementLoading] = useState(true)
  const [settlementError, setSettlementError] = useState(null)
  const [isStartingSettlement, setIsStartingSettlement] = useState(false)
  const [settlementBank, setSettlementBank] = useState('')
  const [settlementAccountNumber, setSettlementAccountNumber] = useState('')
  const [isSavingAccount, setIsSavingAccount] = useState(false)
  const [itemName, setItemName] = useState('')
  const [itemAmount, setItemAmount] = useState('')
  const [itemMemberIds, setItemMemberIds] = useState([])
  const [editingItemId, setEditingItemId] = useState(null)
  const [isSavingItem, setIsSavingItem] = useState(false)
  const [pendingDeleteItemId, setPendingDeleteItemId] = useState(null)
  const [accountCopied, setAccountCopied] = useState(false)
  const [accountSaved, setAccountSaved] = useState(false)
  const [isItemModalOpen, setIsItemModalOpen] = useState(false)
  const [isStartConfirmOpen, setIsStartConfirmOpen] = useState(false)
  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false)
  const [isCanceling, setIsCanceling] = useState(false)
  const [isScanningReceipt, setIsScanningReceipt] = useState(false)
  const receiptInputRef = useRef(null)

  const fetchRoom = useCallback(() => {
    return getRoom(code)
      .then((data) => {
        setRoom(data)
        setLoadError(null)
      })
      .catch((err) => {
        setLoadError(err.response?.status === 404 ? '존재하지 않는 방이에요.' : '방 정보를 불러오지 못했어요.')
      })
  }, [code])

  useEffect(() => {
    fetchRoom()
  }, [fetchRoom])

  const fetchSettlement = useCallback(() => {
    return getSettlement(code)
      .then((data) => {
        setSettlement(data)
        setSettlementError(null)
      })
      .catch((error) => {
        if (error.response?.status === 404) {
          setSettlement(null)
          setSettlementError(null)
        } else {
          setSettlementError('정산 정보를 불러오지 못했어요.')
        }
      })
      .finally(() => setIsSettlementLoading(false))
  }, [code])

  useEffect(() => {
    fetchSettlement()
    const intervalId = setInterval(() => {
      if (document.visibilityState === 'visible') {
        fetchSettlement()
      }
    }, AUTO_REFRESH_INTERVAL_MS)
    return () => clearInterval(intervalId)
  }, [fetchSettlement])

  // 아직 참여 안 한 사람이 정산 링크로 바로 들어오면, 참여는 항상 방 페이지에서 하게 돌려보냄
  const notYetJoined = room && !room.participants.some((participant) => participant.me)
  useEffect(() => {
    if (notYetJoined) {
      navigate(`/rooms/${code}`)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [notYetJoined])

  const handleStartSettlement = async () => {
    if (isStartingSettlement) {
      return
    }
    setIsStartConfirmOpen(false)
    setIsStartingSettlement(true)
    setSettlementError(null)
    try {
      await startSettlement(code)
      await fetchSettlement()
    } catch (error) {
      setSettlementError(error.response?.data?.message || '정산을 시작하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsStartingSettlement(false)
    }
  }

  // 정산 취소 후에는 이 화면에 "정산 시작하기" 버튼을 다시 보여주지 않고, 장소 확정 화면의 CTA로만 다시 시작하게 함
  // 정산만 하기 모드는 정산이 방의 전부라 취소하면 방 자체가 삭제되므로 홈으로 보냄
  const handleCancelSettlement = async () => {
    const isSettlementOnly = room.mode === 'SETTLEMENT_ONLY'
    setIsCancelConfirmOpen(false)
    setIsCanceling(true)
    setSettlementError(null)
    try {
      await cancelSettlement(code)
      navigate(isSettlementOnly ? '/' : `/rooms/${code}`)
    } catch (error) {
      setSettlementError(error.response?.data?.message || '정산을 취소하지 못했어요. 다시 시도해 주세요.')
      setIsCanceling(false)
    }
  }

  const handleSaveAccount = async () => {
    // 화면엔 기존 저장값이 폴백으로 보이지만 입력을 안 건드렸으면 로컬 상태는 비어있을 수 있어, 그 경우도 실제 저장값으로 간주
    const bank = (settlementBank || settlement.payerBank || '').trim()
    const accountNumber = (settlementAccountNumber || settlement.payerAccountNumber || '').trim()
    if (!bank || !accountNumber || isSavingAccount) {
      return
    }
    setIsSavingAccount(true)
    setSettlementError(null)
    try {
      await updateSettlementAccount(code, bank, accountNumber)
      await fetchSettlement()
      setAccountSaved(true)
      setTimeout(() => setAccountSaved(false), 2000)
    } catch (error) {
      setSettlementError(error.response?.data?.message || '계좌 정보를 저장하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsSavingAccount(false)
    }
  }

  const handleToggleItemMember = (participantId) => {
    setItemMemberIds((prev) =>
      prev.includes(participantId) ? prev.filter((id) => id !== participantId) : [...prev, participantId],
    )
  }

  const handleOpenAddItemModal = () => {
    setEditingItemId(null)
    setItemName('')
    setItemAmount('')
    // 대부분 전원이 나눠 내는 상황이라 기본값은 전체 체크, 일부만 먹은 항목이면 빼야 할 사람만 해제하게 함
    setItemMemberIds(room.participants.map((participant) => participant.id))
    setSettlementError(null)
    setIsItemModalOpen(true)
  }

  const handleStartEditItem = (item) => {
    setEditingItemId(item.id)
    setItemName(item.name)
    setItemAmount(String(item.amount))
    setItemMemberIds(item.participantIds)
    setSettlementError(null)
    setIsItemModalOpen(true)
  }

  const handleCloseItemModal = () => {
    setIsItemModalOpen(false)
    setEditingItemId(null)
    setItemName('')
    setItemAmount('')
    setItemMemberIds([])
  }

  const handleSaveItem = async () => {
    const amount = Number(itemAmount)
    if (!itemName.trim() || !amount || amount <= 0 || itemMemberIds.length === 0 || isSavingItem) {
      return
    }
    setIsSavingItem(true)
    setSettlementError(null)
    const payload = { name: itemName.trim(), amount, participantIds: itemMemberIds }
    try {
      if (editingItemId) {
        await updateSettlementItem(code, editingItemId, payload)
      } else {
        await addSettlementItem(code, payload)
      }
      handleCloseItemModal()
      await fetchSettlement()
    } catch (error) {
      setSettlementError(error.response?.data?.message || '항목을 저장하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsSavingItem(false)
    }
  }

  const handleReceiptFileChange = async (e) => {
    const file = e.target.files[0]
    e.target.value = ''
    if (!file || isScanningReceipt) {
      return
    }
    setIsScanningReceipt(true)
    setSettlementError(null)
    try {
      const draft = await scanSettlementReceipt(code, file)
      if (draft.name) {
        setItemName(draft.name)
      }
      if (draft.amount) {
        setItemAmount(String(draft.amount))
      }
      if (!draft.name && !draft.amount) {
        setSettlementError('영수증을 인식하지 못했어요. 직접 입력해 주세요.')
      }
    } catch {
      setSettlementError('영수증을 읽지 못했어요. 다시 시도해 주세요.')
    } finally {
      setIsScanningReceipt(false)
    }
  }

  const handleDeleteItem = async (itemId) => {
    if (pendingDeleteItemId) {
      return
    }
    setPendingDeleteItemId(itemId)
    setSettlementError(null)
    try {
      await deleteSettlementItem(code, itemId)
      await fetchSettlement()
    } catch {
      setSettlementError('항목을 삭제하지 못했어요. 다시 시도해 주세요.')
    } finally {
      setPendingDeleteItemId(null)
    }
  }

  const handleCopyAccount = async () => {
    if (!settlement?.payerAccountNumber) {
      return
    }
    try {
      await navigator.clipboard.writeText(settlement.payerAccountNumber)
      setAccountCopied(true)
      setTimeout(() => setAccountCopied(false), 2000)
    } catch {
      setAccountCopied(false)
    }
  }

  // 토스 송금 딥링크는 "국민"처럼 "은행" 뗀 축약형과, 하이픈·공백 없는 계좌번호만 인식함
  const buildTossLink = (bank, accountNumber, amount) => {
    const normalizedBank = bank.replace(/은행$/, '').trim()
    const normalizedAccountNumber = accountNumber.replace(/[^0-9]/g, '')
    return `supertoss://send?bank=${encodeURIComponent(normalizedBank)}&accountNo=${normalizedAccountNumber}&amount=${amount}`
  }

  if (isLoading) {
    return (
      <div className="settlement-page">
        <p className="settlement-status">불러오는 중...</p>
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="settlement-page">
        <header className="settlement-header">
          <button type="button" aria-label="뒤로" className="settlement-back-btn" onClick={() => navigate(`/rooms/${code}`)}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M15 5l-7 7 7 7" />
            </svg>
          </button>
        </header>
        <p className="settlement-status">{loadError}</p>
      </div>
    )
  }

  // 백엔드 Jackson 직렬화가 boolean getter의 is 접두사를 벗겨서 me 키로 내려줌
  const me = room.participants.find((participant) => participant.me)
  const isPayer = me && settlement && settlement.payerParticipantId === me.id
  const myTotal = settlement?.totals.find((total) => total.participantId === me?.id)
  // 정산만 하기 모드는 역·장소 화면이 없어서 방 페이지로 돌아가면 바로 이 화면으로 되돌아와 버림 - 뒤로가기는 홈으로 보냄
  const isSettlementOnly = room.mode === 'SETTLEMENT_ONLY'

  return (
    <div className="settlement-page">
      <header className="settlement-header">
        <button type="button" aria-label="뒤로" className="settlement-back-btn" onClick={() => navigate(isSettlementOnly ? '/' : `/rooms/${code}`)}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M15 5l-7 7 7 7" />
          </svg>
        </button>
      </header>

      {room.mode !== 'SETTLEMENT_ONLY' && (
        <StepIndicator
          steps={ROOM_FLOW_STEPS}
          currentKey="SETTLEMENT"
          onStepClick={(key) => key !== 'SETTLEMENT' && navigate(`/rooms/${code}`)}
        />
      )}

      <h1 className="settlement-title">정산</h1>
      <p className="settlement-subtitle">
        {room.mode === 'SETTLEMENT_ONLY' ? '정산만 빠르게 하는 방이에요' : `${room.confirmedStationName}에서 만난 약속 정산이에요`}
      </p>

      {isSettlementLoading ? (
        <div className="settlement-progress-hint">불러오는 중...</div>
      ) : !settlement ? (
        <button
          type="button"
          className="settlement-start-btn"
          disabled={isStartingSettlement}
          onClick={() => setIsStartConfirmOpen(true)}
        >
          {isStartingSettlement ? '시작하는 중...' : '정산 시작하기'}
        </button>
      ) : (
        <div className="settlement-panel">
          <div className="settlement-payer-row">
            <div className="settlement-payer-badge">총무: {settlement.payerNickname}</div>
            {(room.owner || isPayer) && (
              <button
                type="button"
                className="settlement-cancel-btn"
                disabled={isCanceling}
                onClick={() => setIsCancelConfirmOpen(true)}
              >
                {isCanceling ? '취소하는 중...' : '정산 취소'}
              </button>
            )}
          </div>

          <div className="settlement-items">
            {settlement.items.length === 0 && (
              <div className="settlement-progress-hint">아직 등록된 항목이 없어요.</div>
            )}
            {settlement.items.map((item) => (
              <div key={item.id} className="settlement-item-card">
                <div className="settlement-item-top">
                  <span className="settlement-item-name">{item.name}</span>
                  <span className="settlement-item-amount">{item.amount.toLocaleString()}원</span>
                </div>
                <div className="settlement-item-members">{item.participantNicknames.join(', ')}</div>
                <div className="settlement-item-actions">
                  <button
                    type="button"
                    aria-label="항목 수정"
                    className="settlement-item-edit-btn"
                    onClick={() => handleStartEditItem(item)}
                  >
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                    </svg>
                    수정
                  </button>
                  <button
                    type="button"
                    aria-label="항목 삭제"
                    className="settlement-item-delete-btn"
                    disabled={pendingDeleteItemId === item.id}
                    onClick={() => handleDeleteItem(item.id)}
                  >
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <path d="M4 7h16M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2m-9 0 1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13" />
                    </svg>
                    삭제
                  </button>
                </div>
              </div>
            ))}
          </div>

          <button type="button" className="settlement-add-item-btn" onClick={handleOpenAddItemModal}>
            + 항목 추가
          </button>

          <div className="settlement-totals">
            {settlement.totals.map((total) => (
              <div key={total.participantId} className="settlement-total-row">
                <span>
                  {total.nickname}
                  {total.payer ? ' (총무)' : ''}
                </span>
                <span>{total.amount.toLocaleString()}원</span>
              </div>
            ))}
          </div>

          {isPayer ? (
            <div className="settlement-account-form">
              <BankSelect
                value={settlementBank || settlement.payerBank || ''}
                onChange={setSettlementBank}
                options={BANK_OPTIONS}
              />
              <input
                type="text"
                placeholder="계좌번호"
                value={settlementAccountNumber || settlement.payerAccountNumber || ''}
                onChange={(e) => setSettlementAccountNumber(e.target.value)}
              />
              <button type="button" disabled={isSavingAccount} onClick={handleSaveAccount}>
                {accountSaved ? '저장됨!' : isSavingAccount ? '저장하는 중...' : '계좌 저장'}
              </button>
            </div>
          ) : (
            myTotal && (
              <div className="settlement-pay-zone">
                <div className="settlement-my-total">
                  {settlement.payerNickname}님에게 {myTotal.amount.toLocaleString()}원 보내면 돼요
                </div>
                {settlement.payerBank && settlement.payerAccountNumber ? (
                  <div className="settlement-pay-links">
                    <a
                      className="settlement-pay-link"
                      href={buildTossLink(settlement.payerBank, settlement.payerAccountNumber, myTotal.amount)}
                    >
                      토스로 송금
                    </a>
                    <a className="settlement-pay-link" href="kakaotalk://kakaopay/home" target="_blank" rel="noreferrer">
                      카카오페이 열기
                    </a>
                    <button type="button" className="settlement-pay-link" onClick={handleCopyAccount}>
                      {accountCopied ? '복사됨!' : '계좌번호 복사'}
                    </button>
                  </div>
                ) : (
                  <div className="settlement-progress-hint">총무가 아직 계좌 정보를 안 넣었어요.</div>
                )}
              </div>
            )
          )}
        </div>
      )}

      {!isItemModalOpen && settlementError && <div className="settlement-error">{settlementError}</div>}

      {isItemModalOpen && (
        <Modal title={editingItemId ? '항목 수정' : '항목 추가'} onClose={handleCloseItemModal}>
          <input
            type="file"
            accept="image/*"
            ref={receiptInputRef}
            className="settlement-receipt-input"
            onChange={handleReceiptFileChange}
          />
          <button
            type="button"
            className="settlement-receipt-tile"
            disabled={isScanningReceipt}
            onClick={() => receiptInputRef.current?.click()}
          >
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <path d="M14 2v6h6" />
              <path d="M9 13h6M9 17h6" />
            </svg>
            <span className="settlement-receipt-tile-title">영수증으로 자동 채우기</span>
            <span className="settlement-receipt-tile-subtitle">
              {isScanningReceipt ? '영수증 읽는 중...' : '탭하여 촬영 또는 업로드 (선택)'}
            </span>
          </button>

          {settlementError && <div className="settlement-error">{settlementError}</div>}

          <input
            type="text"
            placeholder="항목 이름 (예: 1차 고기·식사)"
            className="settlement-modal-input"
            value={itemName}
            onChange={(e) => setItemName(e.target.value)}
          />
          <input
            type="number"
            inputMode="numeric"
            placeholder="금액"
            className="settlement-modal-input settlement-amount-input"
            value={itemAmount}
            onChange={(e) => setItemAmount(e.target.value)}
          />
          <div className="settlement-member-checks-label">이 항목, 누구랑 나눠낼까요?</div>
          <div className="settlement-member-checks">
            {room.participants.map((participant) => (
              <label key={participant.id} className="settlement-member-check">
                <input
                  type="checkbox"
                  checked={itemMemberIds.includes(participant.id)}
                  onChange={() => handleToggleItemMember(participant.id)}
                />
                {participant.nickname}
              </label>
            ))}
          </div>
          <div className="settlement-item-form-actions">
            <button type="button" className="settlement-modal-save-btn" disabled={isSavingItem} onClick={handleSaveItem}>
              {isSavingItem ? '저장하는 중...' : editingItemId ? '수정 완료' : '항목 추가'}
            </button>
          </div>
        </Modal>
      )}

      {isStartConfirmOpen && (
        <ConfirmModal
          message="총무가 되는 거예요, 계속할까요?"
          confirmLabel="계속"
          cancelLabel="취소"
          onConfirm={handleStartSettlement}
          onCancel={() => setIsStartConfirmOpen(false)}
        />
      )}

      {isCancelConfirmOpen && (
        <ConfirmModal
          title="정산 취소"
          message={room.mode === 'SETTLEMENT_ONLY'
            ? '정산만 하는 방이라 취소하면 방 자체가 삭제돼요. 계속할까요?'
            : '총무 지정과 입력된 항목이 모두 지워지고 처음부터 다시 시작해요. 계속할까요?'}
          confirmLabel="취소하기"
          cancelLabel="닫기"
          danger
          onConfirm={handleCancelSettlement}
          onCancel={() => setIsCancelConfirmOpen(false)}
        />
      )}
    </div>
  )
}

export default SettlementPage
