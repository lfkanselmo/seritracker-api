package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.RefreshToken;
import com.seritracker.domain.port.out.RefreshTokenRepository;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    @Override
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(token))
        );
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        jpaRepository.deleteByTokenHash(tokenHash);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
