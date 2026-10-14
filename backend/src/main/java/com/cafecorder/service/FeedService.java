package com.cafecorder.service;

import com.cafecorder.dto.FeedDtos.FeedItem;
import com.cafecorder.dto.FeedDtos.MapCafe;
import com.cafecorder.dto.FeedDtos.TopCafe;
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
    public List<FeedItem> photos(FeedSort sort, int page) {
        Sort order = sort == FeedSort.TOP
                ? Sort.by(DESC, "rating").and(Sort.by(DESC, "visitDate")).and(Sort.by(DESC, "id"))
                : Sort.by(DESC, "visitDate").and(Sort.by(DESC, "id"));
        return repository.findBySharedTrue(PageRequest.of(Math.max(page, 0), PAGE_SIZE, order)).stream()
                .map(FeedItem::from)
                .toList();
    }

    public List<TopCafe> topCafes() {
        return repository.findTopCafes(TOP_CAFES).stream().map(TopCafe::from).toList();
    }

    public List<MapCafe> mapCafes() {
        return repository.findMapCafes(MAP_PINS).stream().map(MapCafe::from).toList();
    }
}
