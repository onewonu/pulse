package com.pulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "odsay-api")
public class OdsayApiProperties {

    private String baseUrl;
    private String key;
    private int displayCount;
    private String referer;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getKey() {
        return key;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public String getReferer() {
        return referer;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDisplayCount(int displayCount) {
        this.displayCount = displayCount;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }
}
