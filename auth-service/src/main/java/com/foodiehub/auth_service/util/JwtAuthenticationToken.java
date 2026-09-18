package com.foodiehub.auth_service.util;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    private final String token;
    private final Object principal;

    // Before JWT validation
    public JwtAuthenticationToken(String token) {
        super((Collection<? extends GrantedAuthority>) null);
        this.token = token;
        this.principal = null;
        setAuthenticated(false);
    }

    // After JWT validation
    public JwtAuthenticationToken(
            Object principal,
            String token,
            Collection<? extends GrantedAuthority> authorities) {

        super(authorities);
        this.token = token;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

}