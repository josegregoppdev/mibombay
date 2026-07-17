package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.ClienteDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.ClienteMapper;
import com.mibombay.sistemaresurante.models.Cliente;
import com.mibombay.sistemaresurante.repositories.ClienteRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public ClienteService(ClienteRepository clienteRepository, ClienteMapper clienteMapper) {
        this.clienteRepository = clienteRepository;
        this.clienteMapper = clienteMapper;
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public Page<ClienteDTO> buscarPaginado(Long empresaId, String busqueda, Pageable pageable) {
        Specification<Cliente> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("empresaId"), empresaId));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("activo")));
        spec = spec.and((root, query, cb) -> cb.isFalse(root.get("esConsumidorFinal")));

        if (busqueda != null && !busqueda.isBlank()) {
            String pattern = "%" + busqueda.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("nombres")), pattern),
                            cb.like(cb.lower(root.get("apellidos")), pattern),
                            cb.like(root.get("telefono"), pattern),
                            cb.like(root.get("dni"), pattern)
                    ));
        }

        return clienteRepository.findAll(spec, pageable)
                .map(clienteMapper::toDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ClienteDTO obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
        verificarPerteneceAEmpresa(cliente);
        return clienteMapper.toDTO(cliente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public ClienteDTO obtenerConsumidorFinal(Long empresaId) {
        Cliente cliente = clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumidor Final no encontrado"));
        return clienteMapper.toDTO(cliente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    @Transactional
    public ClienteDTO crear(ClienteDTO dto) {
        Long empresaActual = TenantContext.getEmpresaId();
        if (empresaActual != null && !empresaActual.equals(dto.getEmpresaId())) {
            throw new BusinessException("No tienes permiso para crear clientes en esta empresa");
        }
        dto.setEsConsumidorFinal(false);
        if (dto.getDni() == null || dto.getDni().isBlank()) {
            throw new BusinessException("El DNI es obligatorio");
        }
        if (clienteRepository.existsByDniAndEmpresaIdAndActivoTrue(dto.getDni(), dto.getEmpresaId())) {
            throw new BusinessException("Ya existe un cliente con este DNI en la empresa");
        }
        Cliente cliente = clienteMapper.toEntity(dto);
        cliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(cliente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    @Transactional
    public ClienteDTO actualizar(Long id, ClienteDTO dto) {
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
        verificarPerteneceAEmpresa(cliente);

        if (Boolean.TRUE.equals(cliente.getEsConsumidorFinal())) {
            throw new BusinessException("No se puede modificar el cliente Consumidor Final");
        }

        if (dto.getDni() != null && !dto.getDni().isBlank()
                && !dto.getDni().equals(cliente.getDni())) {
            if (clienteRepository.existsByDniAndEmpresaIdAndActivoTrue(dto.getDni(), cliente.getEmpresaId())) {
                throw new BusinessException("Ya existe otro cliente con este DNI en la empresa");
            }
        }
        clienteMapper.updateEntity(cliente, dto);
        cliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(cliente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
        verificarPerteneceAEmpresa(cliente);

        if (Boolean.TRUE.equals(cliente.getEsConsumidorFinal())) {
            throw new BusinessException("No se puede eliminar el cliente Consumidor Final");
        }

        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    @Transactional
    public void crearConsumidorFinalSiNoExiste(Long empresaId) {
        if (clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId).isEmpty()) {
            Cliente cliente = Cliente.builder()
                    .empresaId(empresaId)
                    .nombres("Consumidor")
                    .apellidos("Final")
                    .dni("9999999")
                    .esConsumidorFinal(true)
                    .build();
            clienteRepository.save(cliente);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    @Transactional
    public long contarPorEmpresa(Long empresaId) {
        return clienteRepository.findAll(
                Specification.<Cliente>where((root, query, cb) -> cb.equal(root.get("empresaId"), empresaId))
                        .and((root, query, cb) -> cb.isTrue(root.get("activo")))
                        .and((root, query, cb) -> cb.isFalse(root.get("esConsumidorFinal"))),
                Pageable.unpaged()).getTotalElements();
    }

    private void verificarPerteneceAEmpresa(Cliente cliente) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null && !empresaId.equals(cliente.getEmpresaId())) {
            throw new ResourceNotFoundException("Cliente no encontrado: " + cliente.getId());
        }
    }
}
