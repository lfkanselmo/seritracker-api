package com.seritracker.domain.port.out;

import com.seritracker.domain.model.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteByUserId(Long userId);
    void deleteById(Long id);
}
