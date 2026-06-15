package come.back.gotoday.member.repository;

import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Member> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}