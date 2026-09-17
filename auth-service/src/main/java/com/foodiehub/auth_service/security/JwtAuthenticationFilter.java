package com.foodiehub.auth_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodiehub.auth_service.dto.LoginRequest;
import com.foodiehub.auth_service.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@RequiredArgsConstructor
public class JwtAuthenticationFilter  extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!request.getServletPath().equals("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        LoginRequest loginRequest =
                objectMapper.readValue(
                        request.getInputStream(),
                        LoginRequest.class
                );

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                );

        Authentication authResult =
                authenticationManager.authenticate(authToken);

        SecurityContextHolder.getContext()
                .setAuthentication(authResult);

        String token =
                jwtUtil.generateToken(authResult.getName(), 15);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");

        response.setHeader(
                "Authorization",
                "Bearer " + token
        );

        response.getWriter().write(
                objectMapper.writeValueAsString(
                        java.util.Map.of(
                                "accessToken", token,
                                "tokenType", "Bearer"
                        )
                )
        );

        // VERY IMPORTANT:
        // Do NOT call filterChain.doFilter() for login
        return;
    }
}
