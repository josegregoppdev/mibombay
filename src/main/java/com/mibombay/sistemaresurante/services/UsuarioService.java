package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.request.UsuarioRequest;
import com.mibombay.sistemaresurante.DTO.response.UsuarioResponse;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.UsuarioMapper;
import com.mibombay.sistemaresurante.models.Usuario;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
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

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponse> listarPorEmpresa(Long empresaId) {
        return usuarioRepository.findAllByEmpresaIdAndActivoTrue(empresaId).stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse crear(UsuarioRequest request) {
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
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));

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
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public long contarPorEmpresa(Long empresaId) {
        return usuarioRepository.countByEmpresaId(empresaId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public long contarTotal() {
        return usuarioRepository.count();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void activar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }
}
