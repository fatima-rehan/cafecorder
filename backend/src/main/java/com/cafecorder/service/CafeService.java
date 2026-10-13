package com.cafecorder.service;

import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitRequest;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.dto.VisitStats;
import com.cafecorder.exception.VisitNotFoundException;
import com.cafecorder.model.Cafe;
import com.cafecorder.model.StudyCafe;
import com.cafecorder.repository.CafeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

// What CafeTracker used to do, with Postgres instead of an ArrayList
@Service
@Transactional(readOnly = true)
public class CafeService {

    private final CafeRepository repository;

    // constructor injection: Spring passes in the repository, and tests can pass in a mock
    public CafeService(CafeRepository repository) {
        this.repository = repository;
    }

    public List<VisitResponse> list(String neighbourhood, SortOption sort) {
        List<Cafe> cafes = (neighbourhood == null || neighbourhood.isBlank())
                ? repository.findAll(sort.toSort())
                : repository.findByNeighbourhoodIgnoreCase(neighbourhood.trim(), sort.toSort());
        return cafes.stream().map(VisitResponse::from).toList();
    }

    public VisitResponse get(Long id) {
        return VisitResponse.from(find(id));
    }

    public List<String> neighbourhoods() {
        return repository.findNeighbourhoods();
    }

    public VisitStats stats() {
        BigDecimal total = Objects.requireNonNullElse(repository.totalSpent(), BigDecimal.ZERO);
        return new VisitStats(repository.count(), repository.countUniqueCafes(), total, repository.averageRating());
    }

    @Transactional
    public VisitResponse create(VisitRequest request) {
        return VisitResponse.from(repository.save(newCafe(request)));
    }

    @Transactional
    public VisitResponse update(Long id, VisitRequest request) {
        Cafe existing = find(id);

        // A row can't change its visit_type (or Java class) in place, so switching
        // regular <-> study deletes the old row and saves a new one with a new id.
        if (existing.isStudySpot() != request.study()) {
            repository.delete(existing);
            return VisitResponse.from(repository.save(newCafe(request)));
        }

        // Same type: just change the fields. The transaction saves them on commit (dirty checking).
        applyBaseFields(existing, request);
        if (existing instanceof StudyCafe study) {
            applyStudyFields(study, request);
        }
        return VisitResponse.from(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Cafe find(Long id) {
        return repository.findById(id).orElseThrow(() -> new VisitNotFoundException(id));
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
    }

    private void applyStudyFields(StudyCafe study, VisitRequest r) {
        study.setHasWifi(Boolean.TRUE.equals(r.hasWifi()));
        study.setHasOutlets(Boolean.TRUE.equals(r.hasOutlets()));
        study.setSeating(r.seating());
        study.setLaptopPolicy(r.laptopPolicy());
        study.setNoiseLevel(r.noiseLevel());
    }
}
