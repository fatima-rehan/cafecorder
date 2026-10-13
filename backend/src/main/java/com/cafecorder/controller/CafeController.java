package com.cafecorder.controller;

import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitRequest;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.dto.VisitStats;
import com.cafecorder.service.CafeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// Thin on purpose: read the request, call the service, shape the response
@RestController
@RequestMapping("/api/visits")
public class CafeController {

    private final CafeService service;

    public CafeController(CafeService service) {
        this.service = service;
    }

    // GET /api/visits?neighbourhood=Leslieville&sort=RATING
    @GetMapping
    public List<VisitResponse> list(@RequestParam(required = false) String neighbourhood,
                                    @RequestParam(defaultValue = "RECENT") SortOption sort) {
        return service.list(neighbourhood, sort);
    }

    @GetMapping("/{id}")
    public VisitResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/stats")
    public VisitStats stats() {
        return service.stats();
    }

    @GetMapping("/neighbourhoods")
    public List<String> neighbourhoods() {
        return service.neighbourhoods();
    }

    // 201 Created plus a Location header pointing at the new visit
    @PostMapping
    public ResponseEntity<VisitResponse> create(@Valid @RequestBody VisitRequest request) {
        VisitResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public VisitResponse update(@PathVariable Long id, @Valid @RequestBody VisitRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
