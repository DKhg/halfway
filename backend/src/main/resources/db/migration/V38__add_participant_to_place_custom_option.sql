ALTER TABLE place_custom_option ADD COLUMN participant_id BIGINT REFERENCES participant (id);

COMMENT ON COLUMN place_custom_option.participant_id IS '이 가게를 직접 추가한 참여자. 본인만 삭제할 수 있는 기준';
