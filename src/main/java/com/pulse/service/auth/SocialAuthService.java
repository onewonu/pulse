package com.pulse.service.auth;

import com.pulse.api.google.GoogleApiClient;
import com.pulse.api.google.dto.GoogleTokenInfoResponse;
import com.pulse.api.kakao.KakaoApiClient;
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
            String socialAccessToken
    ) {
        String providerId = getProviderIdFromToken(providerType, socialAccessToken);

        String finalNickname = (nickname == null || nickname.trim().isEmpty())
                ? "User_" + UUID.randomUUID().toString().substring(0, 8)
                : nickname;

        return findOrCreateUser(providerType, providerId, finalNickname);
    }

    private String getProviderIdFromToken(ProviderType providerType, String socialAccessToken) {
        switch (providerType) {
            case KAKAO:
                KakaoUserInfoResponse kakaoInfo = kakaoApiClient.getUserInfo(socialAccessToken);
                log.info("Retrieved providerId from Kakao: {}", kakaoInfo.getId());
                return String.valueOf(kakaoInfo.getId());

            case GOOGLE:
                GoogleTokenInfoResponse googleInfo = googleApiClient.getTokenInfo(socialAccessToken);
                log.info("Retrieved providerId from Google: {}", googleInfo.getSub());
                return googleInfo.getSub();

            default:
                throw new IllegalArgumentException("Unsupported provider type: " + providerType);
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
