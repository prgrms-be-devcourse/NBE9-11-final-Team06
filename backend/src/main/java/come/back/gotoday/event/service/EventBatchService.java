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
            //만료 행사 삭제
            eventExpiredCleaner.cleanExpiredEvents();

            // Key: 카테고리 이름 (예: "클래식", "뮤지컬/오페라"), Value: Category 엔티티 객체
            Map<String, Category> categoryMap = prepareCategoryMap();

            // 기본 카테고리 지정 (API 분류가 우리 DB에 없을 경우 대비용 - 대안책)
            Category defaultCategory = getDefaultCategory(categoryMap);

            SeoulEventResponse response = apiClient.fetchEvents(startIndex, endIndex);
            if (response == null || response.culturalEventInfo() == null) {
                log.warn("새벽 배치: 가져올 데이터가 없습니다.");
                return;
            }

            totalCount = response.culturalEventInfo().listTotalCount();
            eventChunkProcessor.saveOrUpdateChunk(response.culturalEventInfo().row(), categoryMap, defaultCategory);

            while (endIndex < totalCount) {
                startIndex += 1000;
                endIndex += 1000;

                Thread.sleep(500);

                SeoulEventResponse nextResponse = apiClient.fetchEvents(startIndex, endIndex);
                if (nextResponse != null && nextResponse.culturalEventInfo() != null) {
                    List<SeoulEventResponse.EventRow> rows = nextResponse.culturalEventInfo().row();

                    if (rows == null || rows.isEmpty()) {
                        log.info("더 이상 가져올 데이터가 없어 배치를 종료합니다.");
                        break;
                    }

                    eventChunkProcessor.saveOrUpdateChunk(rows, categoryMap, defaultCategory);
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
        return allCategories.stream()
                .collect(Collectors.toMap(
                        Category::getName,
                        category -> category,
                        (existing, replacement) -> existing
                ));
    }

    private Category getDefaultCategory(Map<String, Category> categoryMap) {
        return categoryMap.values().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기본 카테고리가 데이터베이스에 존재하지 않습니다."));
    }

}