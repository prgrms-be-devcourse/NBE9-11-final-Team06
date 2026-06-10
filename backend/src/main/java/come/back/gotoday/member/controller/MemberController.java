package come.back.gotoday.member.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.member.dto.MemberCreateRequest;
import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(
            @RequestBody @Valid MemberCreateRequest request
    ) {
        MemberResponse response = memberService.createMember(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입에 성공했습니다."));
    }
}