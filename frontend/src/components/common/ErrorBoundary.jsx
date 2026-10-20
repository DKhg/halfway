import { Component } from 'react'
import './ErrorBoundary.css'

class ErrorBoundary extends Component {
  state = { error: null }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('[렌더링 에러]', error, info.componentStack)
  }

  render() {
    const { error } = this.state
    if (!error) {
      return this.props.children
    }

    return (
      <div className="error-boundary">
        <p className="error-boundary-title">일시적인 문제가 생겼어요</p>
        <button type="button" className="error-boundary-reload-btn" onClick={() => window.location.reload()}>
          새로고침
        </button>
        <details className="error-boundary-details">
          <summary>에러 내용 보기</summary>
          <pre>{error.stack || error.message || String(error)}</pre>
        </details>
      </div>
    )
  }
}

export default ErrorBoundary
