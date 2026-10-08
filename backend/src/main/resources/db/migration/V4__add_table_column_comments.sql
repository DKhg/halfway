COMMENT ON TABLE users IS '익명 사용자. 로그인 없이 기기별 UUID로 구분';
COMMENT ON COLUMN users.id IS '기기별 익명 ID';
COMMENT ON COLUMN users.token_hash IS '인증 토큰(평문)의 SHA-256 해시. 평문은 발급 시 한 번만 클라이언트에 내려주고 서버엔 저장하지 않음';
COMMENT ON COLUMN users.oauth_provider IS '소셜 로그인 provider 종류(KAKAO/GOOGLE/NAVER 등). 로그인 없이는 계속 null';
COMMENT ON COLUMN users.oauth_id IS '소셜 로그인 제공자 쪽 사용자 ID. oauth_provider와 한 쌍으로 유니크';
COMMENT ON COLUMN users.last_nickname IS '최근 참여 때 쓴 닉네임. 다음 방 참여 화면에 미리 채워주기 용도';

COMMENT ON TABLE room IS '약속 방';
COMMENT ON COLUMN room.id IS '내부 식별자. 외부엔 노출하지 않음 (링크·조회는 code로만)';
COMMENT ON COLUMN room.code IS '링크에 쓰이는 추측 불가능한 랜덤 코드';
COMMENT ON COLUMN room.owner_user_id IS '방장. 장소·일시 확정 같은 방장 전용 액션의 권한 기준';
COMMENT ON COLUMN room.name IS '방 이름';
COMMENT ON COLUMN room.mode IS '장소를 정하는 방식: MIDPOINT(중간지점 계산) / FIXED(장소 이미 확정) / CANDIDATE(후보 투표)';
COMMENT ON COLUMN room.fairness_type IS '중간지점 후보 정렬 기준: MAX_MIN(모두 공평하게, 기본) / SUM_MIN(다 같이 빨리)';
COMMENT ON COLUMN room.expected_count IS '방장이 정한 예상 인원수. 진행률 표시용이라 선택 입력이고 강제되지 않음';
COMMENT ON COLUMN room.date_from IS '날짜 투표 선택 가능 기간 시작. 방장이 안 정하면 생성일로 채움';
COMMENT ON COLUMN room.date_to IS '날짜 투표 선택 가능 기간 끝. 방장이 안 정하면 date_from + 28일로 채움';
COMMENT ON COLUMN room.status IS '진행 상태: COLLECTING(출발지 수집) / VOTING(후보 투표) / CONFIRMED(확정) / CLOSED(종료)';

COMMENT ON TABLE participant IS '방 참여자';
COMMENT ON COLUMN participant.room_id IS '참여한 방';
COMMENT ON COLUMN participant.user_id IS '참여한 익명 사용자. UNIQUE(room_id, user_id)로 한 방에 중복 참여 방지';
COMMENT ON COLUMN participant.nickname IS '이 방에서 쓰는 닉네임';
COMMENT ON COLUMN participant.start_station_name IS '출발역 또는 주소. station 도메인이 아직 없어서 임시로 텍스트로 저장 (역 데이터 연결 전까지)';
COMMENT ON COLUMN participant.home_station_name IS '귀가역. 본인에게만 노출, 다른 참여자에겐 막차 시각만 표시';
COMMENT ON COLUMN participant.transport IS '이동수단: SUBWAY(막차 계산 대상) / CAR(막차 제외, 주차장 표시 대상)';
