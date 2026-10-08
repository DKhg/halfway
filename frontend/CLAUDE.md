# Frontend 규칙 (Vite + React JS)

## 구조
- `src/api`: `axios.js` 공통 인스턴스 + 도메인별 API 함수 (`roomApi.js`, `settlementApi.js`)
- `src/components/common`: 버튼, 모달, 인풋 등 공통 UI / `src/components/layout`: 레이아웃
- `src/features/[기능]`: 기능별 모듈 (`room`, `vote`, `midpoint`, `places`, `lasttrain`, `settlement`)
- `src/pages`: 라우팅 대상 페이지 / `src/hooks`: 공통 훅 / `src/stores`: 전역 상태 / `src/utils`: 순수 함수
- `src/styles/tokens.css`: 디자인 토큰 (색·폰트·간격). 색상 하드코딩 금지, 토큰 변수 사용

## 네이밍
- 컴포넌트·페이지: `.jsx`, PascalCase (`ResultTop3.jsx`)
- 훅·유틸·API: `.js`, camelCase (`useRoom.js`, `settlementApi.js`)

## API 통신
- 반드시 `src/api/axios.js` 인스턴스 사용, 컴포넌트에서 `axios` 직접 호출 금지
- 개발 환경 프록시: `vite.config.js`에서 `/api` → `http://localhost:8080`
- 모든 비동기 화면에 로딩(`isLoading`)과 에러(`error`) UI 함께 구현
- 개발 환경(`import.meta.env.DEV`)에서만 인터셉터 로그 출력
  - `console.log('[API REQ]', config.method, config.url, config.data)`
  - `console.log('[API RES]', response.config.url, response.data)`
  - `console.error('[API ERR]', error.config?.url, error.response?.data || error.message)`

## 앱 특성
- 모바일 우선: 390px 기준 설계, 터치 영역 최소 44px
- 첫 방문 시 익명 사용자 ID(UUID) 생성 후 저장, 모든 요청에 포함
- 초대 링크로 들어온 사용자는 가입·설치 없이 닉네임만 입력하고 참여
- 알림 권한은 약속 확정 후 "당일 알림 받기" 버튼 클릭 시에만 요청
- 길찾기·송금은 외부 앱 링크로 열기 (카카오맵, 네이버지도, 토스, 카카오페이)
- 공유 페이지에는 카카오톡 미리보기용 OG 태그 설정
- 방 화면 갱신: 새로고침 버튼 + 당겨서 새로고침 + 10초 자동 조회. `document.visibilityState`가 hidden이면 조회 중지, visible로 돌아오면 즉시 1회 조회

## 환경 변수
- `VITE_` 접두사 사용 (`import.meta.env.VITE_API_BASE_URL`, `VITE_KAKAO_MAP_KEY`)

## 포맷
- 들여쓰기 스페이스 2칸, 함수·컴포넌트 사이 빈 줄 1줄, 연속 빈 줄 금지
- 주석은 명사형 종결, 이유 중심
