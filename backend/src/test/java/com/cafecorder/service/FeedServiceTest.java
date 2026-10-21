package com.cafecorder.service;

import com.cafecorder.dto.FeedDtos.FeedItem;
import com.cafecorder.dto.FeedDtos.TopCafe;
import com.cafecorder.model.Cafe;
import com.cafecorder.model.CafeKind;
import com.cafecorder.repository.CafeRepository;
import com.cafecorder.repository.CafeRepository.TopCafeView;
import com.cafecorder.service.FeedService.FeedSort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private CafeRepository repository;

    @InjectMocks
    private FeedService feed;

    @Test
    void topSortAsksForBestRatedFirstAndCapsThePage() {
        when(repository.findBySharedTrue(any())).thenReturn(List.of());

        feed.photos(FeedSort.TOP, null, null, 0);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findBySharedTrue(page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(30);
        assertThat(page.getValue().getSort().getOrderFor("rating").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void laterPagesReachOlderPostsAndANegativePageMeansTheFirst() {
        when(repository.findBySharedTrue(any())).thenReturn(List.of());

        feed.photos(FeedSort.RECENT, null, null, 2);
        feed.photos(FeedSort.RECENT, null, null, -5);

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, times(2)).findBySharedTrue(page.capture());
        assertThat(page.getAllValues()).extracting(Pageable::getOffset).containsExactly(60L, 0L);
    }

    @Test
    void aKindNarrowsEveryTabAndNoKindMeansEverything() {
        when(repository.findBySharedTrueAndKindsContaining(eq(CafeKind.MATCHA), any())).thenReturn(List.of());

        feed.photos(FeedSort.RECENT, CafeKind.MATCHA, null, 0);
        feed.topCafes(FeedSort.TOP, CafeKind.MATCHA, null);
        feed.mapCafes(CafeKind.MATCHA, null);
        feed.topCafes(FeedSort.RECENT, null, null);

        verify(repository).findBySharedTrueAndKindsContaining(eq(CafeKind.MATCHA), any());
        verify(repository, never()).findBySharedTrue(any());
        verify(repository).findTopCafes("MATCHA", null, "TOP", 12);
        verify(repository).findMapCafes("MATCHA", null, 500);
        verify(repository).findTopCafes(null, null, "RECENT", 12);
    }

    @Test
    void aCityNarrowsEveryTabAndCombinesWithKindAndABlankCityMeansAll() {
        when(repository.findBySharedTrueAndCityIgnoreCase(eq("Dubai"), any())).thenReturn(List.of());
        when(repository.findBySharedTrueAndCityIgnoreCaseAndKindsContaining(eq("Dubai"), eq(CafeKind.YEMENI), any()))
                .thenReturn(List.of());
        when(repository.findBySharedTrue(any())).thenReturn(List.of());

        feed.photos(FeedSort.RECENT, null, " Dubai ", 0);
        feed.photos(FeedSort.RECENT, CafeKind.YEMENI, "Dubai", 0);
        feed.photos(FeedSort.RECENT, null, "  ", 0);
        feed.topCafes(FeedSort.TOP, CafeKind.YEMENI, "Dubai");
        feed.mapCafes(null, " Dubai");

        verify(repository).findBySharedTrueAndCityIgnoreCase(eq("Dubai"), any());
        verify(repository).findBySharedTrueAndCityIgnoreCaseAndKindsContaining(eq("Dubai"), eq(CafeKind.YEMENI), any());
        verify(repository).findBySharedTrue(any());
        verify(repository).findTopCafes("YEMENI", "Dubai", "TOP", 12);
        verify(repository).findMapCafes(null, "Dubai", 500);
    }

    @Test
    void visitsFromBeforeAccountsShowAGenericAuthor() {
        Cafe orphan = new Cafe("Sam James", "Harbord", LocalDate.of(2026, 9, 1), "Cortado", BigDecimal.TEN, 4);
        when(repository.findBySharedTrue(any())).thenReturn(List.of(orphan));

        List<FeedItem> items = feed.photos(FeedSort.RECENT, null, null, 0);

        assertThat(items).singleElement().extracting(FeedItem::author).isEqualTo("A CafeCorder user");
    }

    @Test
    void aPostWithTheNameHiddenHasNoAuthor() {
        Cafe hidden = new Cafe("Sam James", "Harbord", LocalDate.of(2026, 9, 1), "Cortado", BigDecimal.TEN, 4);
        hidden.setShowName(false);
        when(repository.findBySharedTrue(any())).thenReturn(List.of(hidden));

        List<FeedItem> items = feed.photos(FeedSort.RECENT, null, null, 0);

        assertThat(items).singleElement().extracting(FeedItem::author).isNull();
    }

    @Test
    void topCafesRoundsTheAverageToOneDecimal() {
        TopCafeView view = mock(TopCafeView.class);
        when(view.getName()).thenReturn("18Feet");
        when(view.getNeighbourhood()).thenReturn("Queen West");
        when(view.getAverageRating()).thenReturn(new BigDecimal("4.66666"));
        when(view.getVisits()).thenReturn(3L);
        when(repository.findTopCafes(null, null, "TOP", 12)).thenReturn(List.of(view));

        List<TopCafe> top = feed.topCafes(FeedSort.TOP, null, null);

        assertThat(top).singleElement().satisfies(c -> {
            assertThat(c.averageRating()).isEqualTo(4.7);
            assertThat(c.visits()).isEqualTo(3);
        });
    }
}
