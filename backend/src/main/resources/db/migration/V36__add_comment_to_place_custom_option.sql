ALTER TABLE place_custom_option ADD COLUMN comment VARCHAR(20);

COMMENT ON COLUMN place_custom_option.comment IS '추가한 사람이 남긴 짧은 한줄평 (선택, 최대 20자)';
