package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.request.UsuarioRequest;
import com.mibombay.sistemaresurante.DTO.response.UsuarioResponse;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.UsuarioMapper;
import com.mibombay.sistemaresurante.models.Usuario;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponse> listarUsuariosPorEmpresa(Long empresaId) {
        return usuarioRepository.findAllByEmpresaIdAndActivoTrue(empresaId).stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse obtenerUsuarioPorId(Long id) {
        Long empresaId = TenantContext.getEmpresaId();
        Usuario usuario = usuarioRepository
                .findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse crearUsuario(UsuarioRequest request) {
        Long empresaActual = TenantContext.getEmpresaId();
        if (empresaActual != null && !empresaActual.equals(request.getEmpresaId())) {
            throw new BusinessException("No puede crear usuarios en otra empresa");
        }

        if (usuarioRepository.existsByUsernameAndEmpresaId(request.getUsername(), request.getEmpresaId())) {
            throw new BusinessException("El username ya existe en esta empresa: " + request.getUsername());
        }

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse actualizarUsuario(Long id, UsuarioRequest request) {
        Long empresaId = TenantContext.getEmpresaId();
        Usuario usuario = usuarioRepository
                .findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));

        if (request.getEmpresaId() != null && !empresaId.equals(request.getEmpresaId())) {
            throw new BusinessException("No puede mover usuarios a otra empresa");
        }

        if (!usuario.getUsername().equals(request.getUsername())
                && usuarioRepository.existsByUsernameAndEmpresaId(request.getUsername(), request.getEmpresaId())) {
            throw new BusinessException("El username ya existe en esta empresa: " + request.getUsername());
        }

        usuarioMapper.updateEntity(usuario, request);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminarUsuario(Long id) {
        Long empresaId = TenantContext.getEmpresaId();
        Usuario usuario = usuarioRepository
                .findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public long contarUsuariosPorEmpresa(Long empresaId) {
        return usuarioRepository.countByEmpresaId(empresaId);
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    public long contarTodosGlobalUsuarios() {
        return usuarioRepository.count();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void activarUsuario(Long id) {
        Long empresaId = TenantContext.getEmpresaId();
        Usuario usuario = usuarioRepository
                .findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}
