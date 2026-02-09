package com.pulse.dto.user;

import com.pulse.entity.user.User;

public record UserInfoResponse(
    Long id,
    String nickname
) {
    public static UserInfoResponse of(User user) {
        return new UserInfoResponse(user.getId(), user.getNickname());
    }
}
