package come.back.gotoday.event.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.type.CategoryType;
import come.back.gotoday.event.dto.EventDetailResponse;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.enums.EventSource;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.global.exception.BusinessException;
import come.back.gotoday.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils; // 💡 필드 주입용 스프링 유틸 라이브러리

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @Nested
    @DisplayName("이벤트 상세 조회 비즈니스 로직")
    class GetEventLogic {

        @Test
        @DisplayName("[성공] 레포지토리에서 데이터를 찾으면 정상적으로 DTO로 변환하여 리턴한다")
        void success() {
            // given
            Long eventId = 185L;
            Long categoryId = 16L;
            Long placeId = 100L;

            // 1. 정적 팩토리 메서드로 객체 생성
            Category mockCategory = Category.create("클래식", CategoryType.EVENT);
            // ID는 ReflectionTestUtils를 통해 강제로 심어줍니다.
            ReflectionTestUtils.setField(mockCategory, "id", categoryId);

            Place mockPlace = Place.create(
                    mockCategory, "영등포아트홀", "주소", "도로명주소",
                    new BigDecimal("37.5255470"), new BigDecimal("126.8967000"),
                    "02-123-4567", "https://url.com", "설명", "SEOUL_API", "P_100", true
            );
            ReflectionTestUtils.setField(mockPlace, "id", placeId);

            Event mockEvent = Event.create(
                    mockPlace, mockCategory, "클래식", "금난새 콘서트",
                    LocalDate.of(2026, 6, 11), LocalDate.of(2026, 10, 15),
                    "11:00", "전석 15,000원", "초등학생 이상", "https://homepage.com", "https://image.com",
                    "상세설명", EventSource.SEOUL_API, "EV_185", new float[]{0.1f, 0.2f}, "영등포구", 37.525547, 126.8967
            );
            ReflectionTestUtils.setField(mockEvent, "id", eventId);

            // Mockito stubbing
            given(eventRepository.findByIdWithFetch(eventId)).willReturn(Optional.of(mockEvent));

            // when
            EventDetailResponse result = eventService.getEvent(eventId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(eventId);
            assertThat(result.title()).isEqualTo("금난새 콘서트");
            assertThat(result.categoryName()).isEqualTo("클래식");
            assertThat(result.eventCategory()).isEqualTo("클래식");
            assertThat(result.placeName()).isEqualTo("영등포아트홀");
        }

        @Test
        @DisplayName("[실패] 레포지토리 조회 결과가 비어있으면 EVENT_NOT_FOUND 비즈니스 예외를 던진다")
        void fail_notFound() {
            // given
            Long notFoundEventId = 999L;
            given(eventRepository.findByIdWithFetch(notFoundEventId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventService.getEvent(notFoundEventId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.EVENT_NOT_FOUND.getMessage());
        }
    }
}