package com.pulse.api.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KakaoUserInfoResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("properties")
    private Properties properties;

    public static class Properties {
        @JsonProperty("nickname")
        private String nickname;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    public Long getId() {
        return id;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
