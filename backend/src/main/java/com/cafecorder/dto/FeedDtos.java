package com.cafecorder.dto;

import com.cafecorder.model.Cafe;
import com.cafecorder.repository.CafeRepository.TopCafeView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

// What the public feed shows: the cafe, the drink, the price, the rating and who shared it. Never the email.
public final class FeedDtos {

    private FeedDtos() {
    }

    public record FeedItem(Long id, String name, String neighbourhood, LocalDate visitDate, String order,
                           BigDecimal price, List<ExtraItem> extras, BigDecimal total,
                           int rating, String photoUrl, boolean studySpot, String author) {
        public static FeedItem from(Cafe c) {
            String author = c.getUser() != null ? c.getUser().getDisplayName() : "A CafeCorder user";
            return new FeedItem(c.getId(), c.getName(), c.getNeighbourhood(), c.getVisitDate(), c.getOrder(),
                    c.getPrice(), ExtraItem.from(c.getExtras()), c.getTotal(),
                    c.getRating(), c.getPhotoUrl(), c.isStudySpot(), author);
        }
    }

    public record TopCafe(String name, String neighbourhood, double averageRating, long visits, String coverPhoto) {
        public static TopCafe from(TopCafeView v) {
            return new TopCafe(v.getName(), v.getNeighbourhood(),
                    v.getAverageRating().setScale(1, RoundingMode.HALF_UP).doubleValue(),
                    v.getVisits(), v.getCoverPhoto());
        }
    }
}
