
package come.back.gotoday.crowd.controller;

import come.back.gotoday.crowd.dto.CrowdResponse;
import come.back.gotoday.crowd.service.CrowdService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("혼잡도 조회 컨트롤러 테스트")
class CrowdControllerTest {

    @Mock
    private CrowdService crowdService;

    private MockMvc mockMvc;
    private CrowdController crowdController;
    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        crowdController = new CrowdController(crowdService);
        mockMvc = MockMvcBuilders.standaloneSetup(crowdController).build();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("정상 지역명을 전달하면 혼잡도 조회에 성공한다")
    void getCrowdStatusSucceedsWithValidAreaName() throws Exception {
        String areaName = "성수카페거리";
        CrowdResponse response = mock(CrowdResponse.class);
        given(crowdService.getCrowdStatus(areaName)).willReturn(response);

        mockMvc.perform(get("/api/crowds")
                        .param("areaName", areaName))
                .andExpect(status().isOk());

        verify(crowdService).getCrowdStatus(areaName);
    }

    @Test
    @DisplayName("지역명 파라미터가 없으면 400 응답을 반환한다")
    void getCrowdStatusReturnsBadRequestWhenAreaNameIsMissing() throws Exception {
        mockMvc.perform(get("/api/crowds"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지역명이 빈 문자열이면 검증에 실패한다")
    void emptyAreaNameFailsValidation() throws Exception {
        Set<ConstraintViolation<CrowdController>> violations = validateAreaName("");

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("지역명은 필수입니다.");
    }

    @Test
    @DisplayName("지역명이 공백이면 검증에 실패한다")
    void blankAreaNameFailsValidation() throws Exception {
        Set<ConstraintViolation<CrowdController>> violations = validateAreaName("   ");

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("지역명은 필수입니다.");
    }

    private Set<ConstraintViolation<CrowdController>> validateAreaName(String areaName)
            throws Exception {
        Method method = CrowdController.class.getMethod("getCrowdStatus", String.class);

        return validator.forExecutables().validateParameters(
                crowdController,
                method,
                new Object[]{areaName}
        );
    }
}
