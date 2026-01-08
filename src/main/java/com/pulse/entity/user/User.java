package com.pulse.entity.user;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider", columnNames = {"provider_type", "provider_id"})
    },
    indexes = {
        @Index(name = "idx_provider", columnList = "provider_type, provider_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected User() {}

    private User(String nickname, ProviderType providerType, String providerId, UserRole role) {
        this.nickname = nickname;
        this.providerType = providerType;
        this.providerId = providerId;
        this.role = role;
    }

    public static User of(String nickname, ProviderType providerType, String providerId) {
        return new User(nickname, providerType, providerId, UserRole.USER);
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public String getProviderId() {
        return providerId;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
