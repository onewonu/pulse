package com.pulse.service.auth;

import com.pulse.api.google.GoogleApiClient;
import com.pulse.api.google.dto.GoogleTokenInfoResponse;
import com.pulse.api.kakao.KakaoApiClient;
import com.pulse.api.kakao.dto.KakaoUserInfoResponse;
import com.pulse.entity.user.ProviderType;
import com.pulse.entity.user.User;
import com.pulse.exception.auth.SocialLoginException;
import com.pulse.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class SocialAuthService {

    private static final Logger log = LoggerFactory.getLogger(SocialAuthService.class);

    private final UserRepository userRepository;
    private final KakaoApiClient kakaoApiClient;
    private final GoogleApiClient googleApiClient;

    public SocialAuthService(
            UserRepository userRepository,
            KakaoApiClient kakaoApiClient,
            GoogleApiClient googleApiClient
    ) {
        this.userRepository = userRepository;
        this.kakaoApiClient = kakaoApiClient;
        this.googleApiClient = googleApiClient;
    }

    @Transactional
    public User authenticateAndGetUser(
            ProviderType providerType,
            String providerId,
            String nickname,
            String socialAccessToken
    ) {
        verifySocialToken(providerType, providerId, socialAccessToken);

        String finalNickname = (nickname == null || nickname.trim().isEmpty())
                ? "User_" + UUID.randomUUID().toString().substring(0, 8)
                : nickname;

        return findOrCreateUser(providerType, providerId, finalNickname);
    }

    private void verifySocialToken(
            ProviderType providerType,
            String providerId,
            String socialAccessToken
    ) {
        switch (providerType) {
            case KAKAO:
                KakaoUserInfoResponse kakaoInfo = kakaoApiClient.getUserInfo(socialAccessToken);
                if (!String.valueOf(kakaoInfo.getId()).equals(providerId)) {
                    log.error("ProviderId mismatch for Kakao: expected={}, actual={}", providerId, kakaoInfo.getId());
                    throw new SocialLoginException("ProviderId mismatch");
                }
                break;

            case GOOGLE:
                GoogleTokenInfoResponse googleInfo = googleApiClient.getTokenInfo(socialAccessToken);
                if (!googleInfo.getSub().equals(providerId)) {
                    log.error(
                            "ProviderId mismatch for Google: expected={}, actual={}", providerId, googleInfo.getSub()
                    );
                    throw new SocialLoginException("ProviderId mismatch");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported provider type: " + providerType);
        }

        log.info("Social token verified successfully for provider: {}", providerType);
    }

    public User findOrCreateUser(ProviderType providerType, String providerId, String nickname) {
        Optional<User> optionalUser = userRepository.findByProviderTypeAndProviderId(providerType, providerId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (!user.getNickname().equals(nickname)) {
                user.updateNickname(nickname);
                log.info("Updated nickname for user: providerId={}", providerId);
            }
            return user;
        } else {
            log.info("Creating new user: provider={}, providerId={}", providerType, providerId);
            User newUser = User.of(nickname, providerType, providerId);
            return userRepository.save(newUser);
        }
    }
}
