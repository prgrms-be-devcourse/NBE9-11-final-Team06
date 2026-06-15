package come.back.gotoday.event.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.enums.EventSource;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import come.back.gotoday.recommend.engine.VectorEmbeddingEngine;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventChunkProcessor {

    private final EventRepository eventRepository;
    private final VectorEmbeddingEngine vectorEngine;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;


    public void saveOrUpdateChunk(List<SeoulEventResponse.EventRow> rows,
                                  Map<String, Category> categoryMap,
                                  Category defaultCategory) {
        if (rows == null || rows.isEmpty()) return;
        log.info(" [배치 처리] 이번 청크로 들어온 데이터 개수: {}", rows.size());

        // 1. 청크(1000개)에 포함된 모든 externalId를 리스트로 모읍니다.
        List<String> extIds = rows.stream().map(SeoulEventResponse.EventRow::externalId).toList();
        List<Event> existingEvents = eventRepository.findByExternalIdIn(extIds);
        Map<String, Event> eventMap = existingEvents.stream().collect(Collectors.toMap(Event::getExternalId, event -> event));
        LocalDate today = LocalDate.now();

        //  네트워크 성능 최적화를 위해 이번 청크에서 실제로 '임베딩 추출이 필요한 대상'만 임시 리스트에 수집합니다.
        List<SeoulEventResponse.EventRow> validRows = new ArrayList<>();
        List<String> textsToEmbed = new ArrayList<>();

        for (SeoulEventResponse.EventRow row : rows) {
            String extId = row.externalId();
            if (extId.replace("_", "").isBlank() || row.title() == null) {
                log.warn("필수 데이터 누락으로 스킵: TITLE={}", row.title());
                continue;
            }

            LocalDate startDate = parseDate(row.startDate());
            LocalDate endDate = parseDate(row.endDate());
            if (startDate == null || endDate == null) {
                log.warn("필수 날짜 데이터 누락 또는 파싱 실패로 스킵: TITLE={}, START={}, END={}", row.title(), row.startDate(), row.endDate());
                continue;
            }

            if (endDate.isBefore(today)) {
                // 행사 종료일이 오늘보다 이전이면 DB 조회도 하지 않고 바로 스킵 (성능 최적화)
                log.debug("지난 이벤트 스킵: TITLE={}, END_DATE={}", row.title(), endDate);
                continue;
            }

            Event existingEvent = eventMap.get(extId);
            String docText = String.format(
                    "[지역: %s] [카테고리: %s] [타겟/대상: %s] [행사명: %s]",
                    row.guName(),
                    row.codeName(),
                    row.useTrgt(),
                    row.title()
            );
            log.info("생성된 docText 확인: ID={}, TEXT={}", row.externalId(), docText);

            if (existingEvent != null) {
                // 업데이트 대상인데 데이터가 변경되었을 때만 벡터 추출 대상에 포함
                if (existingEvent.isChanged(row.title(), startDate, endDate, row.orgLink(), row.mainImg(), row.lat(), row.lot())) {
                    validRows.add(row);
                    textsToEmbed.add(docText);
                }
            } else {
                // 신규 저장(Insert) 대상은 무조건 벡터 추출 대상에 포함
                validRows.add(row);
                textsToEmbed.add(docText);
            }
        }

        log.info(" [배치 처리] 실제 허깅페이스로 보낼 텍스트 개수: {}", textsToEmbed.size());
        // 수집된 문장 리스트가 있다면 허깅페이스 API를 단 1번만 호출하여 대량으로 벡터를 가져옵니다.
        List<float[]> batchVectors = vectorEngine.getEmbeddings(textsToEmbed);

        // 실제 DB 저장/수정 작업만 트랜잭션 내부에서 처리
        transactionTemplate.executeWithoutResult(status -> {
            // 가져온 벡터들을 순서대로 매핑하며 실제 DB DB 작업을 처리합니다. (순서가 정확히 일치하므로 index로 매핑 가능)
            for (int i = 0; i < validRows.size(); i++) {
                try {
                    SeoulEventResponse.EventRow row = validRows.get(i);
                    float[] embeddingVector = batchVectors.get(i); // 매핑된 벡터 꺼내기

                    String extId = row.externalId();
                    LocalDate startDate = parseDate(row.startDate());
                    LocalDate endDate = parseDate(row.endDate());
                    Event existingEvent = eventMap.get(extId);

                    if (existingEvent != null) {
                        // 진짜 업데이트 수행
                        existingEvent.updateInfo(row.title(), startDate, endDate, row.orgLink(), row.mainImg(), row.lat(), row.lot());
                        existingEvent.setEmbeddingVector(embeddingVector);
                        log.info("행사 정보 변경 감지 - 업데이트 수행: TITLE={}", row.title());
                    } else {
                        // 진짜 인서트 수행
                        //todo 카카오맵 api가 연동되면 행사의 위치를 카카오 맵에서 찾아서 place객체를 만들고 event에 넣어주는 작업 필요
                        Category targetCategory = categoryMap.getOrDefault(row.codeName(), defaultCategory);
                        Event newEvent = Event.create(
                                null, targetCategory, row.title(), startDate, endDate,
                                row.eventTime(), row.useFee(), row.useTrgt(), row.orgLink(), row.mainImg(),
                                null, EventSource.SEOUL_API, extId, embeddingVector, row.guName()
                                ,row.lat(), row.lot()
                        );
                        eventRepository.save(newEvent);
                    }
                } catch (Exception e) {
                    log.error("행사 데이터 처리 중 오류 스킵: {}", e.getMessage());
                }
            }
        });
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
