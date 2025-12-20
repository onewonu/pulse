package com.pulse.api.seoulopendata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResult {

    @JsonProperty("CODE")
    private String code;

    @JsonProperty("MESSAGE")
    private String message;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return "INFO-000".equals(code);
    }
}
