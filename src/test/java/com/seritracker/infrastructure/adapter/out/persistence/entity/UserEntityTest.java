package com.seritracker.infrastructure.adapter.out.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserEntity")
class UserEntityTest {

    @Test
    @DisplayName("onCreate should set createdAt")
    void onCreate_shouldSetCreatedAt() {
        UserEntity entity = new UserEntity();
        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
    }
}