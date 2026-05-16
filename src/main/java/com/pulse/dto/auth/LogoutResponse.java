package com.pulse.dto.auth;

public record LogoutResponse(
    String message
) {
    public static LogoutResponse of() {
        return new LogoutResponse("Logout successful");
    }
}
