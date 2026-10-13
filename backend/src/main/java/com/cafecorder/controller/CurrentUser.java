package com.cafecorder.controller;

import org.springframework.security.oauth2.jwt.Jwt;

// The token's subject is the user's id (set in AuthService.tokenFor)
final class CurrentUser {

    private CurrentUser() {
    }

    static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
