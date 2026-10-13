package com.cafecorder.service;

import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitRequest;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.dto.VisitStats;
import com.cafecorder.exception.VisitNotFoundException;
import com.cafecorder.model.Cafe;
import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;
import com.cafecorder.model.StudyCafe;
import com.cafecorder.repository.CafeRepository;
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

    @Mock
    private CafeRepository repository;

    @InjectMocks
    private CafeService service;

    private VisitRequest regularVisit() {
        return new VisitRequest("  Sam James  ", " Harbord ", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, false, null, null, null, null, null);
    }

    private VisitRequest studyVisit() {
        return new VisitRequest("Sam James", "Harbord", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, true, true, false, Seating.LIMITED, "Before 5pm", NoiseLevel.QUIET);
    }

    @Test
    void createStudySessionSavesAStudyCafe() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.create(studyVisit());

        ArgumentCaptor<Cafe> saved = ArgumentCaptor.forClass(Cafe.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue()).isInstanceOf(StudyCafe.class);
        assertThat(result.studySpot()).isTrue();
        assertThat(result.seating()).isEqualTo(Seating.LIMITED);
    }

    @Test
    void createRegularVisitTrimsText() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.create(regularVisit());

        assertThat(result.name()).isEqualTo("Sam James");
        assertThat(result.neighbourhood()).isEqualTo("Harbord");
        assertThat(result.studySpot()).isFalse();
        assertThat(result.seating()).isNull();
    }

    @Test
    void listFiltersByNeighbourhoodOnlyWhenGiven() {
        when(repository.findAll(SortOption.RATING.toSort())).thenReturn(List.of());
        when(repository.findByNeighbourhoodIgnoreCase("Annex", SortOption.RATING.toSort())).thenReturn(List.of());

        service.list("  ", SortOption.RATING);
        service.list(" Annex ", SortOption.RATING);

        verify(repository).findAll(SortOption.RATING.toSort());
        verify(repository).findByNeighbourhoodIgnoreCase("Annex", SortOption.RATING.toSort());
    }

    @Test
    void updateSameTypeEditsInPlace() {
        Cafe existing = new Cafe("Old", "Annex", LocalDate.of(2026, 1, 1), "Tea", BigDecimal.ONE, 2);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        VisitResponse result = service.update(5L, regularVisit());

        assertThat(result.name()).isEqualTo("Sam James");
        assertThat(result.rating()).isEqualTo(4);
        verify(repository, never()).delete(any());
        verify(repository, never()).save(any());
    }

    @Test
    void updateSwitchingToStudyReplacesTheRow() {
        Cafe existing = new Cafe("Old", "Annex", LocalDate.of(2026, 1, 1), "Tea", BigDecimal.ONE, 2);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.update(5L, studyVisit());

        verify(repository).delete(existing);
        verify(repository).save(any(StudyCafe.class));
        assertThat(result.studySpot()).isTrue();
    }

    @Test
    void missingVisitThrowsNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(42L)).isInstanceOf(VisitNotFoundException.class);
        assertThatThrownBy(() -> service.delete(42L)).isInstanceOf(VisitNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void statsForAnEmptyLogbook() {
        when(repository.count()).thenReturn(0L);
        when(repository.countUniqueCafes()).thenReturn(0L);
        when(repository.totalSpent()).thenReturn(null);
        when(repository.averageRating()).thenReturn(null);

        VisitStats stats = service.stats();

        assertThat(stats.totalSpent()).isEqualByComparingTo("0");
        assertThat(stats.averageRating()).isNull();
    }
}
