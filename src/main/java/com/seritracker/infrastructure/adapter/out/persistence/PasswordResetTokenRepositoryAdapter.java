package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.PasswordResetToken;
import com.seritracker.domain.port.out.PasswordResetTokenRepository;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.PasswordResetTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final JpaPasswordResetTokenRepository jpaRepository;
    private final PasswordResetTokenMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(token))
        );
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
