package com.cafecorder.controller;

import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitRequest;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.dto.VisitStats;
import com.cafecorder.service.CafeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// The signed-in user's own logbook. Thin on purpose: read the request, call the service, shape the response.
@RestController
@RequestMapping("/api/visits")
public class CafeController {

    private final CafeService service;

    public CafeController(CafeService service) {
        this.service = service;
    }

    // GET /api/visits?neighbourhood=Leslieville&sort=RATING
    @GetMapping
    public List<VisitResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(required = false) String neighbourhood,
                                    @RequestParam(defaultValue = "RECENT") SortOption sort) {
        return service.list(CurrentUser.id(jwt), neighbourhood, sort);
    }

    @GetMapping("/{id}")
    public VisitResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @GetMapping("/stats")
    public VisitStats stats(@AuthenticationPrincipal Jwt jwt) {
        return service.stats(CurrentUser.id(jwt));
    }

    @GetMapping("/neighbourhoods")
    public List<String> neighbourhoods(@AuthenticationPrincipal Jwt jwt) {
        return service.neighbourhoods(CurrentUser.id(jwt));
    }

    // 201 Created plus a Location header pointing at the new visit
    @PostMapping
    public ResponseEntity<VisitResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody VisitRequest request) {
        VisitResponse created = service.create(CurrentUser.id(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public VisitResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                @Valid @RequestBody VisitRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
