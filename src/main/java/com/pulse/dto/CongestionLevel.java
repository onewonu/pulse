package com.pulse.dto;

public enum CongestionLevel {
    LOW,
    MEDIUM,
    HIGH;

    public static CongestionLevel fromScore(double score) {
         if (score < 2000) {
            return LOW;
        } else if (score < 5000) {
            return MEDIUM;
        } else {
            return HIGH;
        }
    }
}
