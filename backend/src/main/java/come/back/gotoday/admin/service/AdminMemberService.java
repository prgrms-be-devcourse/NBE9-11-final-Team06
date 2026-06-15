package come.back.gotoday.admin.service;

import come.back.gotoday.admin.dto.response.AdminMemberResponse;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    public Page<AdminMemberResponse> getMembers(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(AdminMemberResponse::from);
    }
}