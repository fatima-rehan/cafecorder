package com.cafecorder.controller;

import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.exception.VisitNotFoundException;
import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;
import com.cafecorder.service.CafeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Loads only the web layer (controller, JSON, validation, error handler). The service is a mock, so no database.
@WebMvcTest(CafeController.class)
class CafeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CafeService service;

    private final VisitResponse pilot = new VisitResponse(1L, "Pilot Coffee", "Leslieville",
            LocalDate.of(2026, 9, 1), "Oat flat white", new BigDecimal("5.75"), 5, true,
            true, true, Seating.GOOD, "No restrictions", NoiseLevel.MODERATE);

    @Test
    void listPassesFilterAndSortToTheService() throws Exception {
        when(service.list("Leslieville", SortOption.RATING)).thenReturn(List.of(pilot));

        mvc.perform(get("/api/visits").param("neighbourhood", "Leslieville").param("sort", "RATING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pilot Coffee"))
                .andExpect(jsonPath("$[0].visitDate").value("2026-09-01"))
                .andExpect(jsonPath("$[0].price").value(5.75))
                .andExpect(jsonPath("$[0].studySpot").value(true));
    }

    @Test
    void unknownSortIs400() throws Exception {
        mvc.perform(get("/api/visits").param("sort", "CHEAPEST"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(pilot);

        mvc.perform(post("/api/visits").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Pilot Coffee","neighbourhood":"Leslieville","visitDate":"2026-09-01",
                         "order":"Oat flat white","price":5.75,"rating":5,"studySession":true,
                         "hasWifi":true,"hasOutlets":true,"seating":"GOOD","noiseLevel":"MODERATE"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/visits/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRejectsBadFieldsWithoutCallingTheService() throws Exception {
        mvc.perform(post("/api/visits").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"","neighbourhood":"Annex","visitDate":"2026-09-01",
                         "order":"Latte","price":-2,"rating":7}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.rating").exists());

        verifyNoInteractions(service);
    }

    @Test
    void studySessionWithoutSeatingOrNoiseIsRejected() throws Exception {
        mvc.perform(post("/api/visits").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Balzac's","neighbourhood":"Distillery","visitDate":"2026-09-01",
                         "order":"Americano","price":4.10,"rating":4,"studySession":true}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studyDetailsComplete").value("study sessions need seating and noise level"));

        verifyNoInteractions(service);
    }

    @Test
    void missingVisitIs404ProblemDetail() throws Exception {
        when(service.get(99L)).thenThrow(new VisitNotFoundException(99L));

        mvc.perform(get("/api/visits/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No visit found with id 99"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/visits/1")).andExpect(status().isNoContent());
        verify(service).delete(1L);
    }
}
