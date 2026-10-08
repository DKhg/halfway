ALTER TABLE room DROP COLUMN date_from;
ALTER TABLE room DROP COLUMN date_to;
ALTER TABLE room ADD COLUMN meeting_date DATE;

COMMENT ON COLUMN room.meeting_date IS '확정된 약속 날짜. 방장이 직접 입력(선택), 안 정하면 null(날짜 미정)';
