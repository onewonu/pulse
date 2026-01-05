package com.pulse.util;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public final class DayCodeConverter {

    private DayCodeConverter() {}

    public static int convert(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            return 3;
        } else if (dayOfWeek == DayOfWeek.SATURDAY) {
            return 2;
        }

        return 1;
    }

    public static String toDayType(int dayCode) {
        return switch (dayCode) {
            case 1 -> "평일";
            case 2, 3 -> "주말";
            default -> throw new IllegalArgumentException("Invalid day code: " + dayCode);
        };
    }
}
