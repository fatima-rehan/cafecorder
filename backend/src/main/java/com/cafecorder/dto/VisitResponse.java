package com.cafecorder.dto;

import com.cafecorder.model.Cafe;
import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;
import com.cafecorder.model.StudyCafe;

import java.math.BigDecimal;
import java.time.LocalDate;

// What the API sends back. A DTO, not the entity, so the JSON shape doesn't change if the table does.
public record VisitResponse(
        Long id,
        String name,
        String neighbourhood,
        LocalDate visitDate,
        String order,
        BigDecimal price,
        int rating,
        boolean studySpot,
        Boolean hasWifi,
        Boolean hasOutlets,
        Seating seating,
        String laptopPolicy,
        NoiseLevel noiseLevel,
        String photoUrl,
        boolean shared
) {
    public static VisitResponse from(Cafe cafe) {
        if (cafe instanceof StudyCafe s) {
            return new VisitResponse(s.getId(), s.getName(), s.getNeighbourhood(), s.getVisitDate(), s.getOrder(),
                    s.getPrice(), s.getRating(), true,
                    s.getHasWifi(), s.getHasOutlets(), s.getSeating(), s.getLaptopPolicy(), s.getNoiseLevel(),
                    s.getPhotoUrl(), s.isShared());
        }
        return new VisitResponse(cafe.getId(), cafe.getName(), cafe.getNeighbourhood(), cafe.getVisitDate(),
                cafe.getOrder(), cafe.getPrice(), cafe.getRating(), false,
                null, null, null, null, null, cafe.getPhotoUrl(), cafe.isShared());
    }
}
