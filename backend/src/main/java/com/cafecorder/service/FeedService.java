package com.cafecorder.service;

import com.cafecorder.dto.FeedDtos.FeedItem;
import com.cafecorder.dto.FeedDtos.MapCafe;
import com.cafecorder.dto.FeedDtos.TopCafe;
import com.cafecorder.model.Cafe;
import com.cafecorder.model.CafeKind;
import com.cafecorder.repository.CafeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

// The public side: everyone's shared visits
@Service
@Transactional(readOnly = true)
public class FeedService {

    public enum FeedSort { RECENT, TOP }

    static final int PAGE_SIZE = 30;
    static final int TOP_CAFES = 12;
    static final int MAP_PINS = 500;

    private final CafeRepository repository;

    public FeedService(CafeRepository repository) {
        this.repository = repository;
    }

    // page 0 is the newest (or best) 30; the frontend asks for the next page with "Load more"
    // kind and city are optional on all three: null (or a blank city) means every cafe
    public List<FeedItem> photos(FeedSort sort, CafeKind kind, String city, int page) {
        Sort order = sort == FeedSort.TOP
                ? Sort.by(DESC, "rating").and(Sort.by(DESC, "visitDate")).and(Sort.by(DESC, "id"))
                : Sort.by(DESC, "visitDate").and(Sort.by(DESC, "id"));
        PageRequest request = PageRequest.of(Math.max(page, 0), PAGE_SIZE, order);
        String place = trimToNull(city);
        List<Cafe> visits;
        if (place == null) {
            visits = kind == null
                    ? repository.findBySharedTrue(request)
                    : repository.findBySharedTrueAndKindsContaining(kind, request);
        } else {
            visits = kind == null
                    ? repository.findBySharedTrueAndCityIgnoreCase(place, request)
                    : repository.findBySharedTrueAndCityIgnoreCaseAndKindsContaining(place, kind, request);
        }
        return visits.stream().map(FeedItem::from).toList();
    }

    public List<TopCafe> topCafes(FeedSort sort, CafeKind kind, String city) {
        return repository.findTopCafes(nameOf(kind), trimToNull(city), sort.name(), TOP_CAFES).stream()
                .map(TopCafe::from).toList();
    }

    public List<MapCafe> mapCafes(CafeKind kind, String city) {
        return repository.findMapCafes(nameOf(kind), trimToNull(city), MAP_PINS).stream().map(MapCafe::from).toList();
    }

    public List<String> cities() {
        return repository.findSharedCities();
    }

    private static String nameOf(CafeKind kind) {
        return kind == null ? null : kind.name();
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
