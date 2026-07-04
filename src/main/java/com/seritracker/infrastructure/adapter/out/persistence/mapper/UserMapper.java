package com.seritracker.infrastructure.adapter.out.persistence.mapper;

import com.seritracker.domain.model.User;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .name(entity.getName())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public UserEntity toEntity(User domain) {
        return UserEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .name(domain.getName())
                .passwordHash(domain.getPasswordHash())
                .role(domain.getRole())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
