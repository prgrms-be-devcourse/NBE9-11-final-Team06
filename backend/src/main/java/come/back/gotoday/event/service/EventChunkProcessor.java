package come.back.gotoday.event.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.enums.EventSource;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventChunkProcessor {

    private final EventRepository eventRepository;

    @Transactional
    public void saveOrUpdateChunk(List<SeoulEventResponse.EventRow> rows,
                                  Map<String, Category> categoryMap,
                                  Category defaultCategory) {
        if (rows == null || rows.isEmpty()) {
            log.info("행사 청크 저장 스킵: 처리할 데이터가 없습니다.");
            return;
        }

        log.info("행사 청크 저장 시작: rowCount={}", rows.size());

        // 1. 청크(1000개)에 포함된 모든 externalId를 리스트로 모읍니다.
        List<String> extIds = rows.stream()
                .map(SeoulEventResponse.EventRow::externalId)
                .filter(externalId -> externalId != null && !externalId.replace("_", "").isBlank())
                .toList();

        log.info("행사 청크 externalId 수집 완료: externalIdCount={}", extIds.size());

        List<Event> existingEvents = eventRepository.findByExternalIdIn(extIds);

        log.info("기존 행사 조회 완료: existingEventCount={}", existingEvents.size());

        Map<String, Event> eventMap = existingEvents.stream()
                .collect(Collectors.toMap(
                        Event::getExternalId,
                        event -> event,
                        (existing, replacement) -> existing
                ));

        // 기준이 되는 당일 날짜 구하기
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        int insertCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (SeoulEventResponse.EventRow row : rows) {
            try {
                // 1. 필수값 검증 및 externalId 추출
                String extId = row.externalId();
                if (extId == null || extId.replace("_", "").isBlank() || row.title() == null) {
                    skipCount++;
                    log.warn("필수 데이터 누락으로 행사 저장 스킵: externalId={}, title={}", extId, row.title());
                    continue;
                }

                // 2. 과거 이벤트 필터링 로직 추가
                LocalDate startDate = parseDate(row.startDate());
                LocalDate endDate = parseDate(row.endDate());

                if (startDate == null || endDate == null) {
                    skipCount++;
                    log.warn("필수 날짜 데이터 누락 또는 파싱 실패로 행사 저장 스킵: title={}, startDate={}, endDate={}", row.title(), row.startDate(), row.endDate());
                    continue;
                }

                if (endDate.isBefore(today)) {
                    // 행사 종료일이 오늘보다 이전이면 DB 조회도 하지 않고 바로 스킵 (성능 최적화)
                    skipCount++;
                    log.debug("지난 행사 저장 스킵: title={}, endDate={}", row.title(), endDate);
                    continue;
                }

                // 3. 기존 행사 조회 (여기서부터는 오늘 포함 미래에 진행할 행사만 도달함)
                Event existingEvent = eventMap.get(extId);

                if (existingEvent!=null) {
                    // Update

                    if (existingEvent.isChanged(row.title(), startDate, endDate, row.orgLink(), row.mainImg())) {
                        existingEvent.updateInfo(
                                row.title(),
                                startDate,
                                endDate,
                                row.orgLink(),
                                row.mainImg()
                        );
                        updateCount++;
                        log.info("행사 정보 변경 감지로 업데이트 수행: eventId={}, title={}", existingEvent.getId(), row.title());
                    } else {
                        skipCount++;
                        log.debug("행사 정보 일치로 업데이트 스킵: eventId={}, title={}", existingEvent.getId(), row.title());
                    }
                } else {
                    // Insert
                    Category targetCategory = categoryMap.getOrDefault(row.codeName(), defaultCategory);

                    if (!categoryMap.containsKey(row.codeName())) {
                        log.debug("행사 카테고리 매핑 실패로 기본 카테고리 사용: codeName={}, defaultCategory={}", row.codeName(), defaultCategory.getName());
                    }

                    //todo 카카오맵 api가 연동되면 행사의 위치를 카카오 맵에서 찾아서 place객체를 만들고 event에 넣어주는 작업 필요
//                    // 1. API가 준 장소 이름(예: 세종문화회관)을 가져옴
//                    String apiPlaceName = row.placeName();
//
//                    // 2. 우리 DB에 존재하는지 확인 (추측 방지용 레포지토리 조회 필요)
//                    Place targetPlace = placeRepository.findByName(apiPlaceName)
//                            .orElseGet(() -> {
//                                // 3.  DB에 장소가 없다면? 카카오 지도 API를 호출해서 상세 정보를 받아옴
//                                KakaoPlaceDto kakaoData = kakaoMapClient.searchPlaceByName(apiPlaceName);
//
//                                if (kakaoData != null) {
//                                    // 카카오가 준 깨끗한 데이터로 place 테이블에 신규 저장
//                                    Place newPlace = Place.create(
//                                            targetCategory.getId(), // category_id 매핑
//                                            apiPlaceName,           // name
//                                            kakaoData.getAddress(), // 주소 (API 한계 극복)
//                                            kakaoData.getRoadAddress(), // 도로명 주소
//                                            kakaoData.getLat(),     // 정밀 위도
//                                            kakaoData.getLng(),     // 정밀 경도
//                                            kakaoData.getPhone(),   // 전화번호
//                                            true                    // is_active 필수값 채움
//                                    );
//                                    return placeRepository.save(newPlace);
//                                }
//                                return null; // 지도에도 안 나오는 유령 장소면 null 처리
//                            });


                    Event newEvent = Event.create(
                            null,                // place (API 데이터에 없으므로 null)
                            targetCategory,       // category (프록시 객체)
                            row.title(),         // title
                            startDate,           // startDate
                            endDate,             // endDate
                            row.eventTime(),                // eventTime (스키마 대응 null)
                            row.useFee(),        // fee
                            row.useTrgt(),       // target
                            row.orgLink(),       // homepageUrl
                            row.mainImg(),       // imageUrl
                            null,                // description (스키마 대응 null)
                            EventSource.SEOUL_API,         // source
                            extId                // externalId
                    );

                    eventRepository.save(newEvent);
                    insertCount++;
                    log.debug("신규 행사 저장 완료: title={}, externalId={}", row.title(), extId);
                }
            } catch (Exception e) {
                errorCount++;
                log.error("행사 데이터 저장 중 오류 발생으로 스킵: title={}, message={}", row.title(), e.getMessage(), e);
            }
        }

        log.info(
                "행사 청크 저장 완료: rowCount={}, insertCount={}, updateCount={}, skipCount={}, errorCount={}",
                rows.size(),
                insertCount,
                updateCount,
                skipCount,
                errorCount
        );
    }

    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isBlank()) return null;
            return LocalDate.parse(dateStr.split(" ")[0]);
        } catch (Exception e) {
            log.warn("날짜 파싱 실패 [{}]: {}", dateStr, e.getMessage());
            return null;
        }
    }
}
