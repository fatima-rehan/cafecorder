package com.cafecorder.controller;

import com.cafecorder.config.SecurityConfig;
import com.cafecorder.dto.AuthDtos.AuthResponse;
import com.cafecorder.dto.AuthDtos.UserResponse;
import com.cafecorder.exception.EmailTakenException;
import com.cafecorder.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sign up and log in work without a token; /me doesn't
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=test-secret-that-is-definitely-32-bytes-long")
class PublicEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService auth;

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
