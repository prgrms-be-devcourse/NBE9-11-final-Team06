-- 1. 무용
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '무용', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '무용');

-- 2. 축제-기타
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '축제-기타', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '축제-기타');

-- 3. 기타 (기타 등등)
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '기타', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '기타');

-- 4. 축제-자연/경관
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '축제-자연/경관', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '축제-자연/경관');

-- 5. 교육/체험
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '교육/체험', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '교육/체험');

-- 6. 영화
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '영화', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '영화');

-- 7. 축제-관광/체육
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '축제-관광/체육', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '축제-관광/체육');

-- 8. 국악
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '국악', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '국악');

-- 9. 축제-문화/예술
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '축제-문화/예술', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '축제-문화/예술');

-- 10. 연극
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '연극', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '연극');

-- 11. 독주/독창회
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '독주/독창회', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '독주/독창회');

-- 12. 축제-전통/역사
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '축제-전통/역사', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '축제-전통/역사');

-- 13. 뮤지컬/오페라
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '뮤지컬/오페라', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '뮤지컬/오페라');

-- 14. 콘서트
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '콘서트', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '콘서트');

-- 15. 축제-시민화합
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '축제-시민화합', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '축제-시민화합');

-- 16. 클래식
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '클래식', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '클래식');

-- 17. 전시/미술
INSERT INTO `category` (`name`, `type`, `created_at`, `updated_at`)
SELECT '전시/미술', 'EVENT', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `name` = '전시/미술');
-- =================================================================================
-- 1. 기초 인프라 데이터 (회원, 선호도, 카테고리 선호, 장소) 삽입
-- =================================================================================

INSERT INTO `member` (`id`, `email`, `password`, `nickname`, `profile_image_url`, `role`, `status`, `created_at`, `updated_at`)
SELECT 1, 'seoul_culture_lover@gotoday.com', 'hashed_pass_456', '영등포마포러버', 'http://example.com/profile.jpg', 'USER', 'ACTIVE', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `member` WHERE `id` = 1);

-- 유저 선호 데이터 (User Preference) - 유저 1번
INSERT INTO `user_preference` (`id`, `member_id`, `preferred_area`, `companion_type`, `mobility_level`, `avoid_crowded`, `created_at`, `updated_at`)
SELECT 1, 1, '영등포', 'FAMILY', 'NORMAL', FALSE, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user_preference` WHERE `id` = 1);

-- 유저 선호 카테고리 매핑 - 유저 1번
INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 1, 1, 16 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 1);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 2, 1, 13 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 2);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 3, 1, 14 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 3);


-- 유저 2번 생성
INSERT INTO `member` (`id`, `email`, `password`, `nickname`, `profile_image_url`, `role`, `status`, `created_at`, `updated_at`)
SELECT 2, 'gallery_lover@gotoday.com', 'pass', '혼자하는전시', 'http://example.com/p2.jpg', 'USER', 'ACTIVE', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `member` WHERE `id` = 2);

INSERT INTO `user_preference` (`id`, `member_id`, `preferred_area`, `companion_type`, `mobility_level`, `avoid_crowded`, `created_at`, `updated_at`)
SELECT 2, 2, '종로', 'SOLO', 'NORMAL', TRUE, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user_preference` WHERE `id` = 2);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 4, 2, 17 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 4);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 5, 2, 6 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 5);


-- 유저 3번 생성
INSERT INTO `member` (`id`, `email`, `password`, `nickname`, `profile_image_url`, `role`, `status`, `created_at`, `updated_at`)
SELECT 3, 'festival_goer@gotoday.com', 'pass', '페스티벌크루', 'http://example.com/p3.jpg', 'USER', 'ACTIVE', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `member` WHERE `id` = 3);

