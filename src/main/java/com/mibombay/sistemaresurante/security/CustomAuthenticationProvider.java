package com.mibombay.sistemaresurante.security;

import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationProvider(UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.debug("[AuthProvider] authenticate() called: authType={}, principal={}",
                authentication.getClass().getSimpleName(), authentication.getPrincipal());

        if (authentication.getPrincipal() == null || authentication.getCredentials() == null) {
            log.warn("[AuthProvider] Principal or credentials is null");
            throw new BadCredentialsException("Credenciales inválidas");
        }
        String username = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();
        CustomUserDetails userDetails;

        if (authentication instanceof CustomAuthenticationToken customAuth) {
            log.debug("[AuthProvider] CustomAuthenticationToken with empresaId={}", customAuth.getEmpresaId());
            userDetails = (CustomUserDetails) userDetailsService
                    .loadUserByUsernameAndEmpresa(username, customAuth.getEmpresaId());
        } else {
            log.debug("[AuthProvider] Regular UsernamePasswordAuthenticationToken");
            userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
        }

        log.debug("[AuthProvider] User found: username={}, empresaId={}, active={}",
                userDetails.getUsername(), userDetails.getEmpresaId(), userDetails.isEnabled());

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            log.warn("[AuthProvider] Password mismatch for user: {}", username);
            throw new BadCredentialsException("Credenciales inválidas");
        }

        TenantContext.setEmpresaId(userDetails.getEmpresaId());
        log.debug("[AuthProvider] Authentication SUCCESS. TenantContext empresaId={}", TenantContext.getEmpresaId());

        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class)
                || authentication.equals(CustomAuthenticationToken.class);
    }
}
