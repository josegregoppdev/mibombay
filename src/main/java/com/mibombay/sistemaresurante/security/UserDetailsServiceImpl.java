package com.mibombay.sistemaresurante.security;

import com.mibombay.sistemaresurante.models.Usuario;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameAndEsSuperadminTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("Superadmin no encontrado: " + username));
        return new CustomUserDetails(usuario);
    }

    public UserDetails loadUserByUsernameAndEmpresa(String username, Long empresaId) {
        Usuario usuario = usuarioRepository.findByUsernameAndEmpresaId(username, empresaId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username + " en empresa " + empresaId));
        return new CustomUserDetails(usuario);
    }
}
