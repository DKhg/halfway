<div align="center">

<img src="frontend/public/pwa-512x512.png" width="88" alt="어디서봄 로고" />

# 어디서봄

**친구 모임의 날짜, 공평한 중간지점, 막차, 정산을 링크 하나로.**
설치도 가입도 없이, 링크만 열면 바로 참여하는 모임 준비 웹앱(PWA)입니다.

[🔗 서비스 바로가기](https://xn--2z1bo4m2odm3f.kr)

</div>

<br/>

## 왜 만들었나

친구들과 약속을 잡을 때마다 똑같은 과정을 반복합니다. 단톡방에서 "어디서 볼까"로 시작해 누군가의 집·회사 근처로 은근슬쩍 정해지고, 막차 시간은 각자 네이버에 검색하고, 정산은 누가 얼마를 더 냈는지 계산기를 두드립니다.

**어디서봄**은 이 과정 전체를 하나의 링크로 묶었습니다. 참여자가 각자 출발역만 입력하면 전원에게 가장 공평한 역을 자동으로 계산해 제시하고, 투표로 확정하고, 막차 시간을 알려주고, 끝나면 정산까지 끝냅니다. 로그인도 앱 설치도 없이, 링크를 받은 사람은 닉네임만 입력하면 바로 참여합니다.

<br/>

## 핵심 플로우

### 1. 출발역만 넣으면 — 공평한 중간지점 자동 계산

참여자가 출발역을 입력할 때마다 서버가 **사전에 계산해둔 지하철 소요시간 그래프**로 전원이 도달 가능한 역 전체를 훑어, "제일 오래 걸리는 사람"을 최소화하는 순서로 상위 3개 후보를 제시합니다. 후보마다 참여자별 소요시간과 0~100점의 공평도 점수를 함께 보여줘서, "왜 이 역인지"가 바로 납득되게 했습니다.

<img src=".github/screenshots/03-midpoint.png" width="360" alt="중간지점 자동 계산 결과 화면" />

### 2. 다중 투표로 확정

추천 후보에 전원이 "괜찮아요"를 체크하는 방식입니다. 득표 1위가 동점이면 공평도로, 그래도 같으면 방장이 직접 고릅니다. 2명뿐인 방은 둘 다 체크한 곳이 바로 합의점이 됩니다.

<img src=".github/screenshots/04-voting.png" width="360" alt="역 후보 투표 화면" />

### 3. 역이 정해지면 — 맛집·카페도 투표로

역이 확정되면 카카오 로컬 API로 반경 내 음식점·카페를 실시간 검색해 보여줍니다. 추천 후보 외에 참여자가 직접 키워드로 검색해 후보를 추가할 수도 있고, 이렇게 추가된 가게는 **역 단위로 누적 저장**돼 같은 역에서 모이는 다른 모임에도 "다른 모임이 추가한 곳"으로 제안됩니다.

<img src=".github/screenshots/05-place-list.png" width="360" alt="음식점 후보 목록 화면" />
<img src=".github/screenshots/06-place-confirmed.png" width="360" alt="장소 확정 후 카카오맵/네이버지도 연결 화면" />

### 4. 약속 당일 — 막차 계산

귀가역을 입력하면 서버가 **배치로 미리 적재해둔 열차 시간표**를 거꾸로 역산해 각자의 막차 출발 시각을 계산합니다. 외부 경로 API를 요청 시점에 호출하지 않고 전부 서버 내부 계산으로 끝내 응답이 빠르고 비용이 들지 않습니다. 귀가역은 본인에게만 보이고, 다른 참여자에게는 막차 시각만 공개됩니다.

<img src=".github/screenshots/07-last-train.png" width="360" alt="막차 계산 결과 화면" />

### 5. 모임이 끝나면 — 정산

먼저 "정산 시작"을 누른 사람이 결제자가 되고, 항목을 추가하면 참여자별 분담액이 자동으로 계산됩니다. 영수증 사진을 올리면 Gemini API가 상호명·금액을 읽어 항목을 자동으로 채워주고, 송금은 토스·카카오페이 앱 링크로 바로 연결합니다(서비스가 돈을 직접 받지 않습니다).

<img src=".github/screenshots/08-settlement.png" width="360" alt="정산 화면" />

<br/>

## 기술적으로 신경 쓴 부분

### 경로 API를 쓰지 않고 지하철 소요시간을 직접 계산

중간지점·막차 계산은 사용자 요청을 처리하는 동안 **외부 API를 한 번도 호출하지 않습니다.** 서울 열린데이터광장·TAGO·GTFS의 역 목록, 역간 소요시간, 열차 시간표를 배치로 DB에 적재해두고, 모든 역 쌍에 대해 다익스트라로 최단 경로를 미리 계산해 저장합니다. 역명이 시대·노선마다 다르게 표기되는 문제, 9호선처럼 공식 실측 데이터가 아예 없는 구간, 급행 운행 등 공공데이터 특유의 결측·불일치를 메우는 데 개발 기간의 상당 부분을 썼습니다.

### 막차는 "가장 늦게 출발해도 되는 경로"를 역산

막차 계산은 단순히 최단 경로 하나를 보는 게 아니라, 약속역과 귀가역을 잇는 **경로 후보 전부**를 시간표 기준으로 거꾸로 역산해서 가장 늦은 출발 시각을 채택합니다. 제일 빠른 경로가 하필 시간표 데이터가 부실한 노선을 거치면 실제보다 이른 시각이 나올 수 있기 때문입니다.

### 로그인 없이, 그러나 안전하게

가입 절차 없이 기기마다 익명 UUID를 발급하고, 인증 토큰은 원문을 서버에 저장하지 않고 **SHA-256 해시만** 저장합니다(조회 가능해야 해서 매번 salt가 바뀌는 bcrypt 대신 결정론적 해시를 선택). "방장만", "결제자만" 같은 권한은 인증과 분리해 서비스 레이어에서 검사합니다. 추후 소셜 로그인을 붙일 수 있도록 `oauth_provider`/`oauth_id` 컬럼만 nullable로 미리 마련해뒀습니다.

### 1대의 서버로 운영 비용 최소화

RDS·ALB·NAT Gateway 같은 관리형 서비스 없이, EC2 한 대에 Nginx·Spring Boot·PostgreSQL을 Docker Compose로 함께 올렸습니다. CI에서 이미지를 빌드해 Docker Hub에 올리고 서버는 pull만 하는 구조라, 배포 시 서버 리소스를 거의 쓰지 않습니다.

```mermaid
flowchart TB
    U["사용자 브라우저 (PWA)"] -->|HTTPS| N

    subgraph EC2["AWS EC2 1대 (t3.small)"]
        N["Nginx"]
        A["Spring Boot API"]
        D[("PostgreSQL")]
        N -->|"/api/* 리버스 프록시"| A
        A --> D
    end

    A -.->|"배치 전용, 요청 처리 중엔 호출 안 함"| EXT1["서울 열린데이터광장 · TAGO<br/>역 · 역간 소요시간 · 시간표"]
    A -->|"실시간 검색 · 좌표"| EXT2["카카오 로컬 API"]
    A -.->|"캐싱 조건으로 예외 허용"| EXT3["서울시 실시간 도착정보"]
```

<br/>

## 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Java 17 · Spring Boot 4 · Spring Data JPA · Spring Security · PostgreSQL · Flyway · Gradle |
| Frontend | React 19 · Vite · JavaScript · PWA(vite-plugin-pwa) · Axios |
| Infra | Docker Compose(Nginx + API + PostgreSQL) on AWS EC2 · Terraform · GitHub Actions |
| 외부 연동 | 카카오맵/카카오 로컬 API · 서울시 공공데이터 · TAGO 국가대중교통정보 · Gemini API(영수증 인식) |

<br/>

## 설계 원칙

개인 프로젝트로 혼자 기획·설계·개발·배포까지 맡으면서 아래 원칙을 지키려 했습니다.

- **비용 최소화**: 관리형 유료 서비스를 쓰지 않고 EC2 한 대로 운영
- **로그인 없음**: 링크 하나로 설치·가입 없이 바로 참여하는 경험이 핵심 가치
- **돈을 직접 받지 않음**: 정산은 계산만 제공, 송금은 토스·카카오페이 앱으로 넘김
- **외부 API는 배치로, 요청 중엔 호출 안 함**: 응답 속도와 비용 모두를 위해 공공데이터는 미리 계산해둠
- **지하철만 지원**: 버스·자차까지 포함한 경로 계산은 정확도·비용 문제로 범위를 좁힘

<br/>

## 프로젝트 구조

```
backend/
└── src/main/java/com/hong/halfway/
    ├── global/         SecurityConfig, 에러 처리, 공통 유틸
    └── domain/
        ├── user         익명 사용자, 토큰 발급
        ├── room          방 생성·조회·모드·상태
        ├── participant   방 참여, 출발역·귀가역
        ├── midpoint      중간지점 계산 (다익스트라 기반)
        ├── vote          역 후보·투표
        ├── place         음식점·카페 후보·투표 (카카오 로컬 API 연동)
        ├── lasttrain     막차 역산 계산
        ├── settlement    정산 항목·분담·영수증 인식(Gemini)
        └── station       역·이동시간 데이터 + 배치 적재(batch)

frontend/
└── src/
    ├── api/             axios 공통 인스턴스 + 도메인별 API 함수
    ├── pages/            HomePage, CreateRoomPage, JoinPage, SettlementPage
    ├── components/       공통 UI, 레이아웃
    ├── hooks/ · utils/   공통 훅, 순수 함수
    └── styles/           디자인 토큰 (색상 하드코딩 금지)

infra/                   Terraform (EC2 · 보안그룹 · Elastic IP)
```

<br/>

## 로컬 실행

```bash
# DB
docker compose up -d

# 백엔드
cd backend && ./gradlew bootRun

# 프론트엔드
cd frontend && npm install && npm run dev
```

카카오 로컬 API, Gemini API 등 외부 연동 키는 `backend/.env.example`, `frontend/.env.example`을 참고해 `.env`로 채워야 합니다.

<br/>

<div align="center">
<br/>

Made solo by [kohonggeun](https://github.com/kohonggeun)

</div>
