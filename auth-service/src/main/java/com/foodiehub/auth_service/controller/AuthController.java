package com.foodiehub.auth_service.controller;

import com.foodiehub.auth_service.dto.LoginRequest;
import com.foodiehub.auth_service.dto.LoginResponse;
import com.foodiehub.auth_service.dto.RegisterRequest;
import com.foodiehub.auth_service.dto.UserProfileResponse;
import com.foodiehub.auth_service.model.Role;
import com.foodiehub.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("This is register User Controller");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request, Role.CUSTOMER));
    }

    @PostMapping("/register/owner")
    public ResponseEntity<UserProfileResponse> registerOwner(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request, Role.OWNER));
    }

    @PostMapping("/register/agent")
    public ResponseEntity<UserProfileResponse> registerAgent(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request, Role.AGENT));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/user")
    public ResponseEntity<String> me() {
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        return new ResponseEntity<>(authentication.getName(),HttpStatus.OK);
    }
}
