package com.cafecorder.service;

import com.cafecorder.dto.AuthDtos.AuthResponse;
import com.cafecorder.dto.AuthDtos.LoginRequest;
import com.cafecorder.dto.AuthDtos.RegisterRequest;
import com.cafecorder.exception.EmailTakenException;
import com.cafecorder.model.AppUser;
import com.cafecorder.repository.UserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Real BCrypt and real JWT signing; only the database is mocked
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final SecretKey key = new SecretKeySpec(
            "test-secret-that-is-definitely-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    // cost 4 instead of the default 10 keeps the tests fast
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository users;

    private AuthService auth;

    @BeforeEach
    void setUp() {
        auth = new AuthService(users, encoder, new NimbusJwtEncoder(new ImmutableSecret<>(key)));
    }

    private AppUser savedUser(long id, String email, String rawPassword) {
        AppUser user = new AppUser(email, "Fatima", encoder.encode(rawPassword));
        // the database normally sets the id
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void registerHashesThePasswordAndReturnsAValidToken() {
        when(users.existsByEmail("fatima@example.com")).thenReturn(false);
        when(users.save(any())).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 42L);
            return u;
        });

        AuthResponse result = auth.register(new RegisterRequest("  Fatima@Example.com ", "coffee123", "Fatima"));

        verify(users).save(argThat(u -> u.getEmail().equals("fatima@example.com")
                && !u.getPasswordHash().equals("coffee123")
                && encoder.matches("coffee123", u.getPasswordHash())));

        Jwt token = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
                .decode(result.token());
        assertThat(token.getSubject()).isEqualTo("42");
        assertThat(token.getClaimAsString("name")).isEqualTo("Fatima");
        assertThat(token.getExpiresAt()).isAfter(token.getIssuedAt());
    }

    @Test
    void registerRejectsATakenEmail() {
        when(users.existsByEmail("fatima@example.com")).thenReturn(true);

        assertThatThrownBy(() -> auth.register(new RegisterRequest("fatima@example.com", "coffee123", "Fatima")))
                .isInstanceOf(EmailTakenException.class);
        verify(users, never()).save(any());
    }

    @Test
    void loginWorksWithTheRightPasswordAndAnyEmailCase() {
        when(users.findByEmail("fatima@example.com"))
                .thenReturn(Optional.of(savedUser(7L, "fatima@example.com", "coffee123")));

        AuthResponse result = auth.login(new LoginRequest("FATIMA@example.com", "coffee123"));

        assertThat(result.user().id()).isEqualTo(7L);
        assertThat(result.token()).isNotBlank();
    }

    @Test
    void loginFailsTheSameWayForWrongPasswordAndUnknownEmail() {
        when(users.findByEmail("fatima@example.com"))
                .thenReturn(Optional.of(savedUser(7L, "fatima@example.com", "coffee123")));
        when(users.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auth.login(new LoginRequest("fatima@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email or password is incorrect");
        assertThatThrownBy(() -> auth.login(new LoginRequest("nobody@example.com", "coffee123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email or password is incorrect");
    }
}
