package come.back.gotoday.course.service;

import come.back.gotoday.category.repository.CategoryRepository;
import come.back.gotoday.course.dto.CourseBookmarkResponse;
import come.back.gotoday.course.dto.SavedCoursePageResponse;
import come.back.gotoday.course.entity.Course;
import come.back.gotoday.course.entity.SavedCourse;
import come.back.gotoday.course.repository.CoursePlaceRepository;
import come.back.gotoday.course.repository.CourseRepository;
import come.back.gotoday.course.repository.SavedCourseRepository;
import come.back.gotoday.event.repository.EventRepository;
import come.back.gotoday.external.kakao.service.KakaoLocalService;
import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.repository.MemberRepository;
import come.back.gotoday.place.repository.PlaceRepository;
import come.back.gotoday.place.service.PlaceService;
import come.back.gotoday.recommend.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseServiceBookmarkTest {

    @InjectMocks
    private CourseService courseService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CoursePlaceRepository coursePlaceRepository;

    @Mock
    private SavedCourseRepository savedCourseRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private KakaoLocalService kakaoLocalService;

    @Mock
    private PlaceService placeService;

    @Test
    @DisplayName("북마크가 없으면 새로 등록한다")
    void toggleBookmark_whenNotBookmarked_thenSaveBookmark() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;

        Member member = createMember(memberId);
        Course course = createCourse(courseId, member);

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));
        given(courseRepository.findById(courseId))
                .willReturn(Optional.of(course));
        given(savedCourseRepository.findByMemberIdAndCourseId(memberId, courseId))
                .willReturn(Optional.empty());
        given(savedCourseRepository.saveAndFlush(any(SavedCourse.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        CourseBookmarkResponse response = courseService.toggleBookmark(memberId, courseId);

        // then
        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.bookmarked()).isTrue();

        verify(savedCourseRepository).saveAndFlush(any(SavedCourse.class));
        verify(savedCourseRepository, never()).delete(any(SavedCourse.class));
    }

    @Test
    @DisplayName("이미 북마크되어 있으면 북마크를 해제한다")
    void toggleBookmark_whenAlreadyBookmarked_thenDeleteBookmark() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;

        Member member = createMember(memberId);
        Course course = createCourse(courseId, member);
        SavedCourse savedCourse = createSavedCourse(100L, member, course);

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));
        given(courseRepository.findById(courseId))
                .willReturn(Optional.of(course));
        given(savedCourseRepository.findByMemberIdAndCourseId(memberId, courseId))
                .willReturn(Optional.of(savedCourse));

        // when
        CourseBookmarkResponse response = courseService.toggleBookmark(memberId, courseId);

        // then
        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.bookmarked()).isFalse();

        verify(savedCourseRepository).delete(savedCourse);
        verify(savedCourseRepository, never()).saveAndFlush(any(SavedCourse.class));
    }

    @Test
    @DisplayName("코스 북마크 여부를 조회한다")
    void isBookmarked() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;

        Member member = createMember(memberId);
        Course course = createCourse(courseId, member);

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));
        given(courseRepository.findById(courseId))
                .willReturn(Optional.of(course));
        given(savedCourseRepository.existsByMemberIdAndCourseId(memberId, courseId))
                .willReturn(true);

        // when
        boolean bookmarked = courseService.isBookmarked(memberId, courseId);

        // then
        assertThat(bookmarked).isTrue();

        verify(savedCourseRepository).existsByMemberIdAndCourseId(memberId, courseId);
    }

    @Test
    @DisplayName("내가 북마크한 코스 목록을 10개 단위로 페이징 조회한다")
    void getSavedCourses_withPaging() {
        // given
        Long memberId = 1L;
        Long courseId = 10L;

        Member member = createMember(memberId);
        Course course = createCourse(courseId, member);
        SavedCourse savedCourse = createSavedCourse(100L, member, course);

        PageRequest pageRequest = PageRequest.of(1, 10);

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));
        given(savedCourseRepository.findAllByMemberIdOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(memberId),
                any(Pageable.class)
        )).willReturn(
                new PageImpl<>(
                        List.of(savedCourse),
                        pageRequest,
                        11
                )
        );

        // when
        SavedCoursePageResponse response = courseService.getSavedCourses(memberId, 1);

        // then
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).savedCourseId()).isEqualTo(100L);
        assertThat(response.content().get(0).courseId()).isEqualTo(courseId);
        assertThat(response.content().get(0).title()).isEqualTo("성수 추천 코스");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(savedCourseRepository)
                .findAllByMemberIdOrderByCreatedAtDesc(
                        org.mockito.ArgumentMatchers.eq(memberId),
                        pageableCaptor.capture()
                );

        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedPageable.getPageNumber()).isEqualTo(1);
        assertThat(capturedPageable.getPageSize()).isEqualTo(10);
    }

    private Member createMember(Long id) {
        Member member = Member.create(
                "test@example.com",
                "encoded-password",
                "테스터",
                "USER",
                "ACTIVE"
        );

        ReflectionTestUtils.setField(member, "id", id);

        return member;
    }

    private Course createCourse(Long id, Member member) {
        Course course = Course.create(
                member,
                "성수 추천 코스",
                "성수에서 즐기는 추천 코스입니다.",
                "RECOMMENDATION",
                LocalDate.of(2026, 6, 22),
                LocalDate.of(2026, 6, 22),
                "성수",
                "FRIEND",
                null,
                null,
                null,
                null,
                "사용자 선호 정보를 기반으로 추천되었습니다."
        );

        ReflectionTestUtils.setField(course, "id", id);

        return course;
    }

    private SavedCourse createSavedCourse(Long id, Member member, Course course) {
        SavedCourse savedCourse = SavedCourse.create(member, course, null);

        ReflectionTestUtils.setField(savedCourse, "id", id);
        ReflectionTestUtils.setField(
                savedCourse,
                "createdAt",
                LocalDateTime.of(2026, 6, 22, 12, 0)
        );

        return savedCourse;
    }
}