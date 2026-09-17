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
import com.foodiehub.auth_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserProfileResponse register(RegisterRequest request, Role role) {

        if (userDao.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "Email already registered"
            );
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(role)
                .build();

        User savedUser = userDao.save(user);

        return toProfileResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        String email = authentication.getName();

        User user = userDao.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        String token = jwtUtil.generateToken(email, 15);

        return new LoginResponse(
                token,
                "Bearer",
                15 * 60,
                user.getId(),
                user.getRole()
        );
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