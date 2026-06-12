package come.back.gotoday.event.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.external.seoul.api_client.SeoulEventApiClient;
import come.back.gotoday.external.seoul.dto.SeoulEventResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBatchService {

    private final SeoulEventApiClient apiClient;
    private final EventChunkProcessor eventChunkProcessor;
    private final CategoryRepository categoryRepository;
    private final EventExpiredCleaner eventExpiredCleaner;

    public void syncSeoulEvents() {
        int startIndex = 1;
        int endIndex = 1000;
        int totalCount = 0;

        try {
            log.info("서울시 행사 배치 동기화 시작: startIndex={}, endIndex={}", startIndex, endIndex);

            //만료 행사 삭제
            eventExpiredCleaner.cleanExpiredEvents();
            log.info("만료 행사 정리 완료");

            // Key: 카테고리 이름 (예: "클래식", "뮤지컬/오페라"), Value: Category 엔티티 객체
            Map<String, Category> categoryMap = prepareCategoryMap();
            log.info("행사 카테고리 매핑 완료: categoryCount={}", categoryMap.size());

            // 기본 카테고리 지정 (API 분류가 우리 DB에 없을 경우 대비용 - 대안책)
            Category defaultCategory = getDefaultCategory(categoryMap);
            log.info("행사 기본 카테고리 설정 완료: categoryName={}", defaultCategory.getName());

            log.info("서울시 행사 API 최초 호출 시작: startIndex={}, endIndex={}", startIndex, endIndex);
            SeoulEventResponse response = apiClient.fetchEvents(startIndex, endIndex);
            if (response == null || response.culturalEventInfo() == null) {
                log.warn("새벽 배치: 가져올 데이터가 없습니다.");
                return;
            }

            totalCount = response.culturalEventInfo().listTotalCount();
            log.info("서울시 행사 API 최초 호출 완료: totalCount={}", totalCount);
            eventChunkProcessor.saveOrUpdateChunk(response.culturalEventInfo().row(), categoryMap, defaultCategory);
            log.info("서울시 행사 첫 번째 청크 저장 완료: startIndex={}, endIndex={}", startIndex, endIndex);

            while (endIndex < totalCount) {
                startIndex += 1000;
                endIndex += 1000;

                Thread.sleep(500);

                log.info("서울시 행사 API 추가 호출 시작: startIndex={}, endIndex={}", startIndex, endIndex);
                SeoulEventResponse nextResponse = apiClient.fetchEvents(startIndex, endIndex);
                if (nextResponse != null && nextResponse.culturalEventInfo() != null) {
                    List<SeoulEventResponse.EventRow> rows = nextResponse.culturalEventInfo().row();
                    log.info("서울시 행사 API 추가 호출 완료: startIndex={}, endIndex={}, rowCount={}", startIndex, endIndex, rows == null ? 0 : rows.size());

                    if (rows == null || rows.isEmpty()) {
                        log.info("더 이상 가져올 데이터가 없어 배치를 종료합니다.");
                        break;
                    }

                    eventChunkProcessor.saveOrUpdateChunk(rows, categoryMap, defaultCategory);
                    log.info("서울시 행사 추가 청크 저장 완료: startIndex={}, endIndex={}, rowCount={}", startIndex, endIndex, rows.size());
                }
                else {
                    log.warn("서울시 행사 API 추가 호출 응답이 비어 있습니다. startIndex={}, endIndex={}", startIndex, endIndex);
                    break;
                }
            }
            log.info("새벽 배치 완료! 총 {}건 기준 동기화 프로세스 종료.", totalCount);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("배치 작업 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("새벽 배치 작업 중 치명적 오류 발생: ", e);
        }
    }

    private Map<String, Category> prepareCategoryMap() {
        List<Category> allCategories = categoryRepository.findAll();
        log.info("행사 카테고리 조회 완료: categoryCount={}", allCategories.size());
        return allCategories.stream()
                .collect(Collectors.toMap(
                        Category::getName,
                        category -> category,
                        (existing, replacement) -> existing
                ));
    }

    private Category getDefaultCategory(Map<String, Category> categoryMap) {
        return java.util.Optional.ofNullable(categoryMap.get("기타"))
                .or(() -> java.util.Optional.ofNullable(categoryMap.get("미분류")))
                .or(() -> categoryMap.values().stream().findFirst())
                .orElseThrow(() -> {
                    log.error("행사 기본 카테고리 설정 실패: 사용 가능한 카테고리가 없습니다.");
                    return new IllegalStateException("기본 카테고리가 데이터베이스에 존재하지 않습니다.");
                });
    }

}