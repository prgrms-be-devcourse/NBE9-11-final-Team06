package come.back.gotoday.course.service;

import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.course.repository.SavedCourseRepository;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.place.service.PlaceService;
import come.back.gotoday.recommend.service.RecommendationService;
import come.back.gotoday.tour.entity.Tour;
import come.back.gotoday.tour.repository.TourRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @InjectMocks
    private CourseService courseService;

    @Mock private CourseRepository courseRepository;
    @Mock private CoursePlaceRepository coursePlaceRepository;
    @Mock private SavedCourseRepository savedCourseRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private PlaceRepository placeRepository;
    @Mock private TourRepository tourRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private RecommendationService recommendationService;
    @Mock private EventRepository eventRepository;
    @Mock private KakaoLocalService kakaoLocalService;
    @Mock private PlaceService placeService;

    @Test
    @DisplayName("빔서치는 출발지에서 가까운 선택 장소부터 방문 순서를 구성한다")
    void findOptimalRouteStartsWithNearestPlace() {
        Event farEvent = event(1L, 37.6000, 127.1000);
        Tour nearTour = tour(2L, 37.5001, 127.0001);
        Place nearRestaurant = place(3L, 37.5002, 127.0002);

        List<?> candidates = createRouteCandidates(
                List.of(farEvent),
                List.of(nearTour),
                nearRestaurant,
                null
        );

        List<?> route = findOptimalRoute(candidates, 37.5000, 127.0000);

        assertThat(route).hasSize(3);
        assertThat(itemType(route.get(0))).isEqualTo("TOUR");
        assertThat(itemId(route.get(0))).isEqualTo(2L);
        assertThat(itemIds(route)).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("행사, 관광지, 식당, 카페를 모두 선택하면 빔서치 결과에 중복 없이 모두 포함된다")
    void findOptimalRouteIncludesAllSelectedItemsWithoutDuplicates() {
        Event event = event(1L, 37.5010, 127.0010);
        Tour tour = tour(2L, 37.5020, 127.0020);
        Place restaurant = place(3L, 37.5030, 127.0030);
        Place cafe = place(4L, 37.5040, 127.0040);

        List<?> candidates = createRouteCandidates(
                List.of(event),
                List.of(tour),
                restaurant,
                cafe
        );

        List<?> route = findOptimalRoute(candidates, 37.5000, 127.0000);

        assertThat(route).hasSize(4);
        assertThat(itemIds(route))
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L)
                .doesNotHaveDuplicates();
        assertThat(itemTypes(route)).containsExactlyInAnyOrder("EVENT", "TOUR", "PLACE", "PLACE");
    }

    @Test
    @DisplayName("좌표가 없는 선택 장소는 빔서치 동선 계산 뒤에 추가된다")
    void findOptimalRouteAppendsItemWithoutCoordinatesAfterRouteableItems() {
        Event event = event(1L, 37.5010, 127.0010);
        Tour tourWithoutCoordinates = tour(2L, null, null);
        Place restaurant = place(3L, 37.5020, 127.0020);

        List<?> candidates = createRouteCandidates(
                List.of(event),
                List.of(tourWithoutCoordinates),
                restaurant,
                null
        );

        List<?> route = findOptimalRoute(candidates, 37.5000, 127.0000);

        assertThat(route).hasSize(3);
        assertThat(itemIds(route)).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(itemId(route.get(2))).isEqualTo(2L);
        assertThat(itemType(route.get(2))).isEqualTo("TOUR");
    }

    @Test
    @DisplayName("식당과 카페를 선택하지 않아도 행사와 관광지만으로 동선을 생성한다")
    void findOptimalRouteWorksWithOnlyEventAndTour() {
        Event event = event(1L, 37.5010, 127.0010);
        Tour tour = tour(2L, 37.5020, 127.0020);

        List<?> candidates = createRouteCandidates(
                List.of(event),
                List.of(tour),
                null,
                null
        );

        List<?> route = findOptimalRoute(candidates, 37.5000, 127.0000);

        assertThat(route).hasSize(2);
        assertThat(itemIds(route)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(itemTypes(route)).containsExactlyInAnyOrder("EVENT", "TOUR");
    }

    @SuppressWarnings("unchecked")
    private List<?> createRouteCandidates(
            List<Event> events,
            List<Tour> tours,
            Place restaurant,
            Place cafe
    ) {
        return (List<?>) ReflectionTestUtils.invokeMethod(
                courseService,
                "createRouteCandidates",
                events,
                tours,
                restaurant,
                cafe
        );
    }

    @SuppressWarnings("unchecked")
    private List<?> findOptimalRoute(
            List<?> candidates,
            Double startLatitude,
            Double startLongitude
    ) {
        return (List<?>) ReflectionTestUtils.invokeMethod(
                courseService,
                "findOptimalRoute",
                candidates,
                startLatitude,
                startLongitude
        );
    }

    private Event event(Long id, Double latitude, Double longitude) {
        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getId()).willReturn(id);
        given(event.getLatitude()).willReturn(latitude);
        given(event.getLongitude()).willReturn(longitude);
        given(event.getPlace()).willReturn(null);
        return event;
    }

    private Tour tour(Long id, Double latitude, Double longitude) {
        Tour tour = org.mockito.Mockito.mock(Tour.class);
        given(tour.getId()).willReturn(id);
        given(tour.getLatitude()).willReturn(latitude);
        given(tour.getLongitude()).willReturn(longitude);
        return tour;
    }

    private Place place(Long id, Double latitude, Double longitude) {
        Place place = org.mockito.Mockito.mock(Place.class);
        given(place.getId()).willReturn(id);
        given(place.getLatitude()).willReturn(latitude == null ? null : java.math.BigDecimal.valueOf(latitude));
        given(place.getLongitude()).willReturn(longitude == null ? null : java.math.BigDecimal.valueOf(longitude));
        return place;
    }

    private List<Long> itemIds(List<?> route) {
        return route.stream().map(this::itemId).toList();
    }

    private List<String> itemTypes(List<?> route) {
        return route.stream().map(this::itemType).toList();
    }

    private Long itemId(Object candidate) {
        Object event = invokeAccessor(candidate, "event");
        if (event != null) {
            return ((Event) event).getId();
        }

        Object tour = invokeAccessor(candidate, "tour");
        if (tour != null) {
            return ((Tour) tour).getId();
        }

        Object place = invokeAccessor(candidate, "place");
        return ((Place) place).getId();
    }

    private String itemType(Object candidate) {
        return String.valueOf(invokeAccessor(candidate, "itemType"));
    }

    private Object invokeAccessor(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("경로 후보 정보를 읽을 수 없습니다.", exception);
        }
    }
}
