package com.cafecorder.dto;

import com.cafecorder.model.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Small records for the auth endpoints, grouped since they're only used together
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            // BCrypt ignores everything past 72 bytes, so cap it there
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 60) String displayName
    ) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserResponse(Long id, String email, String displayName) {
        public static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
        }
    }

    public record AuthResponse(String token, UserResponse user) {
    }
}
