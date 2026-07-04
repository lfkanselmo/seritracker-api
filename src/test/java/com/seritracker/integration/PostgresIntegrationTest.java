package com.seritracker.integration;

import com.seritracker.infrastructure.adapter.out.persistence.JpaNotificationRepository;
import com.seritracker.infrastructure.adapter.out.persistence.JpaUserRepository;
import com.seritracker.infrastructure.adapter.out.persistence.JpaUserSeriesRepository;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserSeriesEntity;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A diferencia del resto de la suite (que usa H2 en modo PostgreSQL via el perfil "test"),
 * esta clase corre contra un Postgres real embebido (sin Docker), aplicando las migraciones
 * de Flyway tal cual se ejecutarian en produccion. Cubre comportamiento especifico de Postgres
 * que H2 no puede validar de forma confiable (constraints, optimistic locking real).
 */
@SpringBootTest
@ActiveProfiles("it")
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY)
@DisplayName("Postgres integration (Flyway + JPA contra Postgres real)")
class PostgresIntegrationTest {

    @Autowired private JpaUserRepository userRepository;
    @Autowired private JpaUserSeriesRepository userSeriesRepository;
    @Autowired private JpaNotificationRepository notificationRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("las migraciones de Flyway aplican limpio contra Postgres real")
    void flywayMigrationsApplyCleanly() {
        assertThat(userRepository).isNotNull();
    }

    @Test
    @DisplayName("el constraint unico (user_id, tmdb_id) lo hace cumplir Postgres")
    void uniqueConstraintOnUserIdAndTmdbId_isEnforced() {
        UserEntity user = userRepository.save(buildUser("it-unique@test.com"));

        userSeriesRepository.saveAndFlush(buildSeries(user.getId(), 1396));

        assertThatThrownBy(() ->
                userSeriesRepository.saveAndFlush(buildSeries(user.getId(), 1396))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("el bloqueo optimista lo hace cumplir Postgres real, no solo H2")
    void optimisticLocking_isEnforcedAgainstRealPostgres() {
        UserEntity user = userRepository.save(buildUser("it-lock@test.com"));

        UserSeriesEntity saved = userSeriesRepository.saveAndFlush(buildSeries(user.getId(), 1397));
        entityManager.detach(saved);

        UserSeriesEntity firstReader = userSeriesRepository.findById(saved.getId()).orElseThrow();
        entityManager.detach(firstReader);
        UserSeriesEntity secondReader = userSeriesRepository.findById(saved.getId()).orElseThrow();
        entityManager.detach(secondReader);

        firstReader.setWatchedEpisodes(1);
        userSeriesRepository.saveAndFlush(firstReader);

        secondReader.setWatchedEpisodes(2);
        assertThatThrownBy(() -> userSeriesRepository.saveAndFlush(secondReader))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("la query nativa findAllDistinctUserIds funciona contra Postgres real")
    void findAllDistinctUserIds_returnsDistinctIds() {
        UserEntity user = userRepository.save(buildUser("it-distinct@test.com"));

        userSeriesRepository.saveAndFlush(buildSeries(user.getId(), 2001));
        userSeriesRepository.saveAndFlush(buildSeries(user.getId(), 2002));

        List<Long> ids = notificationRepository.findAllDistinctUserIds();

        assertThat(ids).contains(user.getId());
    }

    private UserEntity buildUser(String email) {
        return UserEntity.builder()
                .email(email)
                .passwordHash("hash")
                .name("IT Test")
                .role("USER")
                .build();
    }

    private UserSeriesEntity buildSeries(Long userId, Integer tmdbId) {
        return UserSeriesEntity.builder()
                .userId(userId)
                .tmdbId(tmdbId)
                .title("Test Series")
                .status("WATCHING")
                .watchedEpisodes(0)
                .totalEpisodes(10)
                .build();
    }
}
