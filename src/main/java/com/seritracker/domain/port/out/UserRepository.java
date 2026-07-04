package com.seritracker.domain.port.out;

import com.seritracker.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
