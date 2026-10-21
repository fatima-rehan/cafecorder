package com.cafecorder.dto;

import com.cafecorder.model.Cafe;
import com.cafecorder.model.CafeKind;
import com.cafecorder.repository.CafeRepository.MapCafeView;
import com.cafecorder.repository.CafeRepository.TopCafeView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

// What the public feed shows: the cafe, the drink, the price, the rating and who shared it. Never the email.
public final class FeedDtos {

    private FeedDtos() {
    }

    // author is null when the poster chose to hide their name
    public record FeedItem(Long id, String name, String neighbourhood, LocalDate visitDate, String order,
                           BigDecimal price, List<ExtraItem> extras, BigDecimal total,
                           int rating, String photoUrl, boolean studySpot, String author, String address, String city,
                           List<CafeKind> kinds) {
        public static FeedItem from(Cafe c) {
            String author = !c.isShowName() ? null
                    : c.getUser() != null ? c.getUser().getDisplayName() : "A CafeCorder user";
            return new FeedItem(c.getId(), c.getName(), c.getNeighbourhood(), c.getVisitDate(), c.getOrder(),
                    c.getPrice(), ExtraItem.from(c.getExtras()), c.getTotal(),
                    c.getRating(), c.getPhotoUrl(), c.isStudySpot(), author, c.getAddress(), c.getCity(),
                    VisitResponse.sortedKinds(c));
        }
    }

    // One pin on the map: every shared visit to the same cafe (name + address) rolled into one.
    // averageRating is null when none of the visits were rated.
    public record MapCafe(String name, String address, String city, double latitude, double longitude, long visits,
                          Double averageRating, String coverPhoto) {
        public static MapCafe from(MapCafeView v) {
            Double rating = v.getAverageRating() == null ? null
                    : v.getAverageRating().setScale(1, RoundingMode.HALF_UP).doubleValue();
            return new MapCafe(v.getName(), v.getAddress(), v.getCity(), v.getLatitude(), v.getLongitude(), v.getVisits(),
                    rating, v.getCoverPhoto());
        }
    }

    // city is null when none of the visits had one
    public record TopCafe(String name, String neighbourhood, String city, double averageRating, long visits,
                          String coverPhoto) {
        public static TopCafe from(TopCafeView v) {
            return new TopCafe(v.getName(), v.getNeighbourhood(), v.getCity(),
                    v.getAverageRating().setScale(1, RoundingMode.HALF_UP).doubleValue(),
                    v.getVisits(), v.getCoverPhoto());
        }
    }
}
