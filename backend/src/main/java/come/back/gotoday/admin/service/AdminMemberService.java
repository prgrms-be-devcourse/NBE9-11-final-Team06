package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.response.AdminMemberResponse;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    public List<AdminMemberResponse> getMembers() {
        return memberRepository.findAll()
                .stream()
                .map(AdminMemberResponse::from)
                .toList();
    }
}