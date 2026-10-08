# 어디서봄 (코드네임: halfway)
친구 모임의 날짜 투표, 공평한 중간지점, 막차 계산, 정산을 링크 하나로 해결하는 웹앱(PWA).

## 스택
- Backend: Java 17 / Spring Boot 3.x / Spring Data JPA / PostgreSQL / Flyway / Gradle(Lombok)
- Frontend: React (Vite, JavaScript) / PWA(vite-plugin-pwa) / Axios
- Infra: Docker Compose(Nginx + API + PostgreSQL) on AWS EC2 t3.small 1대 / GitHub Actions
- 외부: 카카오맵 API(지도·장소 검색), 서울시 공공 API(실시간 도착), 공공데이터(역·시간표)

## 명령어
- 백엔드 빌드/테스트: `cd backend && ./gradlew build` / `./gradlew test` / `./gradlew test --tests "*이름*"`
- 백엔드 실행: `cd backend && ./gradlew bootRun`
- 프론트: `cd frontend && npm install` / `npm run dev` / `npm run build`
- 로컬 전체: `docker compose up -d`

## 프로젝트 원칙 (항상 지킬 것)
- 비용 최소화: RDS, EKS, ALB, NAT Gateway 등 관리형 유료 서비스 도입 제안 금지
- 로그인 없음: 익명 사용자(`users.id` UUID, 기기별 생성) 기준. `oauth_provider`/`oauth_id`는 nullable로 추후 연결(구글·카카오·네이버 등 복수 지원 대비)
- 돈을 직접 받지 않음: 정산은 금액 계산만, 송금은 토스·카카오페이 앱 링크
- 지하철 이동시간·막차 계산은 사용자 요청 처리 중 외부 경로 API 호출 금지. DB에 미리 적재한 데이터(`station_travel_time`, `train_timetable`)로 서버 내부 계산
  - 공공데이터(역, 역간 소요시간, 환승, 시간표)는 배치로 적재. 최초 1회 + 개정 시에만 실행
  - 예외: 약속 당일 지연 확인용 서울시 실시간 도착정보 API는 캐싱 조건으로 허용
- 이동수단은 지하철만 지원. 자차 체크 기능은 2026-10-05 제거(막차 제외·주차장 표시가 미구현 상태였음, 추후 재구현 검토). 자차·버스 길찾기 API 연동은 보류
- 현재 위치(GPS) 기능 금지. 역·주소는 사용자가 직접 입력
- 귀가역은 본인에게만 노출. 다른 참여자에게는 막차 시각만 표시
- 알림: 웹은 Web Push, 앱은 FCM. 문자·알림톡 등 유료 발송 사용 금지

## 작업 방식
1. 코드 수정 전 한국어로 계획 브리핑: 작업 개요 / 대상 파일 / 계층별 구현 내용 / 영향도(DB 포함)
2. 사용자 승인("진행해줘", "OK") 후에만 파일 생성·수정
3. 순서는 백엔드 API 구현·테스트 → 프론트 연동
4. 작업 후 `backend`에서 `./gradlew test`, `frontend`에서 `npm run build`로 기존 기능 손상 여부 확인
5. 피드백을 받으면 기존 방식 고집하지 말고 수정된 계획 재제시

## 금지 사항
- `.env`, API 키, DB 비밀번호 커밋 및 출력 금지
- 검증되지 않은 라이브러리 임의 추가 금지. 필요하면 이유와 함께 먼저 제안
- 알려진 취약점이 있는 버전 사용 금지, OWASP Top 10(SQL Injection, XSS, CSRF) 준수
- 주석 처리된 옛 코드, 미사용 import·변수 남기지 않기

## 문서 운영 규칙
- 이 파일, `docs/PRD.md`, `docs/DOMAIN.md`, `backend/CLAUDE.md`, `frontend/CLAUDE.md`는 개발하면서 계속 바뀌는 문서
- 기능 범위, 계산 규칙, 원칙이 바뀌는 작업이면 계획 브리핑에 "문서 변경 필요" 항목을 넣고, 승인 후 코드와 문서를 함께 수정
- 문서와 코드가 다르면 임의로 한쪽을 따르지 말고 사용자에게 먼저 확인
- `docs/PRD.md`, `docs/DOMAIN.md`를 고치면 파일 하단 "변경 이력"에 날짜와 한 줄 요약 추가
- 이 파일에 새 규칙을 넣을 때는 짧게 유지. 긴 설명은 `docs/`로 분리
- `docs/DB.md`, `docs/API.md`는 설계 초안. 코드가 생긴 뒤에는 갱신하지 않음

## 개발 일지
- 위치: `docs/일지/YYYY-MM-DD.md`, 양식은 `docs/일지/_TEMPLATE.md`
- 사용자가 "일지 작성" 또는 `/journal`을 요청하면 그날의 `git log`, `git diff`, 대화 내용을 바탕으로 양식에 맞춰 작성
- 같은 날 파일이 이미 있으면 새로 만들지 않고 이어서 추가
- 일지는 매번 읽지 않음. 새 세션 시작 시 가장 최근 일지의 "다음 할 일"만 참고

## 작업 전에 읽을 문서
- 백엔드 작업 → `backend/CLAUDE.md`
- 프론트 작업 → `frontend/CLAUDE.md`
- 중간지점·막차·정산 로직 → `docs/DOMAIN.md` (정답 예시 포함)
- 기능 범위 판단 → `docs/PRD.md`
- 테이블 구조 → `backend/src/main/resources/db/migration/` 최신 파일, 정리된 ERD는 `docs/ERD.md`(스냅샷, `docs/DB.md` 초안은 더 이상 안 씀)
- API 명세 → 로컬 실행 후 `/v3/api-docs`, 엔드포인트 목록은 `docs/ENDPOINTS.md`(스냅샷, `docs/API.md` 초안은 더 이상 안 씀)
