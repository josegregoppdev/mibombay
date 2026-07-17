package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.IngredienteDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.IngredienteMapper;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.enums.UnidadMedida;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.RecetaDetalleRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class IngredienteService {

    private final IngredienteRepository ingredienteRepository;
    private final IngredienteMapper ingredienteMapper;
    private final RecetaDetalleRepository recetaDetalleRepository;

    public IngredienteService(IngredienteRepository ingredienteRepository,
                              IngredienteMapper ingredienteMapper,
                              RecetaDetalleRepository recetaDetalleRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.ingredienteMapper = ingredienteMapper;
        this.recetaDetalleRepository = recetaDetalleRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<IngredienteDTO> buscarPaginado(Long empresaId, String nombre, UnidadMedida unidad, Pageable pageable) {
        Specification<Ingrediente> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("empresaId"), empresaId));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("activo")));

        if (nombre != null && !nombre.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
        }
        if (unidad != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("unidadMedida"), unidad));
        }

        return ingredienteRepository.findAll(spec, pageable)
                .map(ingredienteMapper::toDTO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public IngredienteDTO obtenerPorId(Long id) {
        Ingrediente ingrediente = ingredienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + id));
        verificarPerteneceAEmpresa(ingrediente);
        return ingredienteMapper.toDTO(ingrediente);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public IngredienteDTO crear(IngredienteDTO dto) {
        Long empresaActual = TenantContext.getEmpresaId();
        if (empresaActual != null && !empresaActual.equals(dto.getEmpresaId())) {
            throw new BusinessException("No tienes permiso para crear ingredientes en esta empresa");
        }
        if (ingredienteRepository.existsByNombreAndEmpresaIdAndActivoTrue(dto.getNombre(), dto.getEmpresaId())) {
            throw new BusinessException("Ya existe un ingrediente con el nombre: " + dto.getNombre());
        }
        Ingrediente ingrediente = ingredienteMapper.toEntity(dto);
        ingrediente = ingredienteRepository.save(ingrediente);
        return ingredienteMapper.toDTO(ingrediente);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public IngredienteDTO actualizar(Long id, IngredienteDTO dto) {
        Ingrediente ingrediente = ingredienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + id));
        verificarPerteneceAEmpresa(ingrediente);

        if (!ingrediente.getNombre().equals(dto.getNombre())
                && ingredienteRepository.existsByNombreAndEmpresaIdAndActivoTrue(dto.getNombre(), ingrediente.getEmpresaId())) {
            throw new BusinessException("Ya existe un ingrediente con el nombre: " + dto.getNombre());
        }

        if (!Objects.equals(ingrediente.getConsumible(), dto.getConsumible())) {
            throw new BusinessException("El tipo de ingrediente (consumible/para receta) se define al crear y no se puede modificar posteriormente");
        }

        ingredienteMapper.updateEntity(ingrediente, dto);
        ingrediente = ingredienteRepository.save(ingrediente);
        return ingredienteMapper.toDTO(ingrediente);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void eliminar(Long id) {
        Ingrediente ingrediente = ingredienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + id));
        verificarPerteneceAEmpresa(ingrediente);

        if (recetaDetalleRepository.existsByIngredienteId(id)) {
            throw new BusinessException("No se puede eliminar el ingrediente porque está siendo usado en una o más recetas");
        }

        ingrediente.setActivo(false);
        ingredienteRepository.save(ingrediente);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public long contarPorEmpresa(Long empresaId) {
        return ingredienteRepository.findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId).size();
    }

    private void verificarPerteneceAEmpresa(Ingrediente ingrediente) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null && !empresaId.equals(ingrediente.getEmpresaId())) {
            throw new ResourceNotFoundException("Ingrediente no encontrado: " + ingrediente.getId());
        }
    }
}
