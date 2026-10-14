package com.cafecorder.controller;

import com.cafecorder.config.SecurityConfig;
import com.cafecorder.dto.ExtraItem;
import com.cafecorder.dto.SortOption;
import com.cafecorder.dto.VisitResponse;
import com.cafecorder.exception.VisitNotFoundException;
import com.cafecorder.model.NoiseLevel;
import com.cafecorder.model.Seating;
import com.cafecorder.service.CafeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Web layer plus my real security rules. The service is a mock, so no database.
@WebMvcTest(CafeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=test-secret-that-is-definitely-32-bytes-long")
class CafeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CafeService service;

    // pretend to be signed in as user 1 (no real token needed)
    private static RequestPostProcessor user1() {
        return jwt().jwt(j -> j.subject("1"));
    }

    private final VisitResponse pilot = new VisitResponse(1L, "Pilot Coffee", "Leslieville",
            LocalDate.of(2026, 9, 1), "Oat flat white", new BigDecimal("5.75"), 5, true,
            true, true, Seating.GOOD, "No restrictions", NoiseLevel.MODERATE, null, true,
            List.of(new ExtraItem("Almond croissant", new BigDecimal("4.25"))), new BigDecimal("10.00"));

    @Test
    void visitsNeedAToken() throws Exception {
        mvc.perform(get("/api/visits")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void listPassesUserFilterAndSortToTheService() throws Exception {
        when(service.list(1L, "Leslieville", SortOption.RATING)).thenReturn(List.of(pilot));

        mvc.perform(get("/api/visits").with(user1()).param("neighbourhood", "Leslieville").param("sort", "RATING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pilot Coffee"))
                .andExpect(jsonPath("$[0].visitDate").value("2026-09-01"))
                .andExpect(jsonPath("$[0].price").value(5.75))
                .andExpect(jsonPath("$[0].extras[0].name").value("Almond croissant"))
                .andExpect(jsonPath("$[0].total").value(10.00))
                .andExpect(jsonPath("$[0].studySpot").value(true));
    }

    @Test
    void unknownSortIs400() throws Exception {
        mvc.perform(get("/api/visits").with(user1()).param("sort", "CHEAPEST"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(pilot);

        mvc.perform(post("/api/visits").with(user1()).contentType(MediaType.APPLICATION_JSON).content("""
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
        mvc.perform(post("/api/visits").with(user1()).contentType(MediaType.APPLICATION_JSON).content("""
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
        mvc.perform(post("/api/visits").with(user1()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Balzac's","neighbourhood":"Distillery","visitDate":"2026-09-01",
                         "order":"Americano","price":4.10,"rating":4,"studySession":true}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studyDetailsComplete").value("study sessions need seating and noise level"));

        verifyNoInteractions(service);
    }

    @Test
    void eachExtraItemIsValidated() throws Exception {
        mvc.perform(post("/api/visits").with(user1()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Sam James","neighbourhood":"Harbord","visitDate":"2026-09-01",
                         "order":"Cortado","price":4.50,"rating":4,
                         "extras":[{"name":"Croissant","price":3.50},{"name":"","price":-1}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['extras[1].name']").exists())
                .andExpect(jsonPath("$.errors['extras[1].price']").exists())
                .andExpect(jsonPath("$.errors['extras[0].name']").doesNotExist());

        verifyNoInteractions(service);
    }

    @Test
    void createRejectsAPhotoThatIsNotAnImage() throws Exception {
        mvc.perform(post("/api/visits").with(user1()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Sam James","neighbourhood":"Harbord","visitDate":"2026-09-01",
                         "order":"Cortado","price":4.50,"rating":4,"photoUrl":"javascript:alert(1)"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.photoUrl").exists());

        verifyNoInteractions(service);
    }

    @Test
    void someoneElsesVisitLooksMissing() throws Exception {
        when(service.get(1L, 99L)).thenThrow(new VisitNotFoundException(99L));

        mvc.perform(get("/api/visits/99").with(user1()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No visit found with id 99"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/visits/1").with(user1())).andExpect(status().isNoContent());
        verify(service).delete(1L, 1L);
    }
}
