package com.pulse.dto.user;

import com.pulse.entity.user.User;

public class UserInfoResponse {

    private final Long id;
    private final String nickname;

    private UserInfoResponse(Long id, String nickname) {
        this.id = id;
        this.nickname = nickname;
    }

    public static UserInfoResponse of(User user) {
        return new UserInfoResponse(user.getId(), user.getNickname());
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }
}