INSERT INTO `user_preference` (`id`, `member_id`, `preferred_area`, `companion_type`, `mobility_level`, `avoid_crowded`, `created_at`, `updated_at`)
SELECT 3, 3, '홍대', 'FRIEND', 'NORMAL', FALSE, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user_preference` WHERE `id` = 3);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 6, 3, 9 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 6);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 7, 3, 14 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 7);


-- 유저 4번 생성
INSERT INTO `member` (`id`, `email`, `password`, `nickname`, `profile_image_url`, `role`, `status`, `created_at`, `updated_at`)
SELECT 4, 'mapo_family@gotoday.com', 'pass', '마포가족나들이', 'http://example.com/p4.jpg', 'USER', 'ACTIVE', NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `member` WHERE `id` = 4);

INSERT INTO `user_preference` (`id`, `member_id`, `preferred_area`, `companion_type`, `mobility_level`, `avoid_crowded`, `created_at`, `updated_at`)
SELECT 4, 4, '마포구', 'FAMILY', 'NORMAL', FALSE, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `user_preference` WHERE `id` = 4);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 8, 4, 14 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 8);

INSERT INTO `user_preference_category` (`id`, `user_preference_id`, `category_id`)
SELECT 9, 4, 5 WHERE NOT EXISTS (SELECT 1 FROM `user_preference_category` WHERE `id` = 9);


-- 장소 (Place) 생성
INSERT INTO `place` (`id`, `category_id`, `name`, `address`, `road_address`, `latitude`, `longitude`, `phone`, `place_url`, `description`, `source`, `external_id`, `is_active`, `created_at`, `updated_at`)
SELECT 100, 16, '영등포아트홀', '서울시 영등포구 국회대로53길 20', '서울시 영등포구 국회대로53길 20', 37.5261234, 126.9011234, '02-2670-3131', 'https://www.ydpcf.or.kr', '영등포 문화재단 핵심 아트홀', 'SEOUL_API', 'P_YDP_ART', TRUE, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `place` WHERE `id` = 100);

INSERT INTO `place` (`id`, `category_id`, `name`, `address`, `road_address`, `latitude`, `longitude`, `phone`, `place_url`, `description`, `source`, `external_id`, `is_active`, `created_at`, `updated_at`)
SELECT 101, 16, '마포아트센터 대흥홀', '서울시 마포구 대흥로20길 28', '서울시 마포구 대흥로20길 28', 37.5501234, 126.9421234, '02-3274-8600', 'https://www.mfac.or.kr', '마포 문화공연의 중심지', 'SEOUL_API', 'P_MAPO_ART', TRUE, NOW(), NOW()
    WHERE NOT EXISTS (SELECT 1 FROM `place` WHERE `id` = 101);

-- 2. 실제 API 원본 기반 Event 테이블 41개 전체 인서트 (embedding_vector는 NULL로 시작)
-- =================================================================================
--
INSERT INTO `event` (
    `id`, `place_id`, `category_id`, `title`, `start_date`, `end_date`, `event_time`,
    `fee`, `target`, `homepage_url`, `image_url`, `description`, `source`, `external_id`,
    `created_at`, `updated_at`, `area`, `latitude`, `longitude`, `embedding_vector`
) VALUES
      (185, 100, 16, '[영등포문화재단] 마티네콘서트 With 금난새 #3.베버', '2026-06-11', '2026-10-15', '11:00', '전석 15,000원', '초등학생(2019년생 포함) 이상', 'https://www.ydpcf.or.kr/artexhibit/view.do?id=537', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=0410ae87ca824f11afa6370b0c19d9a8', '[영등포문화재단] 마티네콘서트 With 금난새 #3.베버', 'SEOUL_API', 'EV_185', NOW(), NOW(), '영등포구', 37.52554700, 126.89670000, NULL),
      (186, 101, 16, '[마포문화재단] 2026 M 아티스트 선율 피아노 리사이틀 Ⅱ', '2026-06-11', '2026-09-16', '19:30', 'R석 30,000원, S석 20,000원', '8세이상 관람가능(미취학아동입장불가)', 'https://www.mfac.or.kr/performance/whole_view.jsp?sc_b_category=17&sc_b_code=BOARD_1207683401&pk_seq=2625', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=85fc1df81a114f9b90e3793fc465f23c', '[마포문화재단] 2026 M 아티스트 선율 피아노 리사이틀 Ⅱ', 'SEOUL_API', 'EV_186', NOW(), NOW(), '마포구', 37.54992400, 126.94541700, NULL),
      (187, 101, 14, '[마포문화재단] M 마티네 [2026 MAC 모닝 콘서트 ＃6]', '2026-06-11', '2026-08-26', '11:00', '전석 20,000원', '8세이상 관람가능(미취학아동입장불가)', 'https://www.mfac.or.kr/performance/whole_view.jsp?sc_b_category=17&sc_b_code=BOARD_1207683401&pk_seq=2636', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=15a21c82c86b4906a3a40ffbf0a154b4', '[마포문화재단] M 마티네 [2026 MAC 모닝 콘서트 ＃6]', 'SEOUL_API', 'EV_187', NOW(), NOW(), '마포구', 37.54992400, 126.94541700, NULL),
      (188, 100, 1, '[영등포문화재단] 영등포아트홀 상주단체 ''서울발레시어터'' [해설이 있는 고전발레]', '2026-06-11', '2026-08-22', '16:00', '전석 20,000원', '초등학생 이상(2019년생 포함)', 'https://www.ydpcf.or.kr/artexhibit/view.do?id=548', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=8396dcbd62dc4a159004478d1565e21d', '[영등포문화재단] 영등포아트홀 상주단체 ''서울발레시어터'' [해설이 있는 고전발레]', 'SEOUL_API', 'EV_188', NOW(), NOW(), '영등포구', 37.52554700, 126.89670000, NULL),
      (189, NULL, 17, '서울일러스트레이션페어 V.21', '2026-06-11', '2026-08-02', '10:00 ~ 18:00', '성인 : 15,000원 / 청소년 : 9,000원', '누구나', 'https://www.ocreo.kr/', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=58458ce3d82141a4bf0e8e612233a372', '서울일러스트레이션페어 V.21', 'SEOUL_API', 'EV_189', NOW(), NOW(), '강남구', 37.51139400, 127.05915000, NULL), -- 코엑스 기준
      (190, NULL, 1, '[꿈의숲아트센터] 2026 여름방학 시즌공연 [최태지의 발레 보물상자]', '2026-06-11', '2026-07-25', '토요일 14:00 / 16:30', '전석 35,000원', '성인, 어린이, 청소년', 'https://www.sejongpac.or.kr/dfac/dfacPerformance/dfacPerformance/performTicket.do?performIdx=37260', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=c8712f578e7f4bc6a7701659f65e75f6', '[꿈의숲아트센터] 2026 여름방학 시즌공연 [최태지의 발레 보물상자]', 'SEOUL_API', 'EV_190', NOW(), NOW(), '강북구', 37.62174300, 127.04272100, NULL),
      (191, 101, 16, '2026 마포문화재단 가족·어린이 축제 [해피 마포 와글와글] 이머시브 뮤지컬 [고래밥 - 바다 대운동회]', '2026-06-11', '2026-08-16', '11:00, 14:00, 16:30', 'R석 70,000원, S석 50,000원', '8세이상 관람가능(미취학아동입장불가)', 'https://www.mfac.or.kr/performance/whole_view.jsp?sc_b_category=17&sc_b_code=BOARD_1207683401&pk_seq=2637', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=93c282981eda4e27816283168edb99fc', '2026 마포문화재단 가족·어린이 축제 [해피 마포 와글와글] 이머시브 뮤지컬 [고래밥 - 바다 대운동회]', 'SEOUL_API', 'EV_191', NOW(), NOW(), '마포구', 37.54992400, 126.94541700, NULL),
      (192, 100, 14, '[영등포문화재단] 어슬렁 어슬렁 콘서트 #여름 [죠지&김뜻돌]', '2026-06-11', '2026-07-24', '19:30', '전석 45,000원', '중학생(2013년생 포함) 이상', 'https://www.ydpcf.or.kr/artexhibit/view.do?id=541', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=7419db533c3545ceaf22b37388863cf3', '[영등포문화재단] 어슬렁 어슬렁 콘서트 #여름 [죠지&김뜻돌]', 'SEOUL_API', 'EV_192', NOW(), NOW(), '영등포구', 37.52554700, 126.89670000, NULL),
      (193, NULL, 10, '대학로1등 판타지코믹연극 [타임]', '2026-06-11', '2026-12-31', '화,목 15:00, 17:30 주말 상이', '프리뷰예매 1인당 15,000원', '8세 이상', 'https://booking.naver.com/booking/12/bizes/1669303', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=415b27406ee0410a96615f543202602b', '대학로1등 판타지코믹연극 [타임]', 'SEOUL_API', 'EV_193', NOW(), NOW(), '종로구', 37.58221000, 127.00194400, NULL), -- 대학로 기준
      (194, 101, 16, '[마포문화재단] M 마티네 [2026 MAC 모닝 콘서트 ＃5]', '2026-06-11', '2026-07-15', '11:00', '전석 20,000원', '8세이상 관람가능(미취학아동입장불가)', 'https://www.mfac.or.kr/performance/whole_view.jsp?sc_b_category=17&sc_b_code=BOARD_1207683401&pk_seq=2635', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=9146124c6c174b139468f1e07c66e38c', '[마포문화재단] M 마티네 [2026 MAC 모닝 콘서트 ＃5]', 'SEOUL_API', 'EV_194', NOW(), NOW(), '마포구', 37.54992400, 126.94541700, NULL),
      (195, NULL, 8, '[서울남산국악당] 동해안별신굿 [굿도 보고 떡도 먹고]', '2026-06-11', '2026-07-11', '토 15:00', '전석 30,000원', '8세 이상', 'https://www.sgtt.kr/program/detail/7205', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=c574caf898f44308ab559cc6e13cf39d', '[서울남산국악당] 동해안별신굿 [굿도 보고 떡도 먹고]', 'SEOUL_API', 'EV_195', NOW(), NOW(), '중구', 37.55745400, 126.99419100, NULL),
      (196, NULL, 5, '[서울시립미술관 서소문본관] 코랄 비평연구 모임', '2026-06-11', '2026-09-09', '14:00 ~ 16:00', '무료', '비평가, 연구자', 'https://sema.seoul.go.kr/kr/whatson/education/detail?acadmyEeNo=1547254', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=c7d1eeb3a4014add8f9a91da57273d99', '[서울시립미술관 서소문본관] 코랄 비평연구 모임', 'SEOUL_API', 'EV_196', NOW(), NOW(), '중구', 37.56417500, 126.97380900, NULL),
      (197, NULL, 10, '스릴러 공포연극 [시그널]', '2026-06-11', '2026-08-24', '평일/주말 상이', '프리뷰할인 15,000원', '만 13세이상', 'https://booking.naver.com/booking/12/bizes/1669602', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=0078925dd90a4f1e8553b66555e48a22', '스릴러 공포연극 [시그널]', 'SEOUL_API', 'EV_197', NOW(), NOW(), '종로구', 37.58221000, 127.00194400, NULL),
      (198, NULL, 13, '[동대문문화재단] 2026 신규 기획공연 [브런치 콘서트 아트리움 - 국립오페라스튜디오]', '2026-06-11', '2026-07-08', '11:00 ~ 12:00', '10,000원', '초등학생 이상', 'https://zrr.kr/AUHU2P', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=956a9cd3898f41329eaaf5efac775fb5', '[동대문문화재단] 2026 신규 기획공연 [브런치 콘서트 아트리움 - 국립오페라스튜디오]', 'SEOUL_API', 'EV_198', NOW(), NOW(), '동대문구', 37.57442300, 127.03975200, NULL),
      (199, NULL, 11, '메조 소프라노 사비나 김 독창회 [My Journey]', '2026-06-11', '2026-07-06', '19:30', '전석 20,000원', '7세 이상 관람 가능', 'https://www.sejongpac.or.kr/portal/performance/performance/performTicket.do?performIdx=37327', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=da74e1585f4a49c88e7b635ba48c448d', '메조 소프라노 사비나 김 독창회 [My Journey]', 'SEOUL_API', 'EV_199', NOW(), NOW(), '종로구', 37.57244300, 126.97561500, NULL), -- 세종문화회관 기준
      (200, NULL, 5, '[강남문화재단] 서울문화예술교육 [개인의 장소 : 공공의 지도]', '2026-06-11', '2026-10-10', '상세내용 참조', '무료', '초등 3~6학년 / 중장년층', 'https://www.gangnam.go.kr/office/gfac/board/gfac_notice/623/view.do?mid=gfac_notice01', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=9cb872ba317f4fca8a11184d626ec498', '[강남문화재단] 서울문화예술교육 [개인의 장소 : 공공의 지도]', 'SEOUL_API', 'EV_200', NOW(), NOW(), '강남구', 37.49651500, 127.06208300, NULL),
      (201, NULL, 10, '[세종문화회관] 연극 [오이디푸스]', '2026-06-11', '2026-08-23', '홈페이지 참조', 'R석 90,000원 S석 70,000원', '13세 이상 관람 가능', 'https://www.sejongpac.or.kr/portal/performance/performance/performTicket.do?performIdx=37291', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=dab401cd01bf487d9b538099e996a3f7', '[세종문화회관] 연극 [오이디푸스]', 'SEOUL_API', 'EV_201', NOW(), NOW(), '종로구', 37.57244300, 126.97561500, NULL),
      (202, NULL, 16, '[관악문화재단] FOUR PIANOS [앙상블 클라비어 x 피아노 오케스트라]', '2026-06-11', '2026-07-04', '17:00', '10,000원', '초등학생 이상', 'https://gfac.or.kr/site/main/performance/PERFORMANCE/view/316', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=341268155fc24a298f293dc01259e86d', '[관악문화재단] FOUR PIANOS [앙상블 클라비어 x 피아노 오케스트라]', 'SEOUL_API', 'EV_202', NOW(), NOW(), '관악구', 37.48316700, 126.92477300, NULL),
      (203, NULL, 9, '[동대문문화재단] 2026 생생 국가유산 사업 [지구를 지키는 선농마켓]', '2026-06-11', '2026-07-04', '11:00 ~ 16:00', '무료', '누구나', 'https://naver.me/FSvKJtSl', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=675779ba4ecb4415bd59e692e0aab174', '[동대문문화재단] 2026 생생 국가유산 사업 [지구를 지키는 선농마켓]', 'SEOUL_API', 'EV_203', NOW(), NOW(), '동대문구', 37.57723400, 127.03847700, NULL), -- 선농단 기준
      (204, NULL, 16, '제3회 크레아 트리오 정기연주회', '2026-06-11', '2026-07-04', '19:30', '전석 30,000원', '7세 이상 관람 가능', 'https://www.sejongpac.or.kr/portal/performance/performance/performTicket.do?performIdx=37250', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=b1732335cea443daa632d4193ac4cd68', '제3회 크레아 트리오 정기연주회', 'SEOUL_API', 'EV_204', NOW(), NOW(), '종로구', 37.57244300, 126.97561500, NULL),
      (205, NULL, 8, '[세종문화회관] 26세종시즌 [실내악 시리즈 일노래]', '2026-06-11', '2026-07-03', '19:30', '전석 30,000원', '7세 이상 관람 가능', 'https://www.sejongpac.or.kr/portal/performance/performance/performTicket.do?performIdx=36782', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=0793d2aadef54c7491b2d3a41b363a2c', '[세종문화회관] 26세종시즌 [실내악 시리즈 일노래]', 'SEOUL_API', 'EV_205', NOW(), NOW(), '종로구', 37.57244300, 126.97561500, NULL),
      (206, 101, 16, '[마포문화재단] 제11회 M 클래식 축제 [한국가곡의 밤]', '2026-06-11', '2026-07-03', '19:30', '전석 1만원(균일가)', '8세 이상', 'https://www.mfac.or.kr/performance/whole_view.jsp?sc_b_category=17&sc_b_code=BOARD_1207683401&pk_seq=2647', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=37101e6a34c1453c85c38b22112fce54', '[마포문화재단] 제11회 M 클래식 축제 [한국가곡의 밤]', 'SEOUL_API', 'EV_206', NOW(), NOW(), '마포구', 37.54992400, 126.94541700, NULL),
      (207, 100, 16, '[영등포문화재단] 마티네콘서트 With 금난새 #2.엘가', '2026-06-11', '2026-07-02', '11:00', '전석 15,000원', '초등학생(2019년생 포함) 이상', 'https://www.ydpcf.or.kr/artexhibit/view.do?id=536', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=bd32b9e30c3d408796fb585e5ab20771', '[영등포문화재단] 마티네콘서트 With 금난새 #2.엘가', 'SEOUL_API', 'EV_207', NOW(), NOW(), '영등포구', 37.52554700, 126.89670000, NULL),
      (208, 101, 13, '[마포문화재단] 제11회 M 클래식 축제 [한여름 밤의 뮤지컬]', '2026-06-11', '2026-07-02', '19:30', '전석 1만원(균일가)', '8세 이상', 'https://www.mfac.or.kr/performance/whole_view.jsp?sc_b_category=17&sc_b_code=BOARD_1207683401&pk_seq=2646', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=6b3cfe1c0efc4994b0508fbfbf535078', '[마포문화재단] 제11회 M 클래식 축제 [한여름 밤의 뮤지컬]', 'SEOUL_API', 'EV_208', NOW(), NOW(), '마포구', 37.54992400, 126.94541700, NULL),
      (209, 101, 5, '[마포구립서강도서관] 7월/영유아 [말놀이 잘잘잘]', '2026-06-11', '2026-07-25', '프로그램별 상이', '무료', '24개월~ 만 6세 영유아와 양육자', 'https://mplib.mapo.go.kr/sglib/PGM3028/eventDetail.do?eventSn=12688', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=009aaef4a1724e6580860635728b0306', '[마포구립서강도서관] 7월/영유아 [말놀이 잘잘잘]', 'SEOUL_API', 'EV_209', NOW(), NOW(), '마포구', 37.54948000, 126.93122100, NULL),
      (210, NULL, 7, '해외바이어가 직접 찾는 글로벌 전시회 [2026 인터참코리아] InterCHARM Korea', '2026-06-11', '2026-07-03', '10:00~17:00', '현장등록: 20,000원', '뷰티산업 종사자', 'https://www.intercharmkorea.com/ko-kr.html', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=5f2a4e2e793e49b987e5902a6e58cdf7', '해외바이어가 직접 찾는 글로벌 전시회 [2026 인터참코리아]', 'SEOUL_API', 'EV_210', NOW(), NOW(), '강남구', 37.51139400, 127.05915000, NULL),
      (211, NULL, 16, '[서초문화재단 x 한국하프시코드협회 공동기획] ''건반 음악의 이정표를 찾아'' 시리즈 1 [마디가 없는 프렐류드]', '2026-06-11', '2026-07-01', '수 19:30', 'R석 2만원 S석 1만5천원', '초등학생 이상 관람가능', 'https://www.seochocf.or.kr/site/main/seocho/show/view?show_idx=968', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=b81d94d7bb034a5e84be18a81b345f33', '[서초문화재단 x 한국하프시코드협회 공동기획]', 'SEOUL_API', 'EV_211', NOW(), NOW(), '서초구', 37.48321000, 127.01254300, NULL), -- 반포심산아트홀/서초문화재단 기준
      (212, NULL, 11, '한동연 첼로 독주회', '2026-06-11', '2026-07-01', '19:30', '전석 30,000원', '8세 이상 관람 가능', 'https://www.sejongpac.or.kr/portal/performance/performance/performTicket.do?performIdx=37329', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=dbfaf2925e72467aa8fff83a6be652ae', '한동연 첼로 독주회', 'SEOUL_API', 'EV_212', NOW(), NOW(), '종로구', 37.57244300, 126.97561500, NULL),
      (213, 101, 16, '[마포문화재단] 제11회 M 클래식 축제 [불멸의 오페라]', '2026-06-11', '2026-07-01', '19:30', '전석 1만원(균일가)', '8세 이상', 'https://www.mfac.or.kr/performance/whole_view.jsp?sc_b_category=17&sc_b_code=BOARD_1207683401&pk_seq=2645', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=6261f3a8b4294d2598029e680bc12a80', '[마포문화재단] 제11회 M 클래식 축제 [불멸의 오페라]', 'SEOUL_API', 'EV_213', NOW(), NOW(), '마포구', 37.54992400, 126.94541700, NULL),
      (214, NULL, 16, '[GS아트센터] 양인모 X 김치앤칩스', '2026-06-11', '2026-06-30', '19:30', 'R 11만원, S 8만원', '7세 이상', 'https://www.gsartscenter.com/program/detail/634', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=344dc2e128c94b82977c28aab4a1381d', '[GS아트센터] 양인모 X 김치앤칩스', 'SEOUL_API', 'EV_214', NOW(), NOW(), '강남구', 37.50481000, 127.03784100, NULL), -- 강남 역삼 역아트센터 가상매핑 (LG아트센터 등 고려)
      (215, NULL, 5, '2026 아동·청소년 도박문제예방교육 포럼', '2026-06-11', '2026-06-30', '13:30 ~ 16:30', '무료', '교사, 학부모, 시민 등', 'https://www.goodneighbors.kr/story/notice/2271/view.gn', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=f16efb6d99874eff865a51e08027ad2d', '2026 아동·청소년 도박문제예방교육 포럼', 'SEOUL_API', 'EV_215', NOW(), NOW(), '영등포구', 37.53032100, 126.92143500, NULL), -- 굿네이버스 회관 기준
      (216, NULL, 8, '서은영 가야금 독주회 공력 Ⅴ [철가락 - 이태백류 철가야금산조 전바탕]', '2026-06-11', '2026-06-30', '19:30', '전석 20,000원', '초등이상', 'https://www.gugak.go.kr/site/program/performance/detail?menuid=001001001&performance_id=386681', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=17c62f97c2d24df99cca56e992783b25', '서은영 가야금 독주회 공력 Ⅴ', 'SEOUL_API', 'EV_216', NOW(), NOW(), '서초구', 37.47871300, 127.00974300, NULL), -- 국립국악원 기준
      (217, NULL, 5, '[연세대학교 미래교육원] 2026학년도 여름학기 교육과정 안내', '2026-06-11', '2026-08-21', '프로그램별 상이', '프로그램별 상이', '성인 이상 누구나', 'https://go.yonsei.ac.kr/fro_end/html/dep_04/4100.php', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=904c46dc65f744adb7856a32e8b1c875', '[연세대학교 미래교육원] 여름학기 교육과정 안내', 'SEOUL_API', 'EV_217', NOW(), NOW(), '서대문구', 37.56578400, 126.93854100, NULL),
      (218, NULL, 11, '소프라노 성윤주 독창회', '2026-06-11', '2026-06-29', '19:30', 'R석 200,000원, S석 100,000원', '초등학생 이상 관람 가능', 'https://www.sejongpac.or.kr/portal/performance/performance/performTicket.do?performIdx=37319', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=1b82d4f0f0ac4f2887e05159155a5cdb', '소프라노 성윤주 독창회', 'SEOUL_API', 'EV_218', NOW(), NOW(), '종로구', 37.57244300, 126.97561500, NULL),
      (219, NULL, 1, '[GS아트센터] 다미앵 잘레 X 코헤이 나와 [프리즘]', '2026-06-11', '2026-06-28', '15:00 / 19:00', '전석 50,000원', '14세 이상', 'https://www.gsartscenter.com/program/detail/562', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=7ad9d266674546dfa5fcdeb47fc42b02', '[GS아트센터] 다미앵 잘레 X 코헤이 나와 [프리즘]', 'SEOUL_API', 'EV_219', NOW(), NOW(), '강남구', 37.50481000, 127.03784100, NULL),
      (220, NULL, 1, '[GS아트센터] NDT 1 - 필름 스크리닝 [미스트]', '2026-06-11', '2026-06-28', '17:00', '전석 20,000', '14세 이상', 'https://www.gsartscenter.com/program/detail/595', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=d3b7aaac258d4d63b74178084bc4bb88', '[GS아트센터] NDT 1 - 필름 스크리닝 [미스트]', 'SEOUL_API', 'EV_220', NOW(), NOW(), '강남구', 37.50481000, 127.03784100, NULL),
      (221, NULL, 5, '[관악문화재단] 2026 꿈다락문화예술학교 [관악 어린이 건축가들]', '2026-06-11', '2026-07-26', '매주 토,일 상이', '무료', '어린이,청소년', 'https://gfac.or.kr/site/main/archive/post/2026-꿈다락', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=293143a326ea47a28208d2693201d497', '[관악문화재단] 관악 어린이 건축가들 참여자 모집', 'SEOUL_API', 'EV_221', NOW(), NOW(), '관악구', 37.48316700, 126.92477300, NULL),
      (222, NULL, 14, '더 아카이브: 뮤지컬의 순간들', '2026-06-11', '2026-06-28', '17:00', 'R석 8만원 S석 5만원', '초등학생 이상', 'https://www.nmf.or.kr/user/performance/plan_view.do?show_id=S20260430160755925100', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=a3f308fab6c1405c942a53078c77caee', '더 아카이브: 뮤지컬의 순간들', 'SEOUL_API', 'EV_222', NOW(), NOW(), '중구', 37.56041200, 126.99214300, NULL), -- 국립극장 기준
      (223, NULL, 13, '[동작문화재단] 본동어울마당 아트홀 ''본동세레나데'' [제이에스브라스 콜렉티브의 쇼마칭브라스밴드]', '2026-06-11', '2026-06-27', '14:00 ~ 17:00', '무료', '누구나', 'https://www.idfac.or.kr/bbs/board.php?bo_table=gallery_info&wr_id=276', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=d6f03db9466047f7beb8d3fbe563ac7c', '[동작문화재단] 본동세레나데 쇼마칭 브라스밴드', 'SEOUL_API', 'EV_223', NOW(), NOW(), '동작구', 37.51123400, 126.95432100, NULL),
      (224, NULL, 11, '황소원 피아노 독주회', '2026-06-11', '2026-06-27', '20:00', '전석 30,000원', '미취학아동 입장불가', 'https://www.sac.or.kr/site/main/show/show_view?SN=76591', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=f41755b118264fcaa0a479eb2ebc1f17', '황소원 피아노 독주회', 'SEOUL_API', 'EV_224', NOW(), NOW(), '서초구', 37.47854100, 127.01183200, NULL), -- 예술의전당 기준
      (225, NULL, 5, '[서울문화예술교육센터 용산] 다정한 아트라운지 [안녕을 나누는 사이 ― 글과 만난 공간]', '2026-06-11', '2026-06-27', '14:00 ~ 15:30', '무료', '일반 성인 100명', 'https://www.sfac.or.kr/asa/edu/view.do?eduMstSeq=37059', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=edd98d719aa442b496c890d4349dd280', '[서울문화예술교육센터 용산] 다정한 아트라운지', 'SEOUL_API', 'EV_225', NOW(), NOW(), '용산구', 37.53215200, 126.96781200, NULL),
      (226, NULL, 5, '[서울도서관] 초등3-4학년 [어린이 디지털 리터러시 교육: 디지털 두드림]', '2026-06-11', '2026-06-27', '14:00 ~ 16:00', '무료', '초등학교 3-4학년', 'https://lib.seoul.go.kr/lecture/applyDetail/6961', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=a0a0da7d973545dea342b57e1fc32033', '[서울도서관] 어린이 디지털 리터러시 교육', 'SEOUL_API', 'EV_226', NOW(), NOW(), '중구', 37.56658500, 126.97817500, NULL), -- 서울시청(서울도서관) 기준
      (227, NULL, 14, '[서울갤러리] 유월의 설렘, 유얼 스테이지 [싱어송라이터 무이야드]', '2026-06-11', '2026-06-27', '14:00', '무료', '누구나', 'https://www.seoul.go.kr/seoulgallery/www/program/view.do?key=2509020016&prgmSn=84', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=ef921ed2258941099aadf2e63fa57223', '[서울갤러리] 싱어송라이터 무이야드 공연', 'SEOUL_API', 'EV_227', NOW(), NOW(), '중구', 37.56658500, 126.97817500, NULL),
      (228, 100, 14, '[영등포아트스퀘어] 배우 문정희 북콘서트 [금빛 동행, 마누 이야기]', '2026-06-11', '2026-06-27', '19:00', '무료', '누구나', 'https://ydpartsquare.com/프로그램-및-예약/view/6147037', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=879948e424604b6fa1eabd6aa853ac99', '[영등포아트스퀘어] 배우 문정희 북콘서트', 'SEOUL_API', 'EV_228', NOW(), NOW(), '영등포구', 37.51712400, 126.90423100, NULL), -- 타임스퀘어 영등포아트스퀘어 기준
      (229, NULL, 8, '[관악문화재단] 음악공장 노올량 [플레이리스트 : 세대별 아리랑]', '2026-06-11', '2026-06-27', '16:00', '15,000원', '누구나', 'https://gfac.or.kr/site/main/performance/PERFORMANCE/view/313', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=264e220c83bc49a292d54dd9adde57ef', '[관악문화재단] 음악공장 노올량 국악공연', 'SEOUL_API', 'EV_229', NOW(), NOW(), '관악구', 37.48316700, 126.92477300, NULL),
      (230, NULL, 16, '[살롱클래식] 서린 듀오 - 한 여름의 서사', '2026-06-11', '2026-06-27', '17:00', '전석 30,000', '누구나', 'https://www.rayularthall.com/concerts/', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=686ac975cd604b599a175847bebf33d4', '[살롱클래식] 서린 듀오 클래식 연주회', 'SEOUL_API', 'EV_230', NOW(), NOW(), '강남구', 37.51423100, 127.03541200, NULL), -- 압구정/신사 인근 클래식홀 가상매핑
      (231, NULL, 16, 'Performance by EPT [溫(온)] - 창단 10주년 기념 연주회', '2026-06-11', '2026-06-27', '17:00', '전석 30,000원', '7세 이상 관람 가능', 'https://www.sejongpac.or.kr/portal/performance/performance/performTicket.do?performIdx=37252', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=44b6d78b62214272a9c0abca9ba274db', 'Performance by EPT 10주년 연주회', 'SEOUL_API', 'EV_231', NOW(), NOW(), '종로구', 37.57244300, 126.97561500, NULL),
      (232, NULL, 10, '한일공동창작극 [조세이탄광 - 살고 싶었다]', '2026-06-11', '2026-06-28', '주말 상이 15:00', '전석 40,000원', '누구나', 'https://www.instagram.com/p/DYVuCBOj5Il/', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=39ab0ba0a6e440d3ac798e5cfee15081', '한일공동창작극 [조세이탄광]', 'SEOUL_API', 'EV_232', NOW(), NOW(), '종로구', 37.58221000, 127.00194400, NULL),
      (233, NULL, 1, '2026년 제2회 서울브라보발레페스티벌(SEOUL BRAVO BALLET FESTIVAL)', '2026-06-11', '2026-06-28', '일정별 상이', '전석 30,000원', '초등학생 이상 관람가능', 'https://www.koreaballet.or.kr', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=eba2adadb07b4e58b7c0b81608c3b5a7', '제2회 서울브라보발레페스티벌', 'SEOUL_API', 'EV_233', NOW(), NOW(), '양천구', 37.52735100, 126.87415200, NULL), -- 양천문화회관 등 발레공연장 가상매핑
      (234, NULL, 10, '[서울문화예술교육센터 강북] 공연 ''라이브씨어터'' [단편소설극장]', '2026-06-11', '2026-06-27', '금 19:30 / 토 14:00', '무료', '중등 1학년 이상', 'https://www.sfac.or.kr/asa/edu/view.do?eduMstSeq=36839', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=e37e6edcb88c497fabc1e219ad1e8758', '[서울문화예술교육센터 강북] 단편소설극장 공연', 'SEOUL_API', 'EV_234', NOW(), NOW(), '강북구', 37.62174300, 127.04272100, NULL),
      (235, NULL, 13, '[동작문화재단] 2026 까망돌어울마당 아트홀 공연실황 스크린 ON-AIR 5회차 [춘향탈옥] 2부', '2026-06-11', '2026-06-26', '19:00', '무료', '제한 없음', 'https://www.idfac.or.kr/bbs/board.php?bo_table=gallery_info&wr_id=272', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=0a6230768fb641c280971b270a6b78ae', '까망돌어울마당 아트홀 스크린 상영회', 'SEOUL_API', 'EV_235', NOW(), NOW(), '동작구', 37.50742100, 126.95874100, NULL), -- 까망돌도서관/아트홀 기준
      (236, NULL, 16, '더멘즈콰이어남성합창단 제18회 정기연주회', '2026-06-11', '2026-06-26', '19:30 ~ 21:00', 'R석 150,000원 등', '성인, 청소년, 어린이', 'https://www.lotteconcerthall.com/product/ko/performance/261106', 'https://culture.seoul.go.kr/cmmn/file/getImage.do?atchFileId=67ce3d9bf2df4508923d4da9525474d9', '더멘즈콰이어 남성합창단 연주회', 'SEOUL_API', 'EV_236', NOW(), NOW(), '송파구', 37.51375200, 127.10444500, NULL); -- 롯데콘서트홀 기준


ALTER TABLE `category` ALTER COLUMN `id` RESTART WITH 100;
ALTER TABLE `member` ALTER COLUMN `id` RESTART WITH 100;
ALTER TABLE `user_preference` ALTER COLUMN `id` RESTART WITH 100;
ALTER TABLE `user_preference_category` ALTER COLUMN `id` RESTART WITH 100;
ALTER TABLE `place` ALTER COLUMN `id` RESTART WITH 1000;
ALTER TABLE `event` ALTER COLUMN `id` RESTART WITH 1000;