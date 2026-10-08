ALTER TABLE place_option_vote RENAME COLUMN place_name TO place_id;
ALTER INDEX ux_place_option_vote_room_participant_category_name
    RENAME TO ux_place_option_vote_room_participant_category_place_id;

COMMENT ON COLUMN place_option_vote.place_id IS '체크 대상 가게의 카카오 로컬 API 장소 id (같은 상호가 여러 지점일 수 있어 이름 대신 id로 식별)';
