package com.pulse.service.auth;

import com.pulse.api.google.GoogleApiClient;
import com.pulse.api.google.dto.GoogleTokenInfoResponse;
import com.pulse.api.google.dto.GoogleTokenResponse;
import com.pulse.api.kakao.KakaoApiClient;
import com.pulse.api.kakao.dto.KakaoTokenResponse;
import com.pulse.api.kakao.dto.KakaoUserInfoResponse;
import com.pulse.entity.user.ProviderType;
import com.pulse.entity.user.User;
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
            String nickname,
            String authorizationCode,
            String redirectUri
    ) {
        String providerId = getProviderIdFromAuthCode(providerType, authorizationCode, redirectUri);

        String finalNickname = (nickname == null || nickname.trim().isEmpty())
                ? "User_" + UUID.randomUUID().toString().substring(0, 8)
                : nickname;

        return findOrCreateUser(providerType, providerId, finalNickname);
    }

    private String getProviderIdFromAuthCode(ProviderType providerType, String authorizationCode, String redirectUri) {
        switch (providerType) {
            case KAKAO:
                KakaoTokenResponse kakaoToken = kakaoApiClient.getAccessToken(authorizationCode, redirectUri);
                KakaoUserInfoResponse kakaoInfo = kakaoApiClient.getUserInfo(kakaoToken.getAccessToken());
                return String.valueOf(kakaoInfo.getId());

            case GOOGLE:
                GoogleTokenResponse googleToken = googleApiClient.getAccessToken(authorizationCode, redirectUri);
                GoogleTokenInfoResponse googleInfo = googleApiClient.getUserInfo(googleToken.getAccessToken());
                return googleInfo.getSub();

            default:
                throw new AssertionError("Unreachable code");
        }
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
