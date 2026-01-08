package com.pulse.api.google.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GoogleTokenInfoResponse {

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    public String getSub() {
        return sub;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
