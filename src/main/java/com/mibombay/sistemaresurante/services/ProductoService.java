package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.ProductoDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.ProductoMapper;
import com.mibombay.sistemaresurante.models.Producto;
import com.mibombay.sistemaresurante.models.Receta;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.repositories.RecetaRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final RecetaRepository recetaRepository;
    private final ProductoMapper productoMapper;

    public ProductoService(ProductoRepository productoRepository,
                           RecetaRepository recetaRepository,
                           ProductoMapper productoMapper) {
        this.productoRepository = productoRepository;
        this.recetaRepository = recetaRepository;
        this.productoMapper = productoMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<ProductoDTO> buscarPaginado(Long empresaId, String nombre, Boolean tieneReceta, Pageable pageable) {
        Specification<Producto> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("empresaId"), empresaId));
        spec = spec.and((root, query, cb) -> cb.isTrue(root.get("activo")));

        if (nombre != null && !nombre.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
        }

        if (tieneReceta != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tieneReceta"), tieneReceta));
        }

        return productoRepository.findAll(spec, pageable)
                .map(p -> {
                    ProductoDTO dto = productoMapper.toDTO(p);
                    calcularCostoYMargen(dto);
                    return dto;
                });
    }

    private void calcularCostoYMargen(ProductoDTO dto) {
        BigDecimal costo;
        if (Boolean.TRUE.equals(dto.getTieneReceta())) {
            costo = recetaRepository.findByProductoIdAndActivoTrue(dto.getId())
                    .map(Receta::getCostoReceta)
                    .orElse(null);
            dto.setCostoReceta(costo);
            dto.setPrecioCompra(null);
        } else {
            costo = dto.getPrecioCompra();
            dto.setCostoReceta(null);
        }

        if (costo != null && costo.compareTo(BigDecimal.ZERO) > 0
                && dto.getPrecioVenta() != null && dto.getPrecioVenta().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margen = dto.getPrecioVenta().subtract(costo)
                    .multiply(new BigDecimal("100"))
                    .divide(dto.getPrecioVenta(), 1, java.math.RoundingMode.HALF_UP);
            dto.setMargenGanancia(margen);
        } else {
            dto.setMargenGanancia(null);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ProductoDTO obtenerPorId(Long id) {
        Producto producto = productoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        verificarPerteneceAEmpresa(producto);
        return productoMapper.toDTO(producto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductoDTO crear(ProductoDTO dto) {
        Long empresaActual = TenantContext.getEmpresaId();
        if (empresaActual != null && !empresaActual.equals(dto.getEmpresaId())) {
            throw new BusinessException("No tienes permiso para crear productos en esta empresa");
        }
        if (productoRepository.existsByNombreAndEmpresaIdAndActivoTrue(dto.getNombre(), dto.getEmpresaId())) {
            throw new BusinessException("Ya existe un producto con el nombre: " + dto.getNombre());
        }

        if (Boolean.TRUE.equals(dto.getTieneReceta())) {
            dto.setPrecioCompra(null);
        }

        Producto producto = productoMapper.toEntity(dto);
        producto = productoRepository.save(producto);
        return productoMapper.toDTO(producto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        Producto producto = productoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        verificarPerteneceAEmpresa(producto);

        if (!producto.getNombre().equals(dto.getNombre())
                && productoRepository.existsByNombreAndEmpresaIdAndActivoTrue(dto.getNombre(), producto.getEmpresaId())) {
            throw new BusinessException("Ya existe un producto con el nombre: " + dto.getNombre());
        }

        if (Boolean.TRUE.equals(dto.getTieneReceta())) {
            dto.setPrecioCompra(null);
        }

        productoMapper.updateEntity(producto, dto);
        producto = productoRepository.save(producto);
        return productoMapper.toDTO(producto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void eliminar(Long id) {
        Producto producto = productoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
        verificarPerteneceAEmpresa(producto);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private void verificarPerteneceAEmpresa(Producto producto) {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId != null && !empresaId.equals(producto.getEmpresaId())) {
            throw new ResourceNotFoundException("Producto no encontrado: " + producto.getId());
        }
    }
}
