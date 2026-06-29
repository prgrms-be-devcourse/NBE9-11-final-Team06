

-- 목적: source + external_id 기준으로 중복된 Place를 정리하고 복합 유니크 제약을 추가한다.
-- 실행 전 주의사항
-- 1. 백엔드 서버를 중지한 뒤 실행한다.
-- 2. 반드시 하나의 DB 세션/SQL 콘솔에서 위에서 아래 순서대로 실행한다.
-- 3. 실행 전 DB 백업 또는 스냅샷을 확보한다.
-- 4. 기존 place 데이터 중 external_id가 NULL인 데이터는 정리 대상에서 제외한다.

DROP TEMPORARY TABLE IF EXISTS place_duplicate_map;

-- 같은 source + external_id 조합에서는 가장 작은 place.id를 대표 Place로 유지한다.
CREATE TEMPORARY TABLE place_duplicate_map AS
SELECT
    p.id AS duplicate_id,
    grouped.keep_id
FROM place p
JOIN (
    SELECT
        source,
        external_id,
        MIN(id) AS keep_id
    FROM place
    WHERE external_id IS NOT NULL
    GROUP BY source, external_id
    HAVING COUNT(*) > 1
) grouped
    ON grouped.source = p.source
   AND grouped.external_id = p.external_id
WHERE p.id <> grouped.keep_id;

ALTER TABLE place_duplicate_map
    ADD PRIMARY KEY (duplicate_id),
    ADD INDEX idx_keep_id (keep_id);

-- 중복 Place를 참조하는 데이터를 대표 Place로 연결한다.
UPDATE course_place cp
JOIN (
    SELECT duplicate_id, keep_id
    FROM place_duplicate_map
) m
    ON cp.place_id = m.duplicate_id
SET cp.place_id = m.keep_id;

UPDATE crowd_status cs
JOIN (
    SELECT duplicate_id, keep_id
    FROM place_duplicate_map
) m
    ON cs.place_id = m.duplicate_id
SET cs.place_id = m.keep_id;

UPDATE event e
JOIN (
    SELECT duplicate_id, keep_id
    FROM place_duplicate_map
) m
    ON e.place_id = m.duplicate_id
SET e.place_id = m.keep_id;

-- 참조가 남아 있으면 아래 SELECT 결과가 모두 0인지 확인한 뒤 DELETE를 실행한다.
SELECT COUNT(*) AS remaining_course_place_references
FROM course_place cp
JOIN (
    SELECT duplicate_id
    FROM place_duplicate_map
) m
    ON cp.place_id = m.duplicate_id;

SELECT COUNT(*) AS remaining_crowd_status_references
FROM crowd_status cs
JOIN (
    SELECT duplicate_id
    FROM place_duplicate_map
) m
    ON cs.place_id = m.duplicate_id;

SELECT COUNT(*) AS remaining_event_references
FROM event e
JOIN (
    SELECT duplicate_id
    FROM place_duplicate_map
) m
    ON e.place_id = m.duplicate_id;

-- 위 세 조회 결과가 모두 0인 것을 확인한 후 실행한다.
DELETE p
FROM place p
JOIN (
    SELECT duplicate_id
    FROM place_duplicate_map
) m
    ON p.id = m.duplicate_id;

-- 중복 데이터가 모두 제거됐는지 확인한다.
SELECT
    source,
    external_id,
    COUNT(*) AS duplicate_count
FROM place
WHERE external_id IS NOT NULL
GROUP BY source, external_id
HAVING COUNT(*) > 1;

-- 위 조회 결과가 0건인 것을 확인한 후 실행한다.
ALTER TABLE place
    ADD CONSTRAINT uk_place_source_external_id
    UNIQUE (source, external_id);

-- 제약 적용 확인
SHOW CREATE TABLE place;