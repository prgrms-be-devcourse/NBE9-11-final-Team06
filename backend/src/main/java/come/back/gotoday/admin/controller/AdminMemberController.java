package come.back.gotoday.admin.controller;

import come.back.gotoday.admin.dto.response.AdminMemberResponse;
import come.back.gotoday.admin.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ResponseEntity<List<AdminMemberResponse>> getMembers() {
        return ResponseEntity.ok(adminMemberService.getMembers());
    }
}