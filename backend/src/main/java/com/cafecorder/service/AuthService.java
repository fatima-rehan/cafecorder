package com.cafecorder.service;

import com.cafecorder.dto.AuthDtos.AuthResponse;
import com.cafecorder.dto.AuthDtos.LoginRequest;
import com.cafecorder.dto.AuthDtos.RegisterRequest;
import com.cafecorder.dto.AuthDtos.UserResponse;
import com.cafecorder.exception.EmailTakenException;
import com.cafecorder.model.AppUser;
import com.cafecorder.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private static final Duration TOKEN_LIFETIME = Duration.ofDays(7);
    private static final String BAD_LOGIN = "Email or password is incorrect";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    // checked when the email doesn't exist, so an unknown email takes as long as a wrong password
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailTakenException();
        }
        AppUser user = users.save(new AppUser(email, request.displayName().trim(),
                passwordEncoder.encode(request.password())));
        return new AuthResponse(tokenFor(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = users.findByEmail(normalize(request.email())).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);
        // same message either way, so nobody can use login to find out which emails have accounts
        if (user == null || !passwordOk) {
            throw new BadCredentialsException(BAD_LOGIN);
        }
        return new AuthResponse(tokenFor(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
    }

    // subject = user id, so every later request knows who's asking without a database lookup
    private String tokenFor(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cafecorder")
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .subject(user.getId().toString())
                .claim("name", user.getDisplayName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
