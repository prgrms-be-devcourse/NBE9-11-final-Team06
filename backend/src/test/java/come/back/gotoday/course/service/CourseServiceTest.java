package come.back.gotoday.course.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.course.dto.CoursePreviewRequest;

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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    @DisplayName("선택한 장소가 없으면 빔서치 동선도 빈 목록을 반환한다")
    void findOptimalRouteReturnsEmptyWhenCandidatesAreEmpty() {
        List<?> route = findOptimalRoute(List.of(), 37.5000, 127.0000);

        assertThat(route).isEmpty();
    }

    @Test
    @DisplayName("미리보기는 선택한 행사와 관광지 주변의 식당·카페 후보를 반환한다")
    void previewCourseReturnsNearbyPlacesForSelectedEventAndTour() {
        Long memberId = 1L;
        Category restaurantCategory = org.mockito.Mockito.mock(Category.class);
        Category cafeCategory = org.mockito.Mockito.mock(Category.class);
        Event event = event(10L, 37.5665, 126.9780);
        Tour tour = tour(20L, 37.5670, 126.9790);
        Place previewPlace = place(30L, 37.5666, 126.9781);

        CoursePreviewRequest request = org.mockito.Mockito.mock(CoursePreviewRequest.class);
        given(request.categories()).willReturn(List.of("전시", "문화생활"));
        given(request.eventIds()).willReturn(java.util.Arrays.asList(10L, 10L, null));
        given(request.tourIds()).willReturn(List.of(20L, 20L));
        given(request.startLatitude()).willReturn(37.5665);
        given(request.startLongitude()).willReturn(126.9780);
        given(request.restaurantType()).willReturn(null);

        come.back.gotoday.member.entity.Member member = org.mockito.Mockito.mock(
                come.back.gotoday.member.entity.Member.class
        );
        come.back.gotoday.external.kakao.dto.KakaoPlaceDocument document =
                org.mockito.Mockito.mock(come.back.gotoday.external.kakao.dto.KakaoPlaceDocument.class);
        given(document.y()).willReturn("37.5666");
        given(document.x()).willReturn("126.9781");

        come.back.gotoday.external.kakao.dto.KakaoPlaceResponse cafeResponse =
                org.mockito.Mockito.mock(come.back.gotoday.external.kakao.dto.KakaoPlaceResponse.class);
        come.back.gotoday.external.kakao.dto.KakaoPlaceResponse restaurantResponse =
                org.mockito.Mockito.mock(come.back.gotoday.external.kakao.dto.KakaoPlaceResponse.class);
        given(cafeResponse.documents()).willReturn(List.of(document));
        given(restaurantResponse.documents()).willReturn(List.of(document));

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(eventRepository.findAllById(List.of(10L))).willReturn(List.of(event));
        given(tourRepository.findAllById(List.of(20L))).willReturn(List.of(tour));
        given(categoryRepository.findFirstByNameOrderByIdAsc("식당"))
                .willReturn(Optional.of(restaurantCategory));
        given(categoryRepository.findFirstByNameOrderByIdAsc("카페"))
                .willReturn(Optional.of(cafeCategory));
        given(kakaoLocalService.searchCafe(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble()))
                .willReturn(cafeResponse);
        given(kakaoLocalService.searchRestaurant(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(restaurantResponse);
        given(placeService.getOrCreatePlaces(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any(Category.class)
        )).willReturn(List.of(previewPlace));

        Object response = courseService.previewCourse(memberId, request);

        assertThat(response).isNotNull();
        verify(eventRepository).findAllById(List.of(10L));
        verify(tourRepository).findAllById(List.of(20L));
        verify(kakaoLocalService, org.mockito.Mockito.times(2)).searchCafe(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble()
        );
        verify(kakaoLocalService, org.mockito.Mockito.times(2)).searchRestaurant(
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.anyDouble(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(placeService, org.mockito.Mockito.times(4)).getOrCreatePlaces(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any(Category.class)
        );
    }

    @Test
    @DisplayName("코스 상세 조회는 코스 기본 정보와 방문 장소 목록을 반환한다")
    void getCourseReturnsCourseDetailWithEmptyPlaces() {
        Long courseId = 1L;
        come.back.gotoday.course.entity.Course course = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.Course.class
        );
        java.time.LocalDate startDate = java.time.LocalDate.of(2026, 7, 1);
        java.time.LocalDate endDate = java.time.LocalDate.of(2026, 7, 2);

        given(course.getId()).willReturn(courseId);
        given(course.getTitle()).willReturn("여름 서울 코스");
        given(course.getDescription()).willReturn("전시와 카페를 즐기는 코스");
        given(course.getStartDate()).willReturn(startDate);
        given(course.getEndDate()).willReturn(endDate);
        given(course.getBaseArea()).willReturn("종로구");
        given(course.getStartLatitude()).willReturn(37.5665);
        given(course.getStartLongitude()).willReturn(126.9780);
        given(course.getRecommendationReason()).willReturn("도보 이동이 편리한 코스입니다.");
        given(course.getTotalDistance()).willReturn(2);
        given(course.getEstimatedTime()).willReturn(180);
        given(course.getAverageRating()).willReturn(4.5);
        given(course.getReviewCount()).willReturn(3);

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(coursePlaceRepository.findDetailByCourseId(courseId)).willReturn(List.of());

        come.back.gotoday.course.dto.CourseDetailResponse result = courseService.getCourse(courseId);

        assertThat(result.courseId()).isEqualTo(courseId);
        assertThat(result.title()).isEqualTo("여름 서울 코스");
        assertThat(result.startDate()).isEqualTo(startDate);
        assertThat(result.endDate()).isEqualTo(endDate);
        assertThat(result.baseArea()).isEqualTo("종로구");
        assertThat(result.places()).isEmpty();
        assertThat(result.totalDistance()).isEqualTo(2.0);
        assertThat(result.estimatedTime()).isEqualTo(180);
        verify(courseRepository).findById(courseId);
        verify(coursePlaceRepository).findDetailByCourseId(courseId);
    }

    @Test
    @DisplayName("코스 상세 조회는 관광지, 행사, 일반 장소와 연결 대상 없는 장소를 각각 응답으로 변환한다")
    void getCourseConvertsAllCoursePlaceTypesToResponses() {
        Long courseId = 2L;
        come.back.gotoday.course.entity.Course course = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.Course.class
        );
        given(course.getId()).willReturn(courseId);

        Tour tour = org.mockito.Mockito.mock(Tour.class);
        given(tour.getId()).willReturn(101L);
        given(tour.getTitle()).willReturn("북촌 한옥마을");
        given(tour.getLatitude()).willReturn(37.5826);
        given(tour.getLongitude()).willReturn(126.9830);
        given(tour.getAddress()).willReturn("서울 종로구 계동길");

        come.back.gotoday.course.entity.CoursePlace tourCoursePlace = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.CoursePlace.class
        );
        given(tourCoursePlace.getTour()).willReturn(tour);
        given(tourCoursePlace.getVisitOrder()).willReturn(1);
        given(tourCoursePlace.getRecommendationReason()).willReturn("한옥 거리 산책");

        Place eventPlace = org.mockito.Mockito.mock(Place.class);
        given(eventPlace.getId()).willReturn(201L);
        given(eventPlace.getLatitude()).willReturn(java.math.BigDecimal.valueOf(37.5700));
        given(eventPlace.getLongitude()).willReturn(java.math.BigDecimal.valueOf(126.9900));
        given(eventPlace.getAddress()).willReturn("서울 종로구 세종대로");

        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getId()).willReturn(102L);
        given(event.getTitle()).willReturn("서울 전시");
        given(event.getPlace()).willReturn(eventPlace);

        come.back.gotoday.course.entity.CoursePlace eventCoursePlace = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.CoursePlace.class
        );
        given(eventCoursePlace.getEvent()).willReturn(event);
        given(eventCoursePlace.getVisitOrder()).willReturn(2);
        given(eventCoursePlace.getRecommendationReason()).willReturn("전시 관람");

        Place place = org.mockito.Mockito.mock(Place.class);
        given(place.getId()).willReturn(301L);
        given(place.getName()).willReturn("테스트 카페");
        given(place.getLatitude()).willReturn(java.math.BigDecimal.valueOf(37.5710));
        given(place.getLongitude()).willReturn(java.math.BigDecimal.valueOf(126.9910));
        given(place.getAddress()).willReturn("서울 종로구 카페길");

        come.back.gotoday.course.entity.CoursePlace placeCoursePlace = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.CoursePlace.class
        );
        given(placeCoursePlace.getPlace()).willReturn(place);
        given(placeCoursePlace.getVisitOrder()).willReturn(3);
        given(placeCoursePlace.getRecommendationReason()).willReturn("휴식하기 좋은 카페");

        come.back.gotoday.course.entity.CoursePlace unknownCoursePlace = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.CoursePlace.class
        );
        given(unknownCoursePlace.getVisitOrder()).willReturn(4);
        given(unknownCoursePlace.getRecommendationReason()).willReturn("대체 장소");

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(coursePlaceRepository.findDetailByCourseId(courseId)).willReturn(List.of(
                tourCoursePlace,
                eventCoursePlace,
                placeCoursePlace,
                unknownCoursePlace
        ));

        come.back.gotoday.course.dto.CourseDetailResponse result = courseService.getCourse(courseId);

        assertThat(result.places()).hasSize(4);
        assertThat(result.places().get(0).itemType())
                .isEqualTo(come.back.gotoday.course.type.CourseItemType.TOUR);
        assertThat(result.places().get(0).tourId()).isEqualTo(101L);
        assertThat(result.places().get(0).latitude()).isEqualByComparingTo("37.5826");

        assertThat(result.places().get(1).itemType())
                .isEqualTo(come.back.gotoday.course.type.CourseItemType.EVENT);
        assertThat(result.places().get(1).eventId()).isEqualTo(102L);
        assertThat(result.places().get(1).placeId()).isEqualTo(201L);
        assertThat(result.places().get(1).address()).isEqualTo("서울 종로구 세종대로");

        assertThat(result.places().get(2).itemType())
                .isEqualTo(come.back.gotoday.course.type.CourseItemType.PLACE);
        assertThat(result.places().get(2).placeId()).isEqualTo(301L);
        assertThat(result.places().get(2).itemName()).isEqualTo("테스트 카페");

        assertThat(result.places().get(3).itemType()).isNull();
        assertThat(result.places().get(3).itemName()).isEqualTo("알 수 없는 장소");
        assertThat(result.places().get(3).visitOrder()).isEqualTo(4);
    }

    @Test
    @DisplayName("전체 코스 조회는 저장된 코스를 목록 응답으로 변환한다")
    void getCoursesReturnsCourseListResponses() {
        come.back.gotoday.course.entity.Course firstCourse = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.Course.class
        );
        come.back.gotoday.course.entity.Course secondCourse = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.Course.class
        );

        given(firstCourse.getId()).willReturn(1L);
        given(firstCourse.getTitle()).willReturn("종로 전시 코스");
        given(firstCourse.getBaseArea()).willReturn("종로구");
        given(firstCourse.getStartDate()).willReturn(java.time.LocalDate.of(2026, 7, 1));
        given(firstCourse.getAverageRating()).willReturn(4.0);
        given(firstCourse.getReviewCount()).willReturn(2);

        given(secondCourse.getId()).willReturn(2L);
        given(secondCourse.getTitle()).willReturn("성수 카페 코스");
        given(secondCourse.getBaseArea()).willReturn("성동구");
        given(secondCourse.getStartDate()).willReturn(java.time.LocalDate.of(2026, 7, 2));
        given(secondCourse.getAverageRating()).willReturn(4.8);
        given(secondCourse.getReviewCount()).willReturn(5);

        given(courseRepository.findAll()).willReturn(List.of(firstCourse, secondCourse));

        List<come.back.gotoday.course.dto.CourseListResponse> result = courseService.getCourses();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(come.back.gotoday.course.dto.CourseListResponse::courseId)
                .containsExactly(1L, 2L);
        assertThat(result).extracting(come.back.gotoday.course.dto.CourseListResponse::title)
                .containsExactly("종로 전시 코스", "성수 카페 코스");
        verify(courseRepository).findAll();
    }

    @Test
    @DisplayName("카카오 카페 검색 timeout 발생 시 빈 목록 fallback을 위해 null 응답을 반환한다")
    void searchCafeOrEmptyReturnsNullWhenKakaoCafeSearchTimesOut() {
        double latitude = 37.5260087284496;
        double longitude = 126.900109255921;

        given(kakaoLocalService.searchCafe(latitude, longitude))
                .willThrow(new org.springframework.web.client.RestClientException("Read timed out"));

        Object response = ReflectionTestUtils.invokeMethod(
                courseService,
                "searchCafeOrEmpty",
                null,
                1L,
                latitude,
                longitude
        );

        assertThat(response).isNull();
        verify(kakaoLocalService).searchCafe(latitude, longitude);
        verifyNoInteractions(placeService);
    }

    @Test
    @DisplayName("카카오 식당 검색 timeout 발생 시 빈 목록 fallback을 위해 null 응답을 반환한다")
    void searchRestaurantOrEmptyReturnsNullWhenKakaoRestaurantSearchTimesOut() {
        double latitude = 37.5260087284496;
        double longitude = 126.900109255921;

        given(kakaoLocalService.searchRestaurant(
                latitude,
                longitude,
                come.back.gotoday.course.type.RestaurantType.KOREAN
        )).willThrow(new org.springframework.web.client.RestClientException("Read timed out"));

        Object response = ReflectionTestUtils.invokeMethod(
                courseService,
                "searchRestaurantOrEmpty",
                null,
                1L,
                latitude,
                longitude,
                come.back.gotoday.course.type.RestaurantType.KOREAN
        );

        assertThat(response).isNull();
        verify(kakaoLocalService).searchRestaurant(
                latitude,
                longitude,
                come.back.gotoday.course.type.RestaurantType.KOREAN
        );
        verifyNoInteractions(placeService);
    }

    @Test
    @DisplayName("코스 저장은 생성 요청과 최종 코스 추천 이유를 저장 엔티티에 반영한다")
    void saveCoursePersistsCourseAndReturnsGeneratedId() {
        Long memberId = 1L;
        come.back.gotoday.member.entity.Member member = org.mockito.Mockito.mock(
                come.back.gotoday.member.entity.Member.class
        );
        come.back.gotoday.course.dto.CourseCreateRequest request = org.mockito.Mockito.mock(
                come.back.gotoday.course.dto.CourseCreateRequest.class
        );

        given(request.eventIds()).willReturn(List.of());
        given(request.tourIds()).willReturn(List.of());
        given(request.restaurantId()).willReturn(null);
        given(request.cafeId()).willReturn(null);
        given(request.getSelectedRecommendationItemsOrEmpty()).willReturn(List.of());
        given(request.startLatitude()).willReturn(37.5665);
        given(request.startLongitude()).willReturn(126.9780);
        given(request.title()).willReturn("서울 하루 코스");
        given(request.description()).willReturn("전시와 산책을 즐기는 코스");
        given(request.courseType()).willReturn(null);
        given(request.startDate()).willReturn(java.time.LocalDate.of(2026, 7, 1));
        given(request.endDate()).willReturn(java.time.LocalDate.of(2026, 7, 1));
        given(request.baseArea()).willReturn("종로구");
        given(request.companionType()).willReturn(null);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(courseRepository.save(org.mockito.ArgumentMatchers.any(
                come.back.gotoday.course.entity.Course.class
        ))).willAnswer(invocation -> {
            come.back.gotoday.course.entity.Course savedCourse = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedCourse, "id", 99L);
            return savedCourse;
        });

        Object creationContext = ReflectionTestUtils.invokeMethod(
                courseService,
                "prepareCourseCreation",
                memberId,
                request
        );

        Long result = ReflectionTestUtils.invokeMethod(
                courseService,
                "saveCourse",
                memberId,
                creationContext,
                "AI가 생성한 최종 코스 추천 이유",
                List.of()
        );

        assertThat(result).isEqualTo(99L);
        org.mockito.ArgumentCaptor<come.back.gotoday.course.entity.Course> courseCaptor =
                org.mockito.ArgumentCaptor.forClass(come.back.gotoday.course.entity.Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        assertThat(courseCaptor.getValue().getTitle()).isEqualTo("서울 하루 코스");
        assertThat(courseCaptor.getValue().getRecommendationReason())
                .isEqualTo("AI가 생성한 최종 코스 추천 이유");
    }

    @Test
    @DisplayName("코스 장소 저장은 행사·관광지·일반 장소 유형에 따라 각각 참조 엔티티를 연결한다")
    void addCoursePlaceLinksEventTourAndPlaceCandidates() {
        come.back.gotoday.course.entity.Course course = org.mockito.Mockito.mock(
                come.back.gotoday.course.entity.Course.class
        );
        Event selectedEvent = event(10L, 37.5665, 126.9780);
        Tour selectedTour = tour(20L, 37.5670, 126.9790);
        Place selectedPlace = place(30L, 37.5680, 126.9800);

        Event eventReference = org.mockito.Mockito.mock(Event.class);
        given(eventRepository.getReferenceById(10L)).willReturn(eventReference);
        given(tourRepository.getReferenceById(20L)).willReturn(selectedTour);
        given(placeRepository.getReferenceById(30L)).willReturn(selectedPlace);

        List<?> candidates = createRouteCandidates(
                List.of(selectedEvent),
                List.of(selectedTour),
                selectedPlace,
                null
        );

        ReflectionTestUtils.invokeMethod(
                courseService,
                "addCoursePlace",
                course,
                candidates.get(0),
                1,
                "행사 추천 이유"
        );
        ReflectionTestUtils.invokeMethod(
                courseService,
                "addCoursePlace",
                course,
                candidates.get(1),
                2,
                "관광지 추천 이유"
        );
        ReflectionTestUtils.invokeMethod(
                courseService,
                "addCoursePlace",
                course,
                candidates.get(2),
                3,
                "장소 추천 이유"
        );

        verify(eventRepository).getReferenceById(10L);
        verify(tourRepository).getReferenceById(20L);
        verify(placeRepository).getReferenceById(30L);
        verify(course, org.mockito.Mockito.times(3)).addCoursePlace(
                org.mockito.ArgumentMatchers.any(come.back.gotoday.course.entity.CoursePlace.class)
        );
    }

    @Test
    @DisplayName("동선 후보 생성은 행사 장소 좌표와 개별 추천 이유를 우선 사용하고 좌표 없는 식당·카페도 포함한다")
    void createRouteCandidatesUsesEventPlaceCoordinatesReasonsAndNullPlaceCoordinates() {
        Event event = org.mockito.Mockito.mock(Event.class);
        given(event.getId()).willReturn(10L);

        Place eventPlace = org.mockito.Mockito.mock(Place.class);
        given(eventPlace.getLatitude()).willReturn(java.math.BigDecimal.valueOf(37.5665));
        given(eventPlace.getLongitude()).willReturn(java.math.BigDecimal.valueOf(126.9780));
        given(event.getPlace()).willReturn(eventPlace);

        Tour tour = tour(20L, 37.5670, 126.9790);
        Place restaurantWithoutCoordinates = place(30L, null, null);
        Place cafeWithoutCoordinates = place(40L, null, null);

        @SuppressWarnings("unchecked")
        List<?> candidates = (List<?>) ReflectionTestUtils.invokeMethod(
                courseService,
                "createRouteCandidates",
                List.of(event),
                List.of(tour),
                restaurantWithoutCoordinates,
                cafeWithoutCoordinates,
                Map.of(10L, "행사 맞춤 추천 이유"),
                Map.of(20L, " ")
        );

        assertThat(candidates).hasSize(4);

        Object eventCandidate = candidates.get(0);
        assertThat(invokeAccessor(eventCandidate, "latitude")).isEqualTo(37.5665);
        assertThat(invokeAccessor(eventCandidate, "longitude")).isEqualTo(126.9780);
        assertThat(invokeAccessor(eventCandidate, "recommendationReason"))
                .isEqualTo("행사 맞춤 추천 이유");

        Object tourCandidate = candidates.get(1);
        assertThat(invokeAccessor(tourCandidate, "recommendationReason"))
                .isEqualTo("선택한 관광지입니다.");

        Object restaurantCandidate = candidates.get(2);
        assertThat(invokeAccessor(restaurantCandidate, "latitude")).isNull();
        assertThat(invokeAccessor(restaurantCandidate, "longitude")).isNull();

        Object cafeCandidate = candidates.get(3);
        assertThat(invokeAccessor(cafeCandidate, "latitude")).isNull();
        assertThat(invokeAccessor(cafeCandidate, "longitude")).isNull();
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
                cafe,
                Map.of(),
                Map.of()
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
        org.mockito.Mockito.lenient().when(event.getPlace()).thenReturn(null);
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
        org.mockito.Mockito.lenient().when(place.getId()).thenReturn(id);
        if (latitude == null) {
            org.mockito.Mockito.lenient().when(place.getLatitude()).thenReturn(null);
            org.mockito.Mockito.lenient().when(place.getLongitude()).thenReturn(null);
        } else {
            given(place.getLatitude()).willReturn(java.math.BigDecimal.valueOf(latitude));
            given(place.getLongitude()).willReturn(
                    longitude == null ? null : java.math.BigDecimal.valueOf(longitude)
            );
        }
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
    @Test
    @DisplayName("코스 생성 준비는 선택 항목을 조회하고 추천 이유를 반영한 동선 후보를 생성한다")
    void prepareCourseCreationBuildsOrderedCandidatesWithSelectedReasons() {
        Long memberId = 1L;
        come.back.gotoday.member.entity.Member member = org.mockito.Mockito.mock(
                come.back.gotoday.member.entity.Member.class
        );
        Event event = event(10L, 37.5665, 126.9780);
        Tour tour = tour(20L, 37.5670, 126.9790);
        Place restaurant = place(30L, 37.5680, 126.9800);
        Place cafe = place(40L, 37.5690, 126.9810);

        come.back.gotoday.course.dto.CourseCreateRequest request = org.mockito.Mockito.mock(
                come.back.gotoday.course.dto.CourseCreateRequest.class
        );
        given(request.eventIds()).willReturn(java.util.Arrays.asList(10L, 10L, null));
        given(request.tourIds()).willReturn(List.of(20L, 20L));
        given(request.restaurantId()).willReturn(30L);
        given(request.cafeId()).willReturn(40L);
        given(request.startLatitude()).willReturn(37.5660);
        given(request.startLongitude()).willReturn(126.9770);

        come.back.gotoday.course.dto.CourseCreateRequest.SelectedRecommendationItem eventItem =
                org.mockito.Mockito.mock(
                        come.back.gotoday.course.dto.CourseCreateRequest.SelectedRecommendationItem.class
                );
        given(eventItem.isEvent()).willReturn(true);
        given(eventItem.eventId()).willReturn(10L);
        given(eventItem.recommendationReason()).willReturn("  전시 관람을 추천합니다.  ");

        come.back.gotoday.course.dto.CourseCreateRequest.SelectedRecommendationItem tourItem =
                org.mockito.Mockito.mock(
                        come.back.gotoday.course.dto.CourseCreateRequest.SelectedRecommendationItem.class
                );
        given(tourItem.isTour()).willReturn(true);
        given(tourItem.tourId()).willReturn(20L);
        given(tourItem.recommendationReason()).willReturn("  산책하기 좋은 관광지입니다.  ");

        given(request.getSelectedRecommendationItemsOrEmpty()).willReturn(List.of(eventItem, tourItem));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(eventRepository.findAllById(List.of(10L))).willReturn(List.of(event));
        given(tourRepository.findAllById(List.of(20L))).willReturn(List.of(tour));
        given(placeRepository.findById(30L)).willReturn(Optional.of(restaurant));
        given(placeRepository.findById(40L)).willReturn(Optional.of(cafe));

        Object creationContext = ReflectionTestUtils.invokeMethod(
                courseService,
                "prepareCourseCreation",
                memberId,
                request
        );

        @SuppressWarnings("unchecked")
        List<?> orderedPlaces = (List<?>) invokeAccessor(creationContext, "orderedPlaces");

        assertThat(orderedPlaces).hasSize(4);
        assertThat(itemIds(orderedPlaces)).containsExactlyInAnyOrder(10L, 20L, 30L, 40L);
        assertThat(itemTypes(orderedPlaces))
                .containsExactlyInAnyOrder("EVENT", "TOUR", "PLACE", "PLACE");

        Object eventCandidate = orderedPlaces.stream()
                .filter(candidate -> "EVENT".equals(itemType(candidate)))
                .findFirst()
                .orElseThrow();
        Object tourCandidate = orderedPlaces.stream()
                .filter(candidate -> "TOUR".equals(itemType(candidate)))
                .findFirst()
                .orElseThrow();

        assertThat(invokeAccessor(eventCandidate, "recommendationReason"))
                .isEqualTo("전시 관람을 추천합니다.");
        assertThat(invokeAccessor(tourCandidate, "recommendationReason"))
                .isEqualTo("산책하기 좋은 관광지입니다.");

        verify(memberRepository).findById(memberId);
        verify(eventRepository).findAllById(List.of(10L));
        verify(tourRepository).findAllById(List.of(20L));
        verify(placeRepository).findById(30L);
        verify(placeRepository).findById(40L);
    }
}