package come.back.gotoday.auth.oauth;

import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.entity.OAuthProvider;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider = getProvider(registrationId);
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                provider,
                oauth2User.getAttributes()
        );

        Member member = getOrCreateMember(provider, userInfo);

        if (member.isDeleted()) {
            throw new DisabledException("탈퇴한 회원입니다.");
        }

        return new CustomOAuth2User(
                member.getId(),
                member.getEmail(),
                member.getRole(),
                oauth2User.getAttributes(),
                List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole())),
                nameAttributeKey
        );
    }

    private OAuthProvider getProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> OAuthProvider.GOOGLE;
            case "kakao" -> OAuthProvider.KAKAO;
            default -> throw new OAuth2AuthenticationException("지원하지 않는 OAuth Provider입니다.");
        };
    }

    private Member getOrCreateMember(OAuthProvider provider, OAuth2UserInfo userInfo) {
        return memberRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .map(member -> updateOAuthProfile(member, userInfo))
                .orElseGet(() -> createOAuthMember(provider, userInfo));
    }

    private Member updateOAuthProfile(Member member, OAuth2UserInfo userInfo) {
        member.updateOAuthProfile(
                userInfo.getNickname(),
                userInfo.getProfileImageUrl()
        );
        return member;
    }

    private Member createOAuthMember(OAuthProvider provider, OAuth2UserInfo userInfo) {
        String email = resolveEmail(provider, userInfo);
        String nickname = resolveNickname(userInfo);

        Member member = Member.createOAuthMember(
                email,
                nickname,
                userInfo.getProfileImageUrl(),
                provider,
                userInfo.getProviderId(),
                DEFAULT_ROLE,
                ACTIVE_STATUS
        );

        return memberRepository.save(member);
    }

    private String resolveEmail(OAuthProvider provider, OAuth2UserInfo userInfo) {
        String email = userInfo.getEmail();

        if (email != null && !email.isBlank()) {
            return email;
        }

        return provider.name().toLowerCase() + "_" + userInfo.getProviderId() + "@oauth.local";
    }

    private String resolveNickname(OAuth2UserInfo userInfo) {
        String nickname = userInfo.getNickname();

        if (nickname == null || nickname.isBlank()) {
            return "OAuth사용자";
        }

        if (nickname.length() > 30) {
            return nickname.substring(0, 30);
        }

        return nickname;
    }
}
