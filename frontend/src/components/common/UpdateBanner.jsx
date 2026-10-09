import { useEffect, useState } from 'react'
import { applyPwaUpdate, subscribePwaUpdate } from '../../utils/pwaUpdateBus'
import './UpdateBanner.css'

function UpdateBanner() {
  const [needsRefresh, setNeedsRefresh] = useState(false)

  useEffect(() => subscribePwaUpdate(setNeedsRefresh), [])

  if (!needsRefresh) {
    return null
  }

  return (
    <div className="update-banner" role="status">
      <span className="update-banner-text">새 버전이 있어요</span>
      <button type="button" className="update-banner-btn" onClick={applyPwaUpdate}>
        새로고침
      </button>
    </div>
  )
}

export default UpdateBanner
