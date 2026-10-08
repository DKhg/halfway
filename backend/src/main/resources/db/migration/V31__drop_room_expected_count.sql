-- 예상 인원수는 진행률 표시용으로 만들어뒀지만 프론트엔드에 입력·표시 UI가 끝내 없어서 항상 null로만 남던 컬럼
ALTER TABLE room DROP COLUMN expected_count;
