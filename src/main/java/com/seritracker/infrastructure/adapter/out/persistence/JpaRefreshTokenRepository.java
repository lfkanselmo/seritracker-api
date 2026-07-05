package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    void deleteByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}
