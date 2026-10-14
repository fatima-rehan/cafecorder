package com.cafecorder.controller;

import com.cafecorder.config.SecurityConfig;
import com.cafecorder.dto.AuthDtos.AuthResponse;
import com.cafecorder.dto.AuthDtos.UserResponse;
import com.cafecorder.dto.FeedDtos.FeedItem;
import com.cafecorder.dto.FeedDtos.MapCafe;
import com.cafecorder.dto.FeedDtos.TopCafe;
import com.cafecorder.exception.EmailTakenException;
import com.cafecorder.service.AuthService;
import com.cafecorder.service.FeedService;
import com.cafecorder.service.FeedService.FeedSort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sign up, log in and the public feed work without a token; /me doesn't
@WebMvcTest({AuthController.class, FeedController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=test-secret-that-is-definitely-32-bytes-long")
class PublicEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService auth;

    @MockitoBean
    private FeedService feed;

    @Test
    void feedPhotosAreOpenToEveryoneAndShowPriceButNoEmail() throws Exception {
        when(feed.photos(FeedSort.TOP, 0)).thenReturn(List.of(new FeedItem(3L, "18Feet", "Queen West",
                LocalDate.of(2026, 9, 26), "Dalgona matcha", new BigDecimal("6.50"), List.of(),
                new BigDecimal("6.50"), 5, null, true, "Fatima", null, null)));

        mvc.perform(get("/api/feed/photos").param("sort", "TOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].author").value("Fatima"))
                .andExpect(jsonPath("$[0].price").value(6.50))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void guestsCanPageThroughOlderPosts() throws Exception {
        when(feed.photos(FeedSort.RECENT, 1)).thenReturn(List.of());

        mvc.perform(get("/api/feed/photos").param("page", "1"))
                .andExpect(status().isOk());

        verify(feed).photos(FeedSort.RECENT, 1);
    }

    @Test
    void topCafesAreOpenToEveryone() throws Exception {
        when(feed.topCafes()).thenReturn(List.of(new TopCafe("18Feet", "Queen West", 4.8, 5, null)));

        mvc.perform(get("/api/feed/top-cafes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].averageRating").value(4.8))
                .andExpect(jsonPath("$[0].visits").value(5));
    }

    @Test
    void anExpiredTokenLeftInTheBrowserDoesNotBreakTheFeed() throws Exception {
        when(feed.mapCafes()).thenReturn(List.of());

        mvc.perform(get("/api/feed/map").header("Authorization", "Bearer expired.or.garbage"))
                .andExpect(status().isOk());
        // but it's still rejected anywhere that needs a login
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer expired.or.garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mapPinsAreOpenToEveryone() throws Exception {
        when(feed.mapCafes()).thenReturn(List.of(
                new MapCafe("18Feet", "1 Queen St W, Toronto", "Toronto", 43.65, -79.38, 3, 4.7, null)));

        mvc.perform(get("/api/feed/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitude").value(43.65))
                .andExpect(jsonPath("$[0].longitude").value(-79.38))
                .andExpect(jsonPath("$[0].address").value("1 Queen St W, Toronto"));
    }

    @Test
    void registerReturns201WithAToken() throws Exception {
        when(auth.register(any())).thenReturn(new AuthResponse("token-123", new UserResponse(1L, "f@x.com", "Fatima")));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"f@x.com","password":"coffee123","displayName":"Fatima"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-123"))
                .andExpect(jsonPath("$.user.displayName").value("Fatima"));
    }

    @Test
    void registerRejectsShortPasswordAndBadEmail() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"not-an-email","password":"short","displayName":"Fatima"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(auth);
    }

    @Test
    void duplicateEmailIs409() throws Exception {
        when(auth.register(any())).thenThrow(new EmailTakenException());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"f@x.com","password":"coffee123","displayName":"Fatima"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        when(auth.login(any())).thenThrow(new BadCredentialsException("Email or password is incorrect"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"f@x.com","password":"wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email or password is incorrect"));
    }

    @Test
    void meNeedsAToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    // the browser asks first (preflight) before sending a cross-origin request with a token
    @Test
    void reactDevServerPassesCors() throws Exception {
        mvc.perform(options("/api/visits")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mvc.perform(options("/api/visits")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
