package com.finance.finance_tracker.service.Impl;

import com.finance.finance_tracker.entity.User;
import com.finance.finance_tracker.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("=== DEBUG: Loading user by email: " + email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            System.out.println("=== DEBUG: User not found in database");
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        User user = userOpt.get();
        System.out.println("=== DEBUG: User found:");
        System.out.println("  ID: " + user.getId());
        System.out.println("  Email: " + user.getEmail());
        System.out.println("  Name: " + user.getName() + " " + user.getSurname());
        System.out.println("  Active: " + user.isActive());
        System.out.println("  Password hash: " + user.getPassword().substring(0, 50) + "...");

        // Проверьте пароль
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String testPassword = "password123";
        boolean passwordMatches = encoder.matches(testPassword, user.getPassword());
        System.out.println("=== DEBUG: Password 'password123' matches: " + passwordMatches);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActive())  // <-- Вот здесь проверяется active
                .build();
    }
}