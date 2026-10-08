COMMENT ON TABLE users IS '익명 사용자';
COMMENT ON COLUMN users.id IS '기기별 익명 ID';
COMMENT ON COLUMN users.token_hash IS '인증 토큰 해시';
COMMENT ON COLUMN users.oauth_provider IS '소셜 로그인 provider';
COMMENT ON COLUMN users.oauth_id IS '소셜 로그인 사용자 ID';
COMMENT ON COLUMN users.last_nickname IS '최근 참여 닉네임';

COMMENT ON TABLE room IS '약속 방';
COMMENT ON COLUMN room.id IS '내부 식별자';
COMMENT ON COLUMN room.code IS '링크용 랜덤 코드';
COMMENT ON COLUMN room.owner_user_id IS '방장';
COMMENT ON COLUMN room.name IS '방 이름';
COMMENT ON COLUMN room.mode IS '장소 정하는 방식';
COMMENT ON COLUMN room.fairness_type IS '공평 기준';
COMMENT ON COLUMN room.expected_count IS '예상 인원수';
COMMENT ON COLUMN room.date_from IS '날짜 투표 시작일';
COMMENT ON COLUMN room.date_to IS '날짜 투표 종료일';
COMMENT ON COLUMN room.status IS '진행 상태';

COMMENT ON TABLE participant IS '방 참여자';
COMMENT ON COLUMN participant.room_id IS '참여한 방';
COMMENT ON COLUMN participant.user_id IS '참여한 사용자';
COMMENT ON COLUMN participant.nickname IS '참여자 닉네임';
COMMENT ON COLUMN participant.start_station_name IS '출발역';
COMMENT ON COLUMN participant.home_station_name IS '귀가역';
COMMENT ON COLUMN participant.transport IS '이동수단';
