package come.back.gotoday.auth.oauth;

import come.back.gotoday.member.entity.Member;
import come.back.gotoday.member.entity.OAuthProvider;
import come.back.gotoday.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String DEFAULT_NICKNAME = "OAuth사용자";
    private static final int MAX_NICKNAME_LENGTH = 100;

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

        validateProviderId(userInfo.getProviderId(), provider);

        Member member = getOrCreateMember(provider, userInfo);

        if (member.isDeleted()) {
            throw oauthException(
                    "deleted_member",
                    "탈퇴한 회원은 OAuth 로그인할 수 없습니다."
            );
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
            default -> throw new OAuth2AuthenticationException(new
                    org.springframework.security.oauth2.core.OAuth2Error("invalid_provider"), "지원하지 않는 OAuth provider입니다.");
        };
    }

    private void validateProviderId(String providerId, OAuthProvider provider) {
        if (providerId == null || providerId.isBlank() || "null".equalsIgnoreCase(providerId)) {
            throw oauthException(
                    "missing_provider_id",
                    provider.name() + " OAuth providerId를 찾을 수 없습니다."
            );
        }
    }

    private Member getOrCreateMember(OAuthProvider provider, OAuth2UserInfo userInfo) {
        return memberRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .map(member -> updateOAuthProfile(member, userInfo))
                .orElseGet(() -> createOAuthMember(provider, userInfo));
    }

    private Member updateOAuthProfile(Member member, OAuth2UserInfo userInfo) {
        String nickname = resolveNicknameForUpdate(member, userInfo.getNickname());

        member.updateOAuthProfile(
                nickname,
                userInfo.getProfileImageUrl()
        );

        return member;
    }

    private Member createOAuthMember(OAuthProvider provider, OAuth2UserInfo userInfo) {
        String email = resolveEmail(provider, userInfo);
        validateEmailNotUsedByOtherMember(email, provider, userInfo.getProviderId());

        String nickname = createUniqueNickname(userInfo.getNickname());

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

    private void validateEmailNotUsedByOtherMember(
            String email,
            OAuthProvider provider,
            String providerId
    ) {
        memberRepository.findByEmail(email)
                .ifPresent(existingMember -> {
                    boolean sameOAuthMember = provider.equals(existingMember.getProvider())
                            && providerId.equals(existingMember.getProviderId());

                    if (!sameOAuthMember) {
                        throw oauthException(
                                "email_already_exists",
                                "이미 다른 계정으로 가입된 이메일입니다."
                        );
                    }
                });
    }

    private String resolveNicknameForUpdate(Member member, String rawNickname) {
        if (rawNickname == null || rawNickname.isBlank()) {
            return member.getNickname();
        }

        String normalizedNickname = normalizeNickname(rawNickname);

        if (normalizedNickname.equals(member.getNickname())) {
            return normalizedNickname;
        }

        if (!memberRepository.existsByNickname(normalizedNickname)) {
            return normalizedNickname;
        }

        return member.getNickname();
    }

    private String createUniqueNickname(String rawNickname) {
        String baseNickname = normalizeNickname(rawNickname);

        if (!memberRepository.existsByNickname(baseNickname)) {
            return baseNickname;
        }

        for (int i = 0; i < 10; i++) {
            String suffix = "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String candidate = trimNickname(baseNickname, MAX_NICKNAME_LENGTH - suffix.length()) + suffix;

            if (!memberRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }

        String suffix = "_" + System.currentTimeMillis();
        return trimNickname(baseNickname, MAX_NICKNAME_LENGTH - suffix.length()) + suffix;
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_NICKNAME;
        }

        return trimNickname(nickname.trim(), MAX_NICKNAME_LENGTH);
    }

    private String trimNickname(String nickname, int maxLength) {
        if (nickname.length() <= maxLength) {
            return nickname;
        }

        return nickname.substring(0, maxLength);
    }

    private OAuth2AuthenticationException oauthException(String code, String description) {
        OAuth2Error error = new OAuth2Error(code, description, null);
        return new OAuth2AuthenticationException(error);
    }
}