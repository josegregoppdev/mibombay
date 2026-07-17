package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.request.EmpresaRequest;
import com.mibombay.sistemaresurante.DTO.response.EmpresaResponse;
import com.mibombay.sistemaresurante.DTO.response.RegistroEmpresaResponse;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.EmpresaMapper;
import com.mibombay.sistemaresurante.models.Cliente;
import com.mibombay.sistemaresurante.models.Empresa;
import com.mibombay.sistemaresurante.models.Proveedor;
import com.mibombay.sistemaresurante.models.Usuario;
import com.mibombay.sistemaresurante.models.enums.Rol;
import com.mibombay.sistemaresurante.repositories.ClienteRepository;
import com.mibombay.sistemaresurante.repositories.EmpresaRepository;
import com.mibombay.sistemaresurante.repositories.ProveedorRepository;
import com.mibombay.sistemaresurante.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;
    private final PasswordEncoder passwordEncoder;

    public EmpresaService(EmpresaRepository empresaRepository, EmpresaMapper empresaMapper,
                          UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                          ProveedorRepository proveedorRepository,
                          PasswordEncoder passwordEncoder) {
        this.empresaRepository = empresaRepository;
        this.empresaMapper = empresaMapper;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> listarTodas() {
        return empresaRepository.findAll().stream()
                .map(empresaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarActivas() {
        return empresaRepository.findAllByActivoTrue().size();
    }

    @Transactional(readOnly = true)
    public long contarInactivas() {
        return empresaRepository.findAll().stream()
                .filter(e -> !e.isActivo())
                .count();
    }

    @Transactional(readOnly = true)
    public long contarTotal() {
        return empresaRepository.count();
    }

    @Transactional(readOnly = true)
    public EmpresaResponse obtenerPorId(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + id));
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public EmpresaResponse crear(EmpresaRequest request) {
        if (empresaRepository.existsBySubdominio(request.getSubdominio())) {
            throw new BusinessException("El subdominio ya está registrado: " + request.getSubdominio());
        }
        Empresa empresa = empresaMapper.toEntity(request);
        empresa = empresaRepository.save(empresa);
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public RegistroEmpresaResponse crearConUsuariosPorDefecto(EmpresaRequest request) {
        if (empresaRepository.existsBySubdominio(request.getSubdominio())) {
            throw new BusinessException("El subdominio ya está registrado: " + request.getSubdominio());
        }

        Empresa empresa = empresaMapper.toEntity(request);
        empresa = empresaRepository.save(empresa);

        String adminUsername = "admin_" + request.getSubdominio();
        String cajeroUsername = "cajero_" + request.getSubdominio();
        String adminPassword = "admin123";
        String cajeroPassword = "cajero123";

        Usuario admin = Usuario.builder()
                .empresaId(empresa.getId())
                .nombre("Administrador")
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .rol(Rol.ADMIN)
                .activo(true)
                .esSuperadmin(false)
                .build();
        usuarioRepository.save(admin);

        Usuario cajero = Usuario.builder()
                .empresaId(empresa.getId())
                .nombre("Cajero")
                .username(cajeroUsername)
                .password(passwordEncoder.encode(cajeroPassword))
                .rol(Rol.CAJERO)
                .activo(true)
                .esSuperadmin(false)
                .build();
        usuarioRepository.save(cajero);

        if (clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresa.getId()).isEmpty()) {
            Cliente consumidorFinal = Cliente.builder()
                    .empresaId(empresa.getId())
                    .nombres("Consumidor")
                    .apellidos("Final")
                    .dni("9999999")
                    .esConsumidorFinal(true)
                    .build();
            clienteRepository.save(consumidorFinal);
        }

        if (proveedorRepository.findByEmpresaIdAndEsProveedorDefectoTrueAndActivoTrue(empresa.getId()).isEmpty()) {
            Proveedor proveedorDefecto = Proveedor.builder()
                    .empresaId(empresa.getId())
                    .razonSocial("Proveedor Genérico")
                    .contacto("Sin contacto")
                    .esProveedorDefecto(true)
                    .build();
            proveedorRepository.save(proveedorDefecto);
        }

        return RegistroEmpresaResponse.builder()
                .empresaId(empresa.getId())
                .nombreEmpresa(empresa.getNombre())
                .subdominio(empresa.getSubdominio())
                .adminUsername(adminUsername)
                .adminPassword(adminPassword)
                .cajeroUsername(cajeroUsername)
                .cajeroPassword(cajeroPassword)
                .build();
    }

    @Transactional
    public EmpresaResponse actualizar(Long id, EmpresaRequest request) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + id));

        if (!empresa.getSubdominio().equals(request.getSubdominio())
                && empresaRepository.existsBySubdominio(request.getSubdominio())) {
            throw new BusinessException("El subdominio ya está registrado: " + request.getSubdominio());
        }

        empresaMapper.updateEntity(empresa, request);
        empresa = empresaRepository.save(empresa);
        return empresaMapper.toResponse(empresa);
    }

    @Transactional
    public void eliminar(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + id));
        empresa.setActivo(false);
        empresaRepository.save(empresa);
    }

    @Transactional
    public void activar(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + id));
        empresa.setActivo(true);
        empresaRepository.save(empresa);
    }

    @Transactional
    public void desactivar(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada: " + id));
        empresa.setActivo(false);
        empresaRepository.save(empresa);
    }
}
