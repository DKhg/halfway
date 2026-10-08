# Backend 규칙 (Spring Boot)

## 패키지 구조
- `com.[아이디].halfway.global`: `config`(Security, CORS, JPA Audit), `error`(GlobalExceptionHandler, CustomException), `common`(BaseTimeEntity)
- `com.[아이디].halfway.domain.[도메인]`: `controller`, `service`, `repository`, `entity`, `dto`
- 공공데이터 배치 적재가 필요한 도메인은 `batch`(+`batch/dto`) 서브패키지 추가. `@Profile("batch")` 컴포넌트로 만들어서 `--spring.profiles.active=local,batch`로 실행할 때만 동작하게 함
- 도메인: `user`, `room`, `participant`, `vote`, `midpoint`, `lasttrain`, `settlement`, `place`, `push`, `station`

## 계층별 규칙
- Controller: `@RestController`, `@RequiredArgsConstructor`, `@RequestMapping("/api/[도메인]")`. 반환은 `ResponseEntity<T>`, Entity 직접 반환 금지
- Service: 클래스에 `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)`. CUD 메서드에만 `@Transactional`
- 의존성 주입: `@RequiredArgsConstructor` + `private final`만 사용 (`@Autowired` 금지)
- Entity: `@Entity`, `@Table(name = "snake_case")`, `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. `@Setter`, `@AllArgsConstructor` 금지. `BaseTimeEntity` 상속
- DTO: Request는 `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor`, `@Builder` / Response는 `@Getter`, `@AllArgsConstructor`, `@Builder`

## DB
- 테이블·컬럼은 `snake_case`
- PK: 기본 `IDENTITY`. 단, `users`와 외부 링크에 노출되는 ID(방 코드 등)는 UUID 또는 추측 불가능한 랜덤 코드
- 연관관계는 모두 `FetchType.LAZY`
- 스키마 변경은 반드시 Flyway 마이그레이션 파일 추가 (`V{번호}__{설명}.sql`). 기존 마이그레이션 수정 금지
- 테이블 생성 마이그레이션엔 `COMMENT ON TABLE`/`COMMENT ON COLUMN`으로 테이블·컬럼 설명을 같이 작성 (DB를 직접 열어봐도 무슨 컬럼인지 알 수 있게, 엔티티 필드 주석과 같은 내용)
- 시간은 `Asia/Seoul` 기준으로 저장·계산 (막차·약속 당일 알림)

## 설정
- 환경 변수는 `application.yml`에서 `${SPRING_DATASOURCE_URL}` 형태로 외부화
- CORS는 `SecurityConfig`의 `corsConfigurationSource`에서 `http://localhost:5173`과 배포 도메인만 허용
- API 문서는 springdoc-openapi로 자동 생성

## 로깅 (@Slf4j)
- `System.out.println` 금지, 이모지 금지, 문자열 `+` 연산 대신 `{}` 치환
- INFO: `log.info("[요청] 방 생성 - 참여자 수: {}", size);` / `log.info("[완료] 방 생성 - ID: {}", id);`
- WARN: `log.warn("[경고] 존재하지 않는 방 접근: {}", code);`
- ERROR: `log.error("[에러] 정산 계산 실패", e);`
- 출발역·귀가역 등 위치 정보는 로그에 남기지 않기

## 주석
- 클래스·주요 public 메서드에 `/** */`, 로직 내부에 `//`
- Controller·Service의 public 메서드는 로직이 단순해도 한 줄 요약 주석 필수 (뭘 하는 메서드인지 훑어볼 수 있게)
- Entity는 필드마다 한 줄 주석으로 의미 설명 (enum 값 종류, nullable인 이유 등). `docs/DB.md`는 코드 생긴 뒤 갱신 안 하므로, 그 문서의 "설명" 컬럼 내용을 필드 주석으로 옮겨서 엔티티가 최신 설명의 기준이 되게 함
- 명사형 종결 (예: `막차 시각 역산`, `서연 제외 항목 분배`)
- 로직 내부 주석은 코드 설명이 아닌 이유 중심 (`// 환승 도보 시간을 빼야 앞 구간 막차 탑승 가능`)
- 개선 필요 시 `// TODO:`, `// FIXME:`

## 테스트
- `midpoint`, `lasttrain`, `settlement` 계산 로직은 반드시 단위 테스트 동반
- 계산 규칙과 정답 예시는 `docs/DOMAIN.md` 기준

## 포맷
- 들여쓰기 스페이스 4칸, 메서드 사이 빈 줄 1줄, 연속 빈 줄 금지
