package com.seritracker.infrastructure.adapter.out.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserSeriesEntity")
class UserSeriesEntityTest {

    @Test
    @DisplayName("onCreate should set createdAt and updatedAt")
    void onCreate_shouldSetCreatedAtAndUpdatedAt() {
        UserSeriesEntity entity = new UserSeriesEntity();
        entity.onCreate();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onUpdate should refresh updatedAt")
    void onUpdate_shouldRefreshUpdatedAt() throws InterruptedException {
        UserSeriesEntity entity = new UserSeriesEntity();
        entity.onCreate();
        var createdAt = entity.getCreatedAt();

        Thread.sleep(10);
        entity.onUpdate();

        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }
}