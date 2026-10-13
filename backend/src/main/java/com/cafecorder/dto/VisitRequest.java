package com.cafecorder.dto;

import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;

import java.math.BigDecimal;
import java.time.LocalDate;

// What the frontend sends to create or update a visit. Replaces the old Scanner prompts.
public record VisitRequest(
        String name,
        String neighbourhood,
        LocalDate visitDate,
        String order,
        BigDecimal price,
        Integer rating,
        // Boolean, not boolean: Jackson 3 rejects a missing primitive, and missing should just mean "no"
        Boolean studySession,
        Boolean hasWifi,
        Boolean hasOutlets,
        Seating seating,
        String laptopPolicy,
        NoiseLevel noiseLevel
) {
    public boolean study() {
        return Boolean.TRUE.equals(studySession);
    }
}
