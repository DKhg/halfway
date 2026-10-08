import './StepIndicator.css'

// steps: [{ key, label }], currentKey: 현재 단계, onStepClick(key): 클릭 가능한 단계를 눌렀을 때. disabledKeys: 눌러도 이동 안 되는 단계
function StepIndicator({ steps, currentKey, onStepClick, disabledKeys = [] }) {
  const currentIndex = steps.findIndex((step) => step.key === currentKey)

  return (
    <div className="step-indicator">
      {steps.map((step, index) => {
        const isDone = index < currentIndex
        const isCurrent = step.key === currentKey
        const isDisabled = disabledKeys.includes(step.key)
        const status = isCurrent ? 'current' : isDone ? 'done' : 'upcoming'
        return (
          <div className="step-indicator-item" key={step.key}>
            <button
              type="button"
              className="step-indicator-pill"
              data-status={status}
              disabled={isDisabled || isCurrent}
              onClick={() => onStepClick?.(step.key)}
            >
              {isDone ? '✓' : index + 1} {step.label}
            </button>
            {index < steps.length - 1 && <span className="step-indicator-arrow">→</span>}
          </div>
        )
      })}
    </div>
  )
}

export default StepIndicator
