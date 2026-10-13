package com.cafecorder.controller;

import com.cafecorder.dto.AuthDtos.AuthResponse;
import com.cafecorder.dto.AuthDtos.LoginRequest;
import com.cafecorder.dto.AuthDtos.RegisterRequest;
import com.cafecorder.dto.AuthDtos.UserResponse;
import com.cafecorder.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    // lets the frontend check that a saved token still works, and get the user's name
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(CurrentUser.id(jwt));
    }
}
