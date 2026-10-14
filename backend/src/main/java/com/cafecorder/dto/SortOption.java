package com.cafecorder.dto;

import org.springframework.data.domain.Sort;

import static org.springframework.data.domain.Sort.Direction.DESC;

// ?sort=RECENT|RATING. Sorting happens in SQL, not in Java like the old CafeTracker.
public enum SortOption {
    RECENT(Sort.by(DESC, "visitDate").and(Sort.by(DESC, "id"))),
    // like the old compareTo: best first, unrated (0) at the bottom
    RATING(Sort.by(DESC, "rating").and(Sort.by(DESC, "visitDate")));

    private final Sort sort;

    SortOption(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
