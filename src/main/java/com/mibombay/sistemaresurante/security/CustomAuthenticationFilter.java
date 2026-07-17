package com.mibombay.sistemaresurante.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFilter.class);

    public CustomAuthenticationFilter(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String username = obtainUsername(request);
        String password = obtainPassword(request);
        String empresaIdParam = request.getParameter("empresaId");

        log.debug("[AuthFilter] Login attempt: username={}, empresaId={}", username, empresaIdParam);

        if (username == null) {
            log.warn("[AuthFilter] Username is null");
            return null;
        }

        if (empresaIdParam != null && !empresaIdParam.isEmpty()) {
            try {
                Long empresaId = Long.parseLong(empresaIdParam);
                CustomAuthenticationToken authRequest = new CustomAuthenticationToken(username, password, empresaId);
                setDetails(request, authRequest);
                log.debug("[AuthFilter] Authenticating with empresaId={}", empresaId);
                return this.getAuthenticationManager().authenticate(authRequest);
            } catch (NumberFormatException e) {
                log.warn("[AuthFilter] Invalid empresaId format: {}", empresaIdParam);
            }
        }

        UsernamePasswordAuthenticationToken authRequest = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        setDetails(request, authRequest);
        log.debug("[AuthFilter] Authenticating without empresaId (superadmin?)");
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
