package com.cafecorder.dto;

import com.cafecorder.model.Cafe;
import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;
import com.cafecorder.model.StudyCafe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        boolean shared,
        List<ExtraItem> extras,
        // drink plus extras
        BigDecimal total,
        String address,
        String city,
        // null when the address wasn't found
        Double latitude,
        Double longitude
) {
    public static VisitResponse from(Cafe cafe) {
        List<ExtraItem> extras = ExtraItem.from(cafe.getExtras());
        if (cafe instanceof StudyCafe s) {
            return new VisitResponse(s.getId(), s.getName(), s.getNeighbourhood(), s.getVisitDate(), s.getOrder(),
                    s.getPrice(), s.getRating(), true,
                    s.getHasWifi(), s.getHasOutlets(), s.getSeating(), s.getLaptopPolicy(), s.getNoiseLevel(),
                    s.getPhotoUrl(), s.isShared(), extras, s.getTotal(),
                    s.getAddress(), s.getCity(), s.getLatitude(), s.getLongitude());
        }
        return new VisitResponse(cafe.getId(), cafe.getName(), cafe.getNeighbourhood(), cafe.getVisitDate(),
                cafe.getOrder(), cafe.getPrice(), cafe.getRating(), false,
                null, null, null, null, null, cafe.getPhotoUrl(), cafe.isShared(), extras, cafe.getTotal(),
                cafe.getAddress(), cafe.getCity(), cafe.getLatitude(), cafe.getLongitude());
    }
}
