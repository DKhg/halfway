-- 카카오 로컬 API 카테고리 개수 기반 혼잡도 판정이 실측 결과 신뢰도가 낮아 기능 폐기, has_indoor도 판별 방법이 없어 함께 폐기
ALTER TABLE station DROP COLUMN area_type;
ALTER TABLE station DROP COLUMN has_indoor;
