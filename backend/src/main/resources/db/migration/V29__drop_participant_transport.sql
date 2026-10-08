-- 자차("차 가져와요") 체크 기능 제거. 막차 제외·주차장 표시 등 핵심 로직이 구현되지 않은 채 체크/라벨 표시만 있던 반쪽짜리 기능이라 전부 지하철만 지원하도록 정리
ALTER TABLE participant DROP COLUMN transport;
