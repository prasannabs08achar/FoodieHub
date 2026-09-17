package com.foodiehub.auth_service.service;

import com.foodiehub.auth_service.dao.UserDao;
import com.foodiehub.auth_service.dto.LoginRequest;
import com.foodiehub.auth_service.dto.LoginResponse;
import com.foodiehub.auth_service.dto.RegisterRequest;
import com.foodiehub.auth_service.dto.UserProfileResponse;
import com.foodiehub.auth_service.exception.InvalidCredentialsException;
import com.foodiehub.auth_service.exception.UserAlreadyExistsException;
import com.foodiehub.auth_service.model.Role;
import com.foodiehub.auth_service.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserProfileResponse register(RegisterRequest request, Role role) {
        if (userDao.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(role)
                .build();

        User saved = userDao.save(user);
        return toProfileResponse(saved);
    }
    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }


}
