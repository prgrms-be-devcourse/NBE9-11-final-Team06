package come.back.gotoday.member.controller;

import come.back.gotoday.global.response.ApiResponse;
import come.back.gotoday.global.security.CustomUserDetails;
import come.back.gotoday.member.dto.MemberCreateRequest;
import come.back.gotoday.member.dto.MemberResponse;
import come.back.gotoday.member.dto.MemberUpdateRequest;
import come.back.gotoday.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> createMember(
            @RequestBody @Valid MemberCreateRequest request
    ) {
        log.info("회원가입 요청: email={}", request.email());
        MemberResponse response = memberService.createMember(request);
        log.info("회원가입 응답 완료: memberId={}", response.id());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입에 성공했습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("내 정보 조회 요청: memberId={}", userDetails.getMemberId());
        MemberResponse response = memberService.getMyInfo(userDetails.getMemberId());
        log.info("내 정보 조회 응답: memberId={}", userDetails.getMemberId());

        return ResponseEntity.ok(ApiResponse.success(response, "회원 정보를 조회했습니다."));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid MemberUpdateRequest request
    ) {
        log.info("내 정보 수정 요청: memberId={}", userDetails.getMemberId());
        MemberResponse response = memberService.updateMyInfo(userDetails.getMemberId(), request);
        log.info("내 정보 수정 응답: memberId={}", userDetails.getMemberId());

        return ResponseEntity.ok(ApiResponse.success(response, "회원 정보를 수정했습니다."));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawMyAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("회원 탈퇴 요청: memberId={}", userDetails.getMemberId());
        memberService.withdrawMyAccount(userDetails.getMemberId());
        log.info("회원 탈퇴 응답: memberId={}", userDetails.getMemberId());

        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다."));
    }
}