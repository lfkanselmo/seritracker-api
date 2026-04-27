package com.seritracker.infrastructure.security;

import com.seritracker.infrastructure.adapter.out.persistence.JpaUserRepository;
import com.seritracker.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = jpaUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
    }
}