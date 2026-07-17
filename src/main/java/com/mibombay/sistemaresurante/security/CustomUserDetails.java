package com.mibombay.sistemaresurante.security;

import com.mibombay.sistemaresurante.models.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final Long empresaId;
    private final String nombre;
    private final String username;
    private final String password;
    private final boolean activo;
    private final boolean esSuperadmin;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.empresaId = usuario.getEmpresaId();
        this.nombre = usuario.getNombre();
        this.username = usuario.getUsername();
        this.password = usuario.getPassword();
        this.activo = usuario.isActivo();
        this.esSuperadmin = usuario.isEsSuperadmin();
        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
        if (usuario.isEsSuperadmin()) {
            auths.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
        }
        this.authorities = List.copyOf(auths);
    }

    public Long getId() { return id; }
    public Long getEmpresaId() { return empresaId; }
    public String getNombre() { return nombre; }
    public boolean isEsSuperadmin() { return esSuperadmin; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return activo; }
}
