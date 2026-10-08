import { useEffect, useState } from 'react'
import { subscribeLoading } from '../../utils/loadingBus'
import './GlobalLoadingBar.css'

function GlobalLoadingBar() {
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => subscribeLoading(setIsLoading), [])

  if (!isLoading) {
    return null
  }

  return (
    <div className="global-loading-bar" role="status" aria-label="불러오는 중">
      <div className="global-loading-bar-fill" />
    </div>
  )
}

export default GlobalLoadingBar
