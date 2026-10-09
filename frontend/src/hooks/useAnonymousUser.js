import { useEffect, useState } from 'react'
import { createAnonymousUser } from '../api/userApi'
import { getToken, setToken } from '../utils/authToken'
import { isStandalone } from '../utils/platform'

export function useAnonymousUser() {
  const [isReady, setIsReady] = useState(Boolean(getToken()))
  const [error, setError] = useState(null)
  // 홈 화면 PWA 최초 실행(standalone이면서 토큰 없음)이면 자동으로 새 계정을 만들지 않고
  // "코드로 이어하기 / 새로 시작하기" 선택을 기다림 (다른 브라우저 세션의 식별자를 이어받을 기회)
  const [needsPairingChoice, setNeedsPairingChoice] = useState(!getToken() && isStandalone())

  useEffect(() => {
    if (isReady || needsPairingChoice) {
      return
    }

    createAnonymousUser()
      .then((data) => {
        setToken(data.token)
        setIsReady(true)
      })
      .catch(() => {
        setError('사용자 정보를 만들지 못했어요. 새로고침해 주세요.')
      })
  }, [isReady, needsPairingChoice])

  // "새로 시작하기" 선택 시, 선택 화면을 넘기고 위 useEffect가 일반 익명 계정 생성을 진행하게 함
  const startFresh = () => setNeedsPairingChoice(false)

  // 페어링 코드로 기존 토큰을 복원했을 때 호출
  const applyRestoredToken = (token) => {
    setToken(token)
    setNeedsPairingChoice(false)
    setIsReady(true)
  }

  return { isReady, error, needsPairingChoice, startFresh, applyRestoredToken }
}
