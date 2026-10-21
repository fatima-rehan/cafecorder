package com.cafecorder.service;

import com.cafecorder.dto.ExtraItem;
import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitRequest;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.dto.VisitStats;
import com.cafecorder.exception.VisitNotFoundException;
import com.cafecorder.model.AppUser;
import com.cafecorder.model.Cafe;
import com.cafecorder.model.CafeKind;
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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private Geocoder geocoder;

    @InjectMocks
    private CafeService service;

    private final AppUser me = new AppUser("me@example.com", "Me", "hash");

    private VisitRequest regularVisit() {
        return new VisitRequest("  Sam James  ", " Harbord ", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, false, null, null, null, null, null, null, false, null, null, null, null, null, null, null);
    }

    private VisitRequest visitAt(String address, String city, Double lat, Double lng) {
        return new VisitRequest("Sam James", "Harbord", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, false, null, null, null, null, null, null, true, null, address, city, lat, lng, null, null);
    }

    private VisitRequest visitAt(String address) {
        return visitAt(address, null, null, null);
    }

    @Test
    void anAddressPickedInTheAppKeepsItsCoordinates() {
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.create(ME, visitAt("117 Ossington Avenue", "Toronto", 43.6466, -79.4196));

        assertThat(result.city()).isEqualTo("Toronto");
        assertThat(result.latitude()).isEqualTo(43.6466);
        verifyNoInteractions(geocoder);
    }

    @Test
    void aTypedAddressIsLookedUpWithItsCity() {
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(geocoder.locate("Place des Vosges, Paris")).thenReturn(Optional.of(new Geocoder.Coordinates(48.85, 2.36)));

        VisitResponse result = service.create(ME, visitAt("Place des Vosges", "Paris", null, null));

        assertThat(result.longitude()).isEqualTo(2.36);
    }

    @Test
    void anAddressIsLookedUpAndPinned() {
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(geocoder.locate("297 Harbord St, Toronto")).thenReturn(Optional.of(new Geocoder.Coordinates(43.66, -79.42)));

        VisitResponse result = service.create(ME, visitAt(" 297 Harbord St, Toronto "));

        assertThat(result.address()).isEqualTo("297 Harbord St, Toronto");
        assertThat(result.latitude()).isEqualTo(43.66);
        assertThat(result.longitude()).isEqualTo(-79.42);
    }

    @Test
    void anAddressThatIsNotFoundStillSaves() {
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(geocoder.locate("nowhere at all")).thenReturn(Optional.empty());

        VisitResponse result = service.create(ME, visitAt("nowhere at all"));

        assertThat(result.address()).isEqualTo("nowhere at all");
        assertThat(result.latitude()).isNull();
    }

    @Test
    void editingWithoutChangingTheAddressSkipsTheLookup() {
        Cafe existing = new Cafe("Sam James", "Harbord", LocalDate.of(2026, 1, 1), "Tea", BigDecimal.ONE, 2);
        existing.setLocation("297 Harbord St, Toronto", null, 43.66, -79.42);
        when(repository.findByIdAndUserId(5L, ME)).thenReturn(Optional.of(existing));

        VisitResponse result = service.update(ME, 5L, visitAt("297 Harbord St, Toronto"));

        assertThat(result.latitude()).isEqualTo(43.66);
        verifyNoInteractions(geocoder);
    }

    private VisitRequest studyVisit() {
        return new VisitRequest("Sam James", "Harbord", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, true, true, false, Seating.LIMITED, "Before 5pm", NoiseLevel.QUIET,
                "https://example.com/cortado.jpg", null, null, null, null, null, null, Set.of(CafeKind.TEA_HOUSE), false);
    }

    private VisitRequest visitWithExtras() {
        return new VisitRequest("Sam James", "Harbord", LocalDate.of(2026, 9, 10), "Cortado",
                new BigDecimal("4.50"), 4, false, null, null, null, null, null, null, true,
                List.of(new ExtraItem(" Almond croissant ", new BigDecimal("4.25")),
                        new ExtraItem("Cookie", new BigDecimal("2.10"))), null, null, null, null, null, null);
    }

    @Test
    void extrasAreSavedInOrderAndAddedToTheTotal() {
        when(users.getReferenceById(ME)).thenReturn(me);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VisitResponse result = service.create(ME, visitWithExtras());

        assertThat(result.extras()).extracting(ExtraItem::name).containsExactly("Almond croissant", "Cookie");
        assertThat(result.price()).isEqualByComparingTo("4.50");
        assertThat(result.total()).isEqualByComparingTo("10.85");
    }

    @Test
    void editingAVisitReplacesItsExtras() {
        Cafe existing = new Cafe("Old", "Annex", LocalDate.of(2026, 1, 1), "Tea", BigDecimal.ONE, 2);
        when(repository.findByIdAndUserId(5L, ME)).thenReturn(Optional.of(existing));

        service.update(ME, 5L, visitWithExtras());
        VisitResponse result = service.update(ME, 5L, regularVisit());

        assertThat(result.extras()).isEmpty();
        assertThat(result.total()).isEqualByComparingTo("4.50");
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
        assertThat(result.photoUrl()).isEqualTo("https://example.com/cortado.jpg");
        assertThat(result.shared()).as("shared defaults to true when not sent").isTrue();
        assertThat(result.kinds()).containsExactly(CafeKind.TEA_HOUSE);
        assertThat(result.showName()).isFalse();
    }

    @Test
    void editingWithoutKindsClearsTheOldOnes() {
        Cafe existing = new Cafe("Old", "Annex", LocalDate.of(2026, 1, 1), "Tea", BigDecimal.ONE, 2);
        existing.setKinds(Set.of(CafeKind.MATCHA, CafeKind.YEMENI));
        when(repository.findByIdAndUserId(5L, ME)).thenReturn(Optional.of(existing));

        VisitResponse result = service.update(ME, 5L, regularVisit());

        assertThat(result.kinds()).isEmpty();
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
        assertThat(result.shared()).isFalse();
        assertThat(result.showName()).as("the name shows unless you opt out").isTrue();
    }

    @Test
    void listFiltersByNeighbourhoodOnlyWhenGiven() {
        when(repository.findByUserId(ME, SortOption.RATING.toSort())).thenReturn(List.of());
        when(repository.findByUserIdAndNeighbourhoodIgnoreCase(ME, "Annex", SortOption.RATING.toSort()))
                .thenReturn(List.of());

        service.list(ME, "  ", null, SortOption.RATING);
        service.list(ME, " Annex ", null, SortOption.RATING);

        verify(repository).findByUserId(ME, SortOption.RATING.toSort());
        verify(repository).findByUserIdAndNeighbourhoodIgnoreCase(ME, "Annex", SortOption.RATING.toSort());
    }

    @Test
    void listFiltersByKindAloneOrTogetherWithTheArea() {
        Sort recent = SortOption.RECENT.toSort();
        when(repository.findByUserIdAndKindsContaining(ME, CafeKind.MATCHA, recent)).thenReturn(List.of());
        when(repository.findByUserIdAndNeighbourhoodIgnoreCaseAndKindsContaining(ME, "Annex", CafeKind.YEMENI, recent))
                .thenReturn(List.of());

        service.list(ME, null, CafeKind.MATCHA, SortOption.RECENT);
        service.list(ME, "Annex", CafeKind.YEMENI, SortOption.RECENT);

        verify(repository).findByUserIdAndKindsContaining(ME, CafeKind.MATCHA, recent);
        verify(repository).findByUserIdAndNeighbourhoodIgnoreCaseAndKindsContaining(ME, "Annex", CafeKind.YEMENI, recent);
        verify(repository, never()).findByUserId(any(), any());
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

    @Test
    void totalSpentIncludesExtras() {
        when(repository.totalSpent(ME)).thenReturn(new BigDecimal("12.00"));
        when(repository.extrasSpent(ME)).thenReturn(new BigDecimal("4.25"));

        assertThat(service.stats(ME).totalSpent()).isEqualByComparingTo("16.25");
    }
}
