package come.back.gotoday.recommend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("추천 코스 생성 요청 검증 테스트")
class RecommendationCourseCreateRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("유효한 추천 코스 생성 요청은 검증을 통과한다")
    void validRequestPassesValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                3,
                List.of("전시", "카페"),
                37.5665,
                126.9780
        );

        Set<ConstraintViolation<RecommendationCourseCreateRequest>> violations =
                validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 검증에 실패한다")
    void endDateBeforeStartDateFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(1),
                3,
                List.of("전시"),
                37.5665,
                126.9780
        );

        Set<ConstraintViolation<RecommendationCourseCreateRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("validPeriod");
    }

    @Test
    @DisplayName("topK가 1보다 작으면 검증에 실패한다")
    void topKLessThanMinimumFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now(),
                LocalDate.now(),
                0,
                List.of("전시"),
                37.5665,
                126.9780
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("topK");
    }

    @Test
    @DisplayName("topK가 10보다 크면 검증에 실패한다")
    void topKGreaterThanMaximumFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now(),
                LocalDate.now(),
                11,
                List.of("전시"),
                37.5665,
                126.9780
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("topK");
    }

    @Test
    @DisplayName("카테고리가 5개를 초과하면 검증에 실패한다")
    void moreThanFiveCategoriesFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now(),
                LocalDate.now(),
                3,
                List.of("전시", "카페", "공연", "축제", "맛집", "산책"),
                37.5665,
                126.9780
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("categories");
    }

    @Test
    @DisplayName("위도와 경도 중 하나만 전달하면 검증에 실패한다")
    void onlyOneCoordinateFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now(),
                LocalDate.now(),
                3,
                List.of("전시"),
                37.5665,
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("validCoordinates");
    }

    @Test
    @DisplayName("위도가 허용 범위를 벗어나면 검증에 실패한다")
    void latitudeOutsideRangeFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now(),
                LocalDate.now(),
                3,
                List.of("전시"),
                91.0,
                126.9780
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("latitude");
    }

    @Test
    @DisplayName("경도가 허용 범위를 벗어나면 검증에 실패한다")
    void longitudeOutsideRangeFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now(),
                LocalDate.now(),
                3,
                List.of("전시"),
                37.5665,
                181.0
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("longitude");
    }

    @Test
    @DisplayName("시작일이 과거이면 검증에 실패한다")
    void pastStartDateFailsValidation() {
        RecommendationCourseCreateRequest request = createRequest(
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                3,
                List.of("전시"),
                37.5665,
                126.9780
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("startDate");
    }

    private RecommendationCourseCreateRequest createRequest(
            LocalDate startDate,
            LocalDate endDate,
            Integer topK,
            List<String> categories,
            Double latitude,
            Double longitude
    ) {
        return new RecommendationCourseCreateRequest(
                "서울 데이트 추천 코스",
                startDate,
                endDate,
                topK,
                "강남구",
                categories,
                "커플",
                "서울특별시 강남구",
                latitude,
                longitude
        );
    }
}
