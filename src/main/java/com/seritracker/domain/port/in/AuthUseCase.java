package com.seritracker.domain.port.in;

import com.seritracker.domain.model.AuthResult;

public interface AuthUseCase {
    AuthResult register(String name, String email, String password);
    AuthResult login(String email, String password);
    AuthResult refresh(String refreshToken);
    void logout(String refreshToken);
    void changePassword(Long userId, String currentPassword, String newPassword);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}
