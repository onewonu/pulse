package com.pulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "seoul-api")
public class SeoulApiProperties {

    private String baseUrl;
    private String key;
    private int pageSize;
    private Services services = new Services();

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getKey() {
        return key;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Services getServices() {
        return services;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public static class Services {
        private String subway;

        public String getSubway() {
            return subway;
        }

        public void setSubway(String subway) {
            this.subway = subway;
        }
    }
}
