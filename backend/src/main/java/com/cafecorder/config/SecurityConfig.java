package com.cafecorder.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/*
 * Stateless JWT security instead of server sessions: the token itself proves who you are,
 * so the server stores nothing per user and any instance can handle any request.
 * Register/login hand out the token; the React app sends it as "Authorization: Bearer <token>".
 */
@Configuration
public class SecurityConfig {

    private final SecretKey key;
    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 needs a key of at least 256 bits; fail at startup rather than sign with a weak key
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.allowedOrigins = allowedOrigins;
    }

    private static final RequestMatcher PUBLIC_FEED =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/feed/**");

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // The feed is public, so don't even read a token there: an expired one left in someone's
        // browser would otherwise turn browsing Explore into a 401.
        DefaultBearerTokenResolver bearer = new DefaultBearerTokenResolver();
        BearerTokenResolver skipOnPublicFeed = request -> PUBLIC_FEED.matches(request) ? null : bearer.resolve(request);

        return http
                // CSRF attacks ride on cookies the browser sends automatically. We use no cookies, only a header.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(PUBLIC_FEED).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                // checks "Authorization: Bearer ..." on every request; missing or bad token = 401
                .oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(skipOnPublicFeed).jwt(Customizer.withDefaults()))
                .build();
    }

    // the React app runs on a different origin (localhost:5173 or Vercel), so the browser needs permission
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Location"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    // BCrypt is slow on purpose and salts each hash, so a leaked table is hard to crack
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    // also rejects expired tokens
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
