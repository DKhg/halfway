import { useEffect, useState } from 'react'
import { createAnonymousUser } from '../api/userApi'
import { getToken, setToken } from '../utils/authToken'

export function useAnonymousUser() {
  const [isReady, setIsReady] = useState(Boolean(getToken()))
  const [error, setError] = useState(null)

  useEffect(() => {
    if (isReady) {
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
  }, [isReady])

  return { isReady, error }
}
