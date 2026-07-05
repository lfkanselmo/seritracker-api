package com.seritracker.infrastructure.adapter.out.persistence.mapper;

import com.seritracker.domain.model.PasswordResetToken;
import com.seritracker.infrastructure.adapter.out.persistence.entity.PasswordResetTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokenMapper {

    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return PasswordResetToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PasswordResetTokenEntity toEntity(PasswordResetToken domain) {
        return PasswordResetTokenEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .tokenHash(domain.getTokenHash())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
