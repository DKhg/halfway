-- place_candidate* 테이블은 실제로는 역(station) 후보 데이터인데 이름이 "place"라 음식점/카페 후보 테이블과
-- 혼동됨. station_* 로 옮기고, 자리가 빈 place_vote는 기존 place_option_vote(진짜 가게 체크 테이블)가 이어받음.
ALTER TABLE place_candidate RENAME TO station_candidate;

ALTER TABLE place_candidate_time RENAME TO station_candidate_time;
ALTER INDEX ux_place_candidate_time_candidate_participant
    RENAME TO ux_station_candidate_time_candidate_participant;

ALTER TABLE place_vote RENAME TO station_vote;
ALTER INDEX ux_place_vote_candidate_participant
    RENAME TO ux_station_vote_candidate_participant;

ALTER TABLE place_option_vote RENAME TO place_vote;
ALTER INDEX ux_place_option_vote_room_participant_category_place_id
    RENAME TO ux_place_vote_room_participant_category_place_id;
