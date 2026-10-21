package com.cafecorder.controller;

import com.cafecorder.dto.FeedDtos.FeedItem;
import com.cafecorder.dto.FeedDtos.MapCafe;
import com.cafecorder.dto.FeedDtos.TopCafe;
import com.cafecorder.model.CafeKind;
import com.cafecorder.service.FeedService;
import com.cafecorder.service.FeedService.FeedSort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Public: no token needed (see SecurityConfig)
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feed;

    public FeedController(FeedService feed) {
        this.feed = feed;
    }

    // GET /api/feed/photos?sort=TOP&kind=MATCHA&city=Toronto&page=1
    @GetMapping("/photos")
    public List<FeedItem> photos(@RequestParam(defaultValue = "RECENT") FeedSort sort,
                                 @RequestParam(required = false) CafeKind kind,
                                 @RequestParam(required = false) String city,
                                 @RequestParam(defaultValue = "0") int page) {
        return feed.photos(sort, kind, city, page);
    }

    @GetMapping("/top-cafes")
    public List<TopCafe> topCafes(@RequestParam(required = false) CafeKind kind,
                                  @RequestParam(required = false) String city) {
        return feed.topCafes(kind, city);
    }

    @GetMapping("/map")
    public List<MapCafe> map(@RequestParam(required = false) CafeKind kind,
                             @RequestParam(required = false) String city) {
        return feed.mapCafes(kind, city);
    }

    // cities with shared visits, for the filter
    @GetMapping("/cities")
    public List<String> cities() {
        return feed.cities();
    }
}
