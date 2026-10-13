package com.cafecorder.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // stored lowercase so "Fatima@x.com" and "fatima@x.com" are one account
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    // BCrypt hash, never the password itself
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
        // JPA needs a no-arg constructor
    }

    public AppUser(String email, String displayName, String passwordHash) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
