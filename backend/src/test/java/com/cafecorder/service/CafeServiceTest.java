package com.cafecorder.service;

import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitRequest;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.dto.VisitStats;
import com.cafecorder.exception.VisitNotFoundException;
import com.cafecorder.model.AppUser;
import com.cafecorder.model.Cafe;
import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;
import com.cafecorder.model.StudyCafe;
import com.cafecorder.repository.CafeRepository;
import com.cafecorder.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Plain JUnit + Mockito: no Spring context and no database, so these run in milliseconds
@ExtendWith(MockitoExtension.class)
class CafeServiceTest {

    private static final long ME = 1L;

    @Mock
    private CafeRepository repository;

    @Mock
    private UserRepository users;

    @InjectMocks
    private CafeService service;

    private final AppUser me = new AppUser("me@example.com", "Me", "hash");

    private VisitRequest regularVisit() {
        return new VisitRequest("  Sam James  ", " Harbord ", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, false, null, null, null, null, null);
    }

    private VisitRequest studyVisit() {
        return new VisitRequest("Sam James", "Harbord", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, true, true, false, Seating.LIMITED, "Before 5pm", NoiseLevel.QUIET);
    }

    @Test
    void createStudySessionSavesAStudyCafeForTheUser() {
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.create(ME, studyVisit());

        ArgumentCaptor<Cafe> saved = ArgumentCaptor.forClass(Cafe.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue()).isInstanceOf(StudyCafe.class);
        assertThat(saved.getValue().getUser()).isSameAs(me);
        assertThat(result.studySpot()).isTrue();
        assertThat(result.seating()).isEqualTo(Seating.LIMITED);
    }

    @Test
    void createRegularVisitTrimsText() {
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.create(ME, regularVisit());

        assertThat(result.name()).isEqualTo("Sam James");
        assertThat(result.neighbourhood()).isEqualTo("Harbord");
        assertThat(result.studySpot()).isFalse();
        assertThat(result.seating()).isNull();
    }

    @Test
    void listFiltersByNeighbourhoodOnlyWhenGiven() {
        when(repository.findByUserId(ME, SortOption.RATING.toSort())).thenReturn(List.of());
        when(repository.findByUserIdAndNeighbourhoodIgnoreCase(ME, "Annex", SortOption.RATING.toSort()))
                .thenReturn(List.of());

        service.list(ME, "  ", SortOption.RATING);
        service.list(ME, " Annex ", SortOption.RATING);

        verify(repository).findByUserId(ME, SortOption.RATING.toSort());
        verify(repository).findByUserIdAndNeighbourhoodIgnoreCase(ME, "Annex", SortOption.RATING.toSort());
    }

    @Test
    void updateSameTypeEditsInPlace() {
        Cafe existing = new Cafe("Old", "Annex", LocalDate.of(2026, 1, 1), "Tea", BigDecimal.ONE, 2);
        when(repository.findByIdAndUserId(5L, ME)).thenReturn(Optional.of(existing));

        VisitResponse result = service.update(ME, 5L, regularVisit());

        assertThat(result.name()).isEqualTo("Sam James");
        assertThat(result.rating()).isEqualTo(4);
        verify(repository, never()).delete(any());
        verify(repository, never()).save(any());
    }

    @Test
    void updateSwitchingToStudyReplacesTheRow() {
        Cafe existing = new Cafe("Old", "Annex", LocalDate.of(2026, 1, 1), "Tea", BigDecimal.ONE, 2);
        when(repository.findByIdAndUserId(5L, ME)).thenReturn(Optional.of(existing));
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.update(ME, 5L, studyVisit());

        verify(repository).delete(existing);
        verify(repository).save(any(StudyCafe.class));
        assertThat(result.studySpot()).isTrue();
    }

    @Test
    void cannotReadOrChangeSomeoneElsesVisit() {
        // the lookup includes the owner, so another user's visit 42 is simply not found
        when(repository.findByIdAndUserId(42L, ME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ME, 42L)).isInstanceOf(VisitNotFoundException.class);
        assertThatThrownBy(() -> service.update(ME, 42L, regularVisit())).isInstanceOf(VisitNotFoundException.class);
        assertThatThrownBy(() -> service.delete(ME, 42L)).isInstanceOf(VisitNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void statsForAnEmptyLogbook() {
        when(repository.countByUserId(ME)).thenReturn(0L);
        when(repository.countUniqueCafes(ME)).thenReturn(0L);
        when(repository.totalSpent(ME)).thenReturn(null);
        when(repository.averageRating(ME)).thenReturn(null);

        VisitStats stats = service.stats(ME);

        assertThat(stats.visits()).isZero();
        assertThat(stats.totalSpent()).isEqualByComparingTo("0");
        assertThat(stats.averageRating()).isNull();
    }
}
