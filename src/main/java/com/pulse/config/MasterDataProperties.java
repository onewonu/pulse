package com.pulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "masterdata")
public class MasterDataProperties {

    private String linesPath = "classpath:data/lines.json";
    private String stationsPath = "classpath:data/stations.json";

    public String getLinesPath() {
        return linesPath;
    }

    public void setLinesPath(String linesPath) {
        this.linesPath = linesPath;
    }

    public String getStationsPath() {
        return stationsPath;
    }

    public void setStationsPath(String stationsPath) {
        this.stationsPath = stationsPath;
    }
}
