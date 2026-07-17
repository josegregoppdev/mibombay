package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.CompraDTO;
import com.mibombay.sistemaresurante.DTO.CompraDetalleDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.CompraMapper;
import com.mibombay.sistemaresurante.models.Compra;
import com.mibombay.sistemaresurante.models.CompraDetalle;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.Producto;
import com.mibombay.sistemaresurante.models.MovimientoInventario;
import com.mibombay.sistemaresurante.models.Proveedor;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.models.enums.TipoItemCompra;
import com.mibombay.sistemaresurante.repositories.CompraDetalleRepository;
import com.mibombay.sistemaresurante.repositories.CompraRepository;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.MovimientoInventarioRepository;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.repositories.ProveedorRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CompraService {

    private final CompraRepository compraRepository;
    private final CompraDetalleRepository compraDetalleRepository;
    private final IngredienteRepository ingredienteRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CompraMapper compraMapper;

    public CompraService(CompraRepository compraRepository,
                         CompraDetalleRepository compraDetalleRepository,
                         IngredienteRepository ingredienteRepository,
                         ProductoRepository productoRepository,
                         ProveedorRepository proveedorRepository,
                         MovimientoInventarioRepository movimientoInventarioRepository,
                         CompraMapper compraMapper) {
        this.compraRepository = compraRepository;
        this.compraDetalleRepository = compraDetalleRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.compraMapper = compraMapper;
    }

    public Page<CompraDTO> listar(String busqueda, Long proveedorId,
                                  LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable) {
        Long empresaId = TenantContext.getEmpresaId();
        Specification<Compra> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("empresaId"), empresaId));
            predicates.add(cb.isTrue(root.get("activo")));
            if (busqueda != null && !busqueda.isBlank()) {
                String pattern = "%" + busqueda.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("numeroFactura")), pattern));
            }
            if (proveedorId != null) {
                predicates.add(cb.equal(root.get("proveedorId"), proveedorId));
            }
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCompra"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCompra"), fechaHasta.atTime(LocalTime.MAX)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return compraRepository.findAll(spec, pageable).map(compra -> {
            CompraDTO dto = compraMapper.toDTO(compra);
            dto.setNombreProveedor(proveedorRepository.findById(compra.getProveedorId())
                    .map(Proveedor::getRazonSocial).orElse("---"));
            return dto;
        });
    }

    public CompraDTO obtenerPorId(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada: " + id));
        verificarPerteneceAEmpresa(compra);
        CompraDTO dto = compraMapper.toDTO(compra);
        dto.setNombreProveedor(proveedorRepository.findById(compra.getProveedorId())
                .map(Proveedor::getRazonSocial).orElse("---"));
        dto.setDetalles(compraDetalleRepository.findByCompraId(id).stream()
                .map(compraMapper::toDetalleDTO).toList());
        return dto;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CompraDTO crear(CompraDTO dto) {
        Long empresaId = TenantContext.getEmpresaId();
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new BusinessException("Debe agregar al menos un detalle a la compra");
        }

        int countIng = 0, countProd = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CompraDetalle> detalles = new ArrayList<>();

        for (CompraDetalleDTO detDTO : dto.getDetalles()) {
            BigDecimal lineaSubtotal = detDTO.getCantidad().multiply(detDTO.getPrecioUnitario());
            detDTO.setSubtotal(lineaSubtotal);
            subtotal = subtotal.add(lineaSubtotal);
            if (detDTO.getItemTipo() == TipoItemCompra.INGREDIENTE) {
                countIng++;
            } else {
                countProd++;
            }
            CompraDetalle detalle = compraMapper.toDetalleEntity(detDTO);
            detalle.setItemNombre(buscarNombreItem(detDTO));
            detalles.add(detalle);
        }

        String tipo;
        if (countIng > 0 && countProd > 0) {
            tipo = "MIXTO";
        } else if (countIng > 0) {
            tipo = "INGREDIENTES";
        } else {
            tipo = "PRODUCTOS";
        }

        Compra compra = compraMapper.toEntity(dto);
        compra.setEmpresaId(empresaId);
        compra.setSubtotal(subtotal);
        compra.setTotal(subtotal);
        compra.setTipo(tipo);
        compra = compraRepository.save(compra);

        Long compraIdFinal = compra.getId();
        Long usuarioId = dto.getUsuarioId();
        for (CompraDetalle detalle : detalles) {
            detalle.setCompraId(compraIdFinal);
            compraDetalleRepository.save(detalle);
            BigDecimal stockAntes = obtenerStockActualItem(detalle);
            actualizarStock(detalle);
            BigDecimal stockDespues = stockAntes.add(detalle.getCantidad());
            registrarMovimiento(empresaId, usuarioId, detalle.getItemTipo().name(),
                    detalle.getItemId(), detalle.getItemNombre(),
                    MovimientoTipo.COMPRA, compraIdFinal,
                    detalle.getCantidad(), "+", stockAntes, stockDespues, null);
        }

        CompraDTO result = compraMapper.toDTO(compra);
        result.setNombreProveedor(proveedorRepository.findById(compra.getProveedorId())
                .map(Proveedor::getRazonSocial).orElse("---"));
        return result;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CompraDTO actualizar(Long id, CompraDTO dto) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada: " + id));
        verificarPerteneceAEmpresa(compra);
        if (!compra.getActivo()) {
            throw new BusinessException("No se puede editar una compra anulada");
        }

        List<CompraDetalle> oldDetalles = compraDetalleRepository.findByCompraId(id);
        Long empresaIdActual = TenantContext.getEmpresaId();
        for (CompraDetalle old : oldDetalles) {
            BigDecimal stockAntes = obtenerStockActualItem(old);
            revertirStock(old);
            BigDecimal stockDespues = stockAntes.subtract(old.getCantidad());
            registrarMovimiento(empresaIdActual, compra.getUsuarioId(), old.getItemTipo().name(),
                    old.getItemId(), old.getItemNombre(),
                    MovimientoTipo.COMPRA_ANULACION, id,
                    old.getCantidad(), "-", stockAntes, stockDespues,
                    "Anulación por edición de compra");
        }
        compraDetalleRepository.deleteByCompraId(id);
        compraDetalleRepository.flush();

        compra.setProveedorId(dto.getProveedorId());
        compra.setFechaCompra(dto.getFechaCompra());
        compra.setNumeroFactura(dto.getNumeroFactura());
        compra.setObservaciones(dto.getObservaciones());

        List<CompraDetalleDTO> nuevosDetallesDTO = dto.getDetalles() != null
                ? dto.getDetalles().stream()
                    .filter(d -> d.getItemId() != null && d.getCantidad() != null
                            && d.getPrecioUnitario() != null && d.getCantidad().compareTo(BigDecimal.ZERO) > 0)
                    .toList()
                : new ArrayList<>();

        if (nuevosDetallesDTO.isEmpty()) {
            throw new BusinessException("Debe agregar al menos un detalle a la compra");
        }

        int countIng = 0, countProd = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        List<CompraDetalle> nuevosDetalles = new ArrayList<>();

        for (CompraDetalleDTO detDTO : nuevosDetallesDTO) {
            BigDecimal lineaSubtotal = detDTO.getCantidad().multiply(detDTO.getPrecioUnitario());
            detDTO.setSubtotal(lineaSubtotal);
            subtotal = subtotal.add(lineaSubtotal);
            if (detDTO.getItemTipo() == TipoItemCompra.INGREDIENTE) {
                countIng++;
            } else {
                countProd++;
            }
            CompraDetalle detalle = compraMapper.toDetalleEntity(detDTO);
            detalle.setItemNombre(buscarNombreItem(detDTO));
            nuevosDetalles.add(detalle);
        }

        String tipo;
        if (countIng > 0 && countProd > 0) {
            tipo = "MIXTO";
        } else if (countIng > 0) {
            tipo = "INGREDIENTES";
        } else {
            tipo = "PRODUCTOS";
        }

        compra.setSubtotal(subtotal);
        compra.setTotal(subtotal);
        compra.setTipo(tipo);
        compra = compraRepository.save(compra);

        for (CompraDetalle detalle : nuevosDetalles) {
            detalle.setCompraId(compra.getId());
            compraDetalleRepository.save(detalle);
            BigDecimal stockAntes = obtenerStockActualItem(detalle);
            actualizarStock(detalle);
            BigDecimal stockDespues = stockAntes.add(detalle.getCantidad());
            registrarMovimiento(empresaIdActual, dto.getUsuarioId(), detalle.getItemTipo().name(),
                    detalle.getItemId(), detalle.getItemNombre(),
                    MovimientoTipo.COMPRA, id,
                    detalle.getCantidad(), "+", stockAntes, stockDespues,
                    "Corrección por edición de compra");
        }

        CompraDTO result = compraMapper.toDTO(compra);
        result.setNombreProveedor(proveedorRepository.findById(compra.getProveedorId())
                .map(Proveedor::getRazonSocial).orElse("---"));
        result.setDetalles(compraDetalleRepository.findByCompraId(compra.getId()).stream()
                .map(compraMapper::toDetalleDTO).toList());
        return result;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void anular(Long id) {
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada: " + id));
        verificarPerteneceAEmpresa(compra);
        if (!compra.getActivo()) {
            throw new BusinessException("La compra ya está anulada");
        }
        List<CompraDetalle> detalles = compraDetalleRepository.findByCompraId(id);
        Long empresaId = TenantContext.getEmpresaId();
        for (CompraDetalle detalle : detalles) {
            BigDecimal stockAntes = obtenerStockActualItem(detalle);
            revertirStock(detalle);
            BigDecimal stockDespues = stockAntes.subtract(detalle.getCantidad());
            registrarMovimiento(empresaId, compra.getUsuarioId(), detalle.getItemTipo().name(),
                    detalle.getItemId(), detalle.getItemNombre(),
                    MovimientoTipo.COMPRA_ANULACION, id,
                    detalle.getCantidad(), "-", stockAntes, stockDespues, null);
        }
        compra.setActivo(false);
        compraRepository.save(compra);
    }

    private void actualizarStock(CompraDetalle detalle) {
        if (detalle.getItemTipo() == TipoItemCompra.INGREDIENTE) {
            Ingrediente ing = ingredienteRepository.findById(detalle.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + detalle.getItemId()));
            verificarPerteneceAEmpresa(ing.getEmpresaId());
            BigDecimal actualIng = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            ing.setStockActual(actualIng.add(detalle.getCantidad()));
            ingredienteRepository.save(ing);
        } else {
            Producto prod = productoRepository.findById(detalle.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detalle.getItemId()));
            verificarPerteneceAEmpresa(prod.getEmpresaId());
            BigDecimal actualProd = prod.getStockActual() != null ? prod.getStockActual() : BigDecimal.ZERO;
            prod.setStockActual(actualProd.add(detalle.getCantidad()));
            productoRepository.save(prod);
        }
    }

    private void revertirStock(CompraDetalle detalle) {
        if (detalle.getItemTipo() == TipoItemCompra.INGREDIENTE) {
            Ingrediente ing = ingredienteRepository.findById(detalle.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + detalle.getItemId()));
            BigDecimal actualIng = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            ing.setStockActual(actualIng.subtract(detalle.getCantidad()));
            ingredienteRepository.save(ing);
        } else {
            Producto prod = productoRepository.findById(detalle.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detalle.getItemId()));
            BigDecimal actualProd = prod.getStockActual() != null ? prod.getStockActual() : BigDecimal.ZERO;
            prod.setStockActual(actualProd.subtract(detalle.getCantidad()));
            productoRepository.save(prod);
        }
    }

    private BigDecimal obtenerStockActualItem(CompraDetalle detalle) {
        if (detalle.getItemTipo() == TipoItemCompra.INGREDIENTE) {
            return ingredienteRepository.findById(detalle.getItemId())
                    .map(Ingrediente::getStockActual)
                    .orElse(BigDecimal.ZERO);
        }
        return productoRepository.findById(detalle.getItemId())
                .map(Producto::getStockActual)
                .orElse(BigDecimal.ZERO);
    }

    private void registrarMovimiento(Long empresaId, Long usuarioId,
                                     String itemTipo, Long itemId, String itemNombre,
                                     MovimientoTipo movimientoTipo, Long referenciaId,
                                     BigDecimal cantidad, String signo,
                                     BigDecimal stockAnterior, BigDecimal stockPosterior,
                                     String observacion) {
        MovimientoInventario mov = MovimientoInventario.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .itemTipo(itemTipo)
                .itemId(itemId)
                .itemNombre(itemNombre)
                .movimientoTipo(movimientoTipo)
                .referenciaId(referenciaId)
                .cantidad(cantidad)
                .signo(signo)
                .stockAnterior(stockAnterior)
                .stockPosterior(stockPosterior)
                .fechaMovimiento(LocalDateTime.now())
                .observacion(observacion)
                .build();
        movimientoInventarioRepository.save(mov);
    }

    private String buscarNombreItem(CompraDetalleDTO detDTO) {
        if (detDTO.getItemTipo() == TipoItemCompra.INGREDIENTE) {
            return ingredienteRepository.findById(detDTO.getItemId())
                    .map(Ingrediente::getNombre)
                    .orElse("Ingrediente #" + detDTO.getItemId());
        }
        return productoRepository.findById(detDTO.getItemId())
                .map(Producto::getNombre)
                .orElse("Producto #" + detDTO.getItemId());
    }

    private void verificarPerteneceAEmpresa(Compra compra) {
        Long empresaId = TenantContext.getEmpresaId();
        if (!compra.getEmpresaId().equals(empresaId)) {
            throw new ResourceNotFoundException("Compra no encontrada: " + compra.getId());
        }
    }

    private void verificarPerteneceAEmpresa(Long empresaId) {
        Long current = TenantContext.getEmpresaId();
        if (!empresaId.equals(current)) {
            throw new ResourceNotFoundException("El recurso no pertenece a su empresa");
        }
    }
}
