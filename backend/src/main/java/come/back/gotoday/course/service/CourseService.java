package come.back.gotoday.course.service;

import come.back.gotoday.category.entity.Category;
import come.back.gotoday.category.repository.CategoryRepository; // 💡 카테고리 레포 추가
import come.back.gotoday.course.dto.*;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.CoursePlace;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.event.entity.Event;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.kakao.dto.KakaoPlaceDocument;
import come.back.gotoday.external.kakao.dto.KakaoPlaceResponse;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.entity.Place;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.recommend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CoursePlaceRepository coursePlaceRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository; // 💡 주입 완료!

    private final RecommendationService recommendationService;
    private final EventRepository eventRepository;
    private final KakaoLocalService kakaoLocalService;

    // 코스 저장 (생성)
    @Transactional
    public Long createCourse(Long memberId, CourseCreateRequest request) {
        log.info("코스 생성 처리 시작: memberId={}, title={}", memberId, request.title());

        // 1. 회원 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 2. AI 추천 엔진 호출 및 이벤트 조회
        String queryText = recommendationService.createQueryText(request.baseArea(), request.courseType(), request.companionType());
        List<Long> recommendedEventIds = recommendationService.getRecommendedEventIds(
                memberId, queryText, request.startDate(), request.endDate(), 3
        );

        List<Event> events = eventRepository.findAllById(recommendedEventIds);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("추천된 행사가 없습니다.");
        }

        // 3. 행사 중심 좌표 계산 (중간값)
        double centerLat = events.stream()
                .filter(e -> e.getLatitude() != null)
                .mapToDouble(Event::getLatitude)
                .average()
                .orElseThrow(() -> new IllegalArgumentException("유효한 위도 정보가 없습니다."));
        double centerLng = events.stream()
                .filter(e -> e.getLongitude() != null)
                .mapToDouble(Event::getLongitude)
                .average()
                .orElseThrow(() -> new IllegalArgumentException("유효한 경도 정보가 없습니다."));

        // 4. 카카오 API 카페, 식당 추천 받기
        KakaoPlaceResponse cafeResponse = kakaoLocalService.searchCafe(centerLat, centerLng);
        KakaoPlaceDocument cafeDoc = cafeResponse.documents().stream().findFirst().orElse(null);

        KakaoPlaceResponse restaurantResponse = kakaoLocalService.searchRestaurant(centerLat, centerLng);
        KakaoPlaceDocument restaurantDoc = restaurantResponse.documents().stream().findFirst().orElse(null);

        // 5. 코스 마스터 엔티티 생성
        Course course = Course.create(
                member, request.title(), request.description(), request.courseType(),
                request.startDate(), request.endDate(), request.baseArea(), request.companionType(),
                null, null, "AI 추천 행사 및 주변 장소 기반 코스"
        );

        // 취향 저격용 추천 사유 베이스 문구 생성
        String userPreferenceText = String.format("%s에서 %s와(과) 함께 즐기는 %s 코스",
                request.baseArea(), request.companionType(), request.courseType());

        int order = 1;

        // ① AI 추천 행사들을 코스 장소 리스트에 추가
        for (Event event : events) {
            CoursePlace coursePlace = CoursePlace.create(
                    course,
                    event.getPlace(),
                    event,
                    order++,
                    null, null, null, null, null, null,
                    String.format("[%s] 맞춤 추천 행사입니다.", userPreferenceText)
            );
            course.addCoursePlace(coursePlace);
        }

        // ② 주변 추천 식당 추가 (Kakao -> Place 변환 및 저장 로직 적용)
        if (restaurantDoc != null) {
            Category restaurantCategory = categoryRepository.findByName("맛집")
                    .orElseThrow(() -> new IllegalArgumentException("'맛집' 카테고리가 초기화되지 않았습니다."));

            Place restaurantPlace = placeRepository
                    .findByNameAndAddressAndIsActiveTrue(
                            restaurantDoc.placeName(),
                            restaurantDoc.addressName()
                    )
                    .orElseGet(() -> placeRepository.save(
                            Place.builder()
                                    .name(restaurantDoc.placeName())
                                    .address(restaurantDoc.addressName())
                                    .roadAddress(restaurantDoc.roadAddressName())
                                    .phone(restaurantDoc.phone())
                                    .latitude(BigDecimal.valueOf(Double.parseDouble(restaurantDoc.y())))
                                    .longitude(BigDecimal.valueOf(Double.parseDouble(restaurantDoc.x())))
                                    .category(restaurantCategory)
                                    // 💡 DB 제약조건 통과를 위해 직접 현재 시간 주입!
                                    .createdAt(java.time.LocalDateTime.now())
                                    .updatedAt(java.time.LocalDateTime.now())
                                    .isActive(true) // 💡 혹시 isActive도 Not Null 이라면 안전하게 미리 세팅!
                                    .source("KAKAO")
                                    .build()
                    ));

            CoursePlace restaurantCoursePlace = CoursePlace.create(
                    course,
                    restaurantPlace,
                    null,
                    order++,
                    null, null, null, null, null, null,
                    String.format("추천 행사 주변 맛집입니다. (%s 취향 반영)", userPreferenceText)
            );
            course.addCoursePlace(restaurantCoursePlace);
        }

        // ③ 주변 추천 카페 추가
        if (cafeDoc != null) {
            Category cafeCategory = categoryRepository.findByName("카페")
                    .orElseThrow(() -> new IllegalArgumentException("'카페' 카테고리가 초기화되지 않았습니다."));

            Place cafePlace = placeRepository
                    .findByNameAndAddressAndIsActiveTrue(
                            cafeDoc.placeName(),
                            cafeDoc.addressName()
                    )
                    .orElseGet(() -> placeRepository.save(
                            Place.builder()
                                    .name(cafeDoc.placeName())
                                    .address(cafeDoc.addressName())
                                    .roadAddress(cafeDoc.roadAddressName())
                                    .phone(cafeDoc.phone())
                                    .latitude(BigDecimal.valueOf(Double.parseDouble(cafeDoc.y())))
                                    .longitude(BigDecimal.valueOf(Double.parseDouble(cafeDoc.x())))
                                    .category(cafeCategory)
                                    // 💡 카페도 동일하게 시간 데이터 완비!
                                    .createdAt(java.time.LocalDateTime.now())
                                    .updatedAt(java.time.LocalDateTime.now())
                                    .isActive(true)
                                    .source("KAKAO")
                                    .build()
                    ));

            CoursePlace cafeCoursePlace = CoursePlace.create(
                    course,
                    cafePlace,
                    null,
                    order++,
                    null, null, null, null, null, null,
                    String.format("코스의 마무리를 장식할 추천 카페입니다. (%s 취향 반영)", userPreferenceText)
            );
            course.addCoursePlace(cafeCoursePlace);
        }

        courseRepository.save(course);
        return course.getId();
    }

    // 코스 상세조회
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long courseId) {
        log.info("코스 단건 조회 처리 시작: courseId={}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        List<CoursePlaceResponse> places =
                coursePlaceRepository.findDetailByCourseId(courseId)
                        .stream()
                        .map(cp -> {
                            Long placeId = null;
                            String placeName = "알 수 없는 장소";

                            // 💡 [수정] 이름 매핑 우선순위 필터링 개조
                            if (cp.getEvent() != null) {
                                // 1. 코스 장소에 '행사(Event)'가 꽂혀있는 경우 -> 행사 정보가 최우선!
                                placeName = cp.getEvent().getTitle(); // 🎯 행사명을 바로 대입!
                                if (cp.getEvent().getPlace() != null) {
                                    placeId = cp.getEvent().getPlace().getId();
                                }
                            } else if (cp.getPlace() != null) {
                                // 2. 카카오 API로 받아온 식당/카페처럼 일반 장소('Place')만 꽂혀있는 경우
                                placeId = cp.getPlace().getId();
                                placeName = cp.getPlace().getName();
                            }

                            return new CoursePlaceResponse(
                                    placeId,
                                    placeName,
                                    cp.getVisitOrder(),
                                    cp.getRecommendationReason()
                            );
                        })
                        .toList();

        return new CourseDetailResponse(
                course.getId(), course.getTitle(), course.getDescription(), course.getCourseType(),
                course.getStartDate(), course.getEndDate(), course.getBaseArea(), course.getCompanionType(),
                places,
                course.getTotalDistance() != null ? course.getTotalDistance() : 0.0,
                course.getEstimatedTime() != null ? course.getEstimatedTime() : 0
        );
    }

    // 코스 목록 조회
    @Transactional(readOnly = true)
    public List<CourseListResponse> getCourses() {
        return courseRepository.findAll()
                .stream()
                .map(course -> new CourseListResponse(
                        course.getId(), course.getTitle(), course.getCourseType(),
                        course.getBaseArea(), course.getStartDate()
                ))
                .toList();
    }

    // 코스 삭제
    @Transactional
    public void deleteCourse(Long memberId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코스입니다."));

        if (!course.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 코스를 삭제할 권한이 없습니다.");
        }

        coursePlaceRepository.deleteByCourseId(courseId);
        courseRepository.delete(course);
    }
}