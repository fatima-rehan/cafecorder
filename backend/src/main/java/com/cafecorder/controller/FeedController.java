package com.cafecorder.controller;

import com.cafecorder.dto.FeedDtos.FeedItem;
import com.cafecorder.dto.FeedDtos.MapCafe;
import com.cafecorder.dto.FeedDtos.TopCafe;
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

    // GET /api/feed/photos?sort=TOP&page=1
    @GetMapping("/photos")
    public List<FeedItem> photos(@RequestParam(defaultValue = "RECENT") FeedSort sort,
                                 @RequestParam(defaultValue = "0") int page) {
        return feed.photos(sort, page);
    }

    @GetMapping("/top-cafes")
    public List<TopCafe> topCafes() {
        return feed.topCafes();
    }

    @GetMapping("/map")
    public List<MapCafe> map() {
        return feed.mapCafes();
    }
}
