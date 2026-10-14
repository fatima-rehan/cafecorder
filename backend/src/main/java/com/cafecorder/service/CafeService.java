package com.cafecorder.service;

import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitRequest;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.dto.VisitStats;
import com.cafecorder.exception.VisitNotFoundException;
import com.cafecorder.model.Cafe;
import com.cafecorder.model.Extra;
import com.cafecorder.model.StudyCafe;
import com.cafecorder.repository.CafeRepository;
import com.cafecorder.repository.UserRepository;
import com.cafecorder.service.Geocoder.Coordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// What CafeTracker used to do, with Postgres instead of an ArrayList. Every method works on one user's visits.
@Service
@Transactional(readOnly = true)
public class CafeService {

    private final CafeRepository repository;
    private final UserRepository users;
    private final Geocoder geocoder;

    // constructor injection: Spring passes in the dependencies, and tests can pass in mocks
    public CafeService(CafeRepository repository, UserRepository users, Geocoder geocoder) {
        this.repository = repository;
        this.users = users;
        this.geocoder = geocoder;
    }

    public List<VisitResponse> list(Long userId, String neighbourhood, SortOption sort) {
        List<Cafe> cafes = (neighbourhood == null || neighbourhood.isBlank())
                ? repository.findByUserId(userId, sort.toSort())
                : repository.findByUserIdAndNeighbourhoodIgnoreCase(userId, neighbourhood.trim(), sort.toSort());
        return cafes.stream().map(VisitResponse::from).toList();
    }

    public VisitResponse get(Long userId, Long id) {
        return VisitResponse.from(find(userId, id));
    }

    public List<String> neighbourhoods(Long userId) {
        return repository.findNeighbourhoods(userId);
    }

    public VisitStats stats(Long userId) {
        BigDecimal total = Objects.requireNonNullElse(repository.totalSpent(userId), BigDecimal.ZERO)
                .add(Objects.requireNonNullElse(repository.extrasSpent(userId), BigDecimal.ZERO));
        return new VisitStats(repository.countByUserId(userId), repository.countUniqueCafes(userId), total,
                repository.averageRating(userId));
    }

    @Transactional
    public VisitResponse create(Long userId, VisitRequest request) {
        Cafe cafe = toEntity(userId, request);
        applyAddress(cafe, request, null);
        return VisitResponse.from(repository.save(cafe));
    }

    @Transactional
    public VisitResponse update(Long userId, Long id, VisitRequest request) {
        Cafe existing = find(userId, id);

        // A row can't change its visit_type (or Java class) in place, so switching
        // regular <-> study deletes the old row and saves a new one with a new id.
        if (existing.isStudySpot() != request.study()) {
            Cafe replacement = toEntity(userId, request);
            applyAddress(replacement, request, existing);
            repository.delete(existing);
            return VisitResponse.from(repository.save(replacement));
        }

        // Same type: just change the fields. The transaction saves them on commit (dirty checking).
        applyBaseFields(existing, request);
        applyAddress(existing, request, existing);
        if (existing instanceof StudyCafe study) {
            applyStudyFields(study, request);
        }
        return VisitResponse.from(existing);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(find(userId, id));
    }

    // owner check lives in the query, so someone else's visit looks exactly like a missing one
    private Cafe find(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> new VisitNotFoundException(id));
    }

    private Cafe toEntity(Long userId, VisitRequest r) {
        Cafe cafe = newCafe(r);
        // a reference (just the id), so no extra SELECT for the user
        cafe.setUser(users.getReferenceById(userId));
        cafe.setPhotoUrl(blankToNull(r.photoUrl()));
        cafe.setShared(r.isShareable());
        cafe.setExtras(toExtras(r));
        return cafe;
    }

    private static List<Extra> toExtras(VisitRequest r) {
        return r.extrasOrEmpty().stream().map(e -> new Extra(e.name().trim(), e.price())).toList();
    }

    private Cafe newCafe(VisitRequest r) {
        if (r.study()) {
            return new StudyCafe(r.name().trim(), r.neighbourhood().trim(), r.visitDate(), r.order().trim(),
                    r.price(), r.rating(), Boolean.TRUE.equals(r.hasWifi()), Boolean.TRUE.equals(r.hasOutlets()),
                    r.seating(), r.laptopPolicy(), r.noiseLevel());
        }
        return new Cafe(r.name().trim(), r.neighbourhood().trim(), r.visitDate(), r.order().trim(),
                r.price(), r.rating());
    }

    private void applyBaseFields(Cafe cafe, VisitRequest r) {
        cafe.setName(r.name().trim());
        cafe.setNeighbourhood(r.neighbourhood().trim());
        cafe.setVisitDate(r.visitDate());
        cafe.setOrder(r.order().trim());
        cafe.setPrice(r.price());
        cafe.setRating(r.rating());
        cafe.setPhotoUrl(blankToNull(r.photoUrl()));
        cafe.setShared(r.isShareable());
        cafe.setExtras(toExtras(r));
    }

    // Coordinates picked in the app win. Otherwise only a new or changed address is geocoded;
    // an unchanged one keeps the coordinates it already has.
    private void applyAddress(Cafe target, VisitRequest r, Cafe before) {
        String address = trimToNull(r.address());
        String city = trimToNull(r.city());
        if (address == null) {
            target.setLocation(null, city, null, null);
            return;
        }
        if (r.latitude() != null) {
            target.setLocation(address, city, r.latitude(), r.longitude());
            return;
        }
        if (before != null && Objects.equals(address, before.getAddress()) && Objects.equals(city, before.getCity())) {
            target.setLocation(address, city, before.getLatitude(), before.getLongitude());
            return;
        }
        Optional<Coordinates> found = geocoder.locate(city == null ? address : address + ", " + city);
        target.setLocation(address, city,
                found.map(Coordinates::latitude).orElse(null),
                found.map(Coordinates::longitude).orElse(null));
    }

    private static String trimToNull(String s) {
        return s == null ? null : blankToNull(s.trim());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private void applyStudyFields(StudyCafe study, VisitRequest r) {
        study.setHasWifi(Boolean.TRUE.equals(r.hasWifi()));
        study.setHasOutlets(Boolean.TRUE.equals(r.hasOutlets()));
        study.setSeating(r.seating());
        study.setLaptopPolicy(r.laptopPolicy());
        study.setNoiseLevel(r.noiseLevel());
    }
}
