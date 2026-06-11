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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventChunkProcessor {

    private final EventRepository eventRepository;

    @Transactional
    public void saveOrUpdateChunk(List<SeoulEventResponse.EventRow> rows,
                                  Map<String, Category> categoryMap,
                                  Category defaultCategory) {
        if (rows == null || rows.isEmpty()) return;

        // ✨ 기준이 되는 당일 날짜 구하기
        LocalDate today = LocalDate.now();

        for (SeoulEventResponse.EventRow row : rows) {
            try {
                // 1. 필수값 검증 및 externalId 추출
                String extId = row.externalId();
                if (extId.replace("_", "").isBlank() || row.title() == null) {
                    log.warn("필수 데이터 누락으로 스킵: TITLE={}", row.title());
                    continue;
                }

                // ✨ 2. 과거 이벤트 필터링 로직 추가
                LocalDate endDate = parseDate(row.endDate());
                if (endDate.isBefore(today)) {
                    // 행사 종료일이 오늘보다 이전이면 DB 조회도 하지 않고 바로 스킵 (성능 최적화)
                    log.debug("지난 이벤트 스킵: TITLE={}, END_DATE={}", row.title(), endDate);
                    continue;
                }

                // 3. 기존 행사 조회 (여기서부터는 오늘 포함 미래에 진행할 행사만 도달함)
                Optional<Event> existingEvent = eventRepository.findByExternalId(extId);

                if (existingEvent.isPresent()) {
                    // Update
                    Event event = existingEvent.get();
                    LocalDate startDate = parseDate(row.startDate());

                    if (event.isChanged(row.title(), startDate, endDate, row.orgLink(), row.mainImg())) {
                        event.updateInfo(
                                row.title(),
                                startDate,
                                endDate,
                                row.orgLink(),
                                row.mainImg()
                        );
                        log.info("행사 정보 변경 감지 - 업데이트 수행: TITLE={}", row.title());
                    } else {
                        log.debug("행사 정보 일치 - 업데이트 스킵: TITLE={}", row.title());
                    }
                } else {
                    // Insert
                    LocalDate startDate = parseDate(row.startDate());
                    Category targetCategory = categoryMap.getOrDefault(row.codeName(), defaultCategory);

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
                }
            } catch (Exception e) {
                log.error("행사 데이터 저장 중 오류 스킵 (제목: {}): {}", row.title(), e.getMessage());
            }
        }
    }

    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.isBlank()) return LocalDate.now();
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            log.warn("날짜 파싱 실패 [{}], 오늘 날짜로 대체합니다.", dateStr);
            return LocalDate.now(); // 파싱 에러 시 오늘 날짜로 대입하여 일단 필터링을 통과하도록 유도
        }
    }
}
