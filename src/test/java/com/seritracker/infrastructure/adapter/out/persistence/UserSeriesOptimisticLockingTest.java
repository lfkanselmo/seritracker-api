package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserSeriesEntity optimistic locking")
class UserSeriesOptimisticLockingTest {

    @Autowired
    private JpaUserSeriesRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("should throw OptimisticLockingFailureException when two concurrent readers update the same row")
    void shouldThrowOptimisticLockingFailureException_whenUpdatingStaleVersion() {
        UserSeriesEntity saved = repository.saveAndFlush(UserSeriesEntity.builder()
                .userId(1L)
                .tmdbId(1396)
                .title("Breaking Bad")
                .status("WATCHING")
                .watchedEpisodes(0)
                .totalEpisodes(62)
                .build());
        entityManager.detach(saved);

        UserSeriesEntity firstReader = repository.findById(saved.getId()).orElseThrow();
        entityManager.detach(firstReader);
        UserSeriesEntity secondReader = repository.findById(saved.getId()).orElseThrow();
        entityManager.detach(secondReader);

        firstReader.setWatchedEpisodes(1);
        repository.saveAndFlush(firstReader);

        secondReader.setWatchedEpisodes(2);
        assertThatThrownBy(() -> repository.saveAndFlush(secondReader))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
