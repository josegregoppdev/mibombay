package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.ProveedorDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.ProveedorMapper;
import com.mibombay.sistemaresurante.models.Proveedor;
import com.mibombay.sistemaresurante.repositories.ProveedorRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

    public ProveedorService(ProveedorRepository proveedorRepository, ProveedorMapper proveedorMapper) {
        this.proveedorRepository = proveedorRepository;
        this.proveedorMapper = proveedorMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<ProveedorDTO> buscarPaginado(Long empresaId, String busqueda, Pageable pageable) {
        Specification<Proveedor> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("empresaId"), empresaId));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("activo")));
        spec = spec.and((root, query, cb) -> cb.isFalse(root.get("esProveedorDefecto")));

        if (busqueda != null && !busqueda.isBlank()) {
            String pattern = "%" + busqueda.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("razonSocial")), pattern),
                            cb.like(cb.lower(root.get("contacto")), pattern)
                    ));
        }

        return proveedorRepository.findAll(spec, pageable)
                .map(proveedorMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ProveedorDTO obtenerPorId(Long id) {
        Proveedor proveedor = proveedorRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        verificarPerteneceAEmpresa(proveedor);
        return proveedorMapper.toDTO(proveedor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProveedorDTO crear(ProveedorDTO dto) {
        Long empresaActual = TenantContext.getEmpresaId();
        if (empresaActual != null && !empresaActual.equals(dto.getEmpresaId())) {
            throw new BusinessException("No tienes permiso para crear proveedores en esta empresa");
        }
        if (proveedorRepository.existsByRazonSocialAndEmpresaIdAndActivoTrue(dto.getRazonSocial(), dto.getEmpresaId())) {
            throw new BusinessException("Ya existe un proveedor con esa razón social");
        }
        dto.setEsProveedorDefecto(false);
        Proveedor proveedor = proveedorMapper.toEntity(dto);
        proveedor = proveedorRepository.save(proveedor);
        return proveedorMapper.toDTO(proveedor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorDTO dto) {
        Proveedor proveedor = proveedorRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        verificarPerteneceAEmpresa(proveedor);

        if (Boolean.TRUE.equals(proveedor.getEsProveedorDefecto())) {
            throw new BusinessException("No se puede modificar el proveedor por defecto");
        }
        if (!proveedor.getRazonSocial().equals(dto.getRazonSocial())
                && proveedorRepository.existsByRazonSocialAndEmpresaIdAndActivoTrue(dto.getRazonSocial(), proveedor.getEmpresaId())) {
            throw new BusinessException("Ya existe un proveedor con esa razón social");
        }

        proveedorMapper.updateEntity(proveedor, dto);
        proveedor = proveedorRepository.save(proveedor);
        return proveedorMapper.toDTO(proveedor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void eliminar(Long id) {
        Proveedor proveedor = proveedorRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
        verificarPerteneceAEmpresa(proveedor);

        if (Boolean.TRUE.equals(proveedor.getEsProveedorDefecto())) {
            throw new BusinessException("No se puede eliminar el proveedor por defecto");
        }

        proveedor.setActivo(false);
        proveedorRepository.save(proveedor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void crearProveedorDefectoSiNoExiste(Long empresaId) {
        if (proveedorRepository.findByEmpresaIdAndEsProveedorDefectoTrueAndActivoTrue(empresaId).isEmpty()) {
            Proveedor proveedor = Proveedor.builder()
                    .empresaId(empresaId)
                    .razonSocial("Proveedor Genérico")
                    .contacto("Sin contacto")
                    .esProveedorDefecto(true)
                    .build();
            proveedorRepository.save(proveedor);
        }
    }

    private void verificarPerteneceAEmpresa(Proveedor proveedor) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null && !empresaId.equals(proveedor.getEmpresaId())) {
            throw new ResourceNotFoundException("Proveedor no encontrado: " + proveedor.getId());
        }
    }
}
