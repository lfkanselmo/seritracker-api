package com.seritracker.infrastructure.adapter.out.persistence;

import com.seritracker.domain.model.User;
import com.seritracker.domain.port.out.UserRepository;
import com.seritracker.infrastructure.adapter.out.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(user))
        );
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
