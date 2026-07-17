package com.mibombay.sistemaresurante.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final Long empresaId;

    public CustomAuthenticationToken(Object principal, Object credentials, Long empresaId) {
        super(principal, credentials);
        this.empresaId = empresaId;
    }

    public Long getEmpresaId() {
        return empresaId;
    }
}
