package com.pulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "seoul-metro-api")
public class SeoulMetroApiProperties {

    private String baseUrl;
    private String key;
    private String dataType;
    private int numOfRows;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getKey() {
        return key;
    }

    public String getDataType() {
        return dataType;
    }

    public int getNumOfRows() {
        return numOfRows;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public void setNumOfRows(int numOfRows) {
        this.numOfRows = numOfRows;
    }
}
