package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.VentaDTO;
import com.mibombay.sistemaresurante.DTO.VentaDetalleDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.mapper.VentaMapper;
import com.mibombay.sistemaresurante.models.*;
import com.mibombay.sistemaresurante.models.enums.MetodoPago;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.models.enums.TipoVenta;
import com.mibombay.sistemaresurante.repositories.*;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class VentaService {

    private final VentaRepository ventaRepository;
    private final VentaDetalleRepository ventaDetalleRepository;
    private final ProductoRepository productoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final RecetaRepository recetaRepository;
    private final RecetaDetalleRepository recetaDetalleRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CierreZRepository cierreZRepository;
    private final VentaMapper ventaMapper;
    private final NotificacionService notificacionService;

    public VentaService(VentaRepository ventaRepository,
            VentaDetalleRepository ventaDetalleRepository,
            ProductoRepository productoRepository,
            IngredienteRepository ingredienteRepository,
            RecetaRepository recetaRepository,
            RecetaDetalleRepository recetaDetalleRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            CierreZRepository cierreZRepository,
            VentaMapper ventaMapper,
            NotificacionService notificacionService) {
        this.ventaRepository = ventaRepository;
        this.ventaDetalleRepository = ventaDetalleRepository;
        this.productoRepository = productoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.recetaRepository = recetaRepository;
        this.recetaDetalleRepository = recetaDetalleRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.cierreZRepository = cierreZRepository;
        this.ventaMapper = ventaMapper;
        this.notificacionService = notificacionService;
    }

    /*
     * Lista ventas con filtros dinámicos (búsqueda, cliente, fechas, para llevar).
     * Construye condiciones según los parámetros recibidos y devuelve página
     * ordenada por fecha descendente.
     */

    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public Page<VentaDTO> listarVentasConFiltros(String busqueda, Long clienteId,
            LocalDate fechaDesde, LocalDate fechaHasta,
            Boolean paraLlevar,
            Pageable pageable) {
        Long empresaId = TenantContext.getEmpresaId();
        Specification<Venta> spec = (tabla, consulta, criteria) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(criteria.equal(tabla.get("empresaId"), empresaId));
            condiciones.add(criteria.isTrue(tabla.get("activo")));
            if (busqueda != null && !busqueda.isBlank()) {
                condiciones.add(criteria.like(criteria.lower(tabla.get("id").as(String.class)),
                        "%" + busqueda.toLowerCase() + "%"));
            }
            if (clienteId != null) {
                condiciones.add(criteria.equal(tabla.get("clienteId"), clienteId));
            }
            if (fechaDesde != null) {
                condiciones.add(criteria.greaterThanOrEqualTo(tabla.get("fechaVenta"), fechaDesde.atStartOfDay()));
            }
            if (fechaHasta != null) {
                condiciones.add(criteria.lessThanOrEqualTo(tabla.get("fechaVenta"), fechaHasta.atTime(LocalTime.MAX)));
            }
            if (paraLlevar != null) {
                condiciones.add(criteria.equal(tabla.get("paraLlevar"), paraLlevar));
            }
            consulta.orderBy(criteria.desc(tabla.get("fechaVenta")));
            return criteria.and(condiciones.toArray(new Predicate[0]));
        };
        return ventaRepository.findAll(spec, pageable).map(this::toDTOConRelaciones);
    }

    /*
     * Obtiene una venta completa con sus detalles por ID.
     * Lanza ResourceNotFoundException si no existe o no pertenece a la empresa.
     */
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public VentaDTO obtenerVentaPorId(Long id) {
        Long empresaId = TenantContext.getEmpresaId();
        Venta venta = ventaRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
        VentaDTO dto = toDTOConRelaciones(venta);
        dto.setDetalles(ventaDetalleRepository.findByVentaId(id).stream()
                .map(ventaMapper::toDetalleDTO).toList());
        return dto;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public VentaDTO crear(VentaDTO dto) {
        Long empresaId = TenantContext.getEmpresaId();

        if (cierreZRepository.existsByEmpresaIdAndFechaAndActivoTrue(empresaId, LocalDate.now())) {
            throw new BusinessException("No se pueden registrar ventas: el día de hoy ya tiene un cierre Z");
        }
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new BusinessException("Debe agregar al menos un producto a la venta");
        }
        if (dto.getClienteId() == null) {
            Cliente cf = clienteRepository.findByEmpresaIdAndEsConsumidorFinalTrueAndActivoTrue(empresaId)
                    .orElseThrow(() -> new BusinessException("Debe seleccionar un cliente"));
            dto.setClienteId(cf.getId());
        }

        List<VentaDetalle> detallesEntity = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (VentaDetalleDTO detDTO : dto.getDetalles()) {
            Producto producto = productoRepository.findByIdAndActivoTrue(detDTO.getProductoId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Producto no encontrado: " + detDTO.getProductoId()));
            verificarPerteneceAEmpresa(producto.getEmpresaId());

            BigDecimal stockDisponible = obtenerStockProducto(producto);
            if (stockDisponible.compareTo(detDTO.getCantidad()) < 0) {
                throw new BusinessException("Stock insuficiente para " + producto.getNombre()
                        + ". Disponible: " + stockDisponible + ", requerido: " + detDTO.getCantidad());
            }

            detDTO.setProductoNombre(producto.getNombre());
            BigDecimal lineaSubtotal = detDTO.getCantidad().multiply(detDTO.getPrecioUnitario())
                    .setScale(2, RoundingMode.HALF_UP);
            detDTO.setSubtotal(lineaSubtotal);
            total = total.add(lineaSubtotal);

            VentaDetalle detalle = ventaMapper.toDetalleEntity(detDTO);
            detallesEntity.add(detalle);
        }

        BigDecimal efectivo = dto.getRecibidoEfectivo() != null ? dto.getRecibidoEfectivo() : BigDecimal.ZERO;
        BigDecimal transferencia = dto.getRecibidoTransferencia() != null ? dto.getRecibidoTransferencia()
                : BigDecimal.ZERO;

        Venta venta = Venta.builder()
                .empresaId(empresaId)
                .usuarioId(dto.getUsuarioId())
                .clienteId(dto.getClienteId())
                .tipoVenta(TipoVenta.BARRA)
                .subtotal(total)
                .total(total)
                .metodoPago(dto.getMetodoPago())
                .paraLlevar(dto.getParaLlevar() != null && dto.getParaLlevar())
                .recibidoEfectivo(efectivo)
                .recibidoTransferencia(transferencia)
                .cambio(calcularCambio(dto.getMetodoPago(), efectivo, transferencia, total))
                .observaciones(dto.getObservaciones())
                .build();
        venta = ventaRepository.save(venta);

        Long ventaIdFinal = venta.getId();
        Long usuarioId = dto.getUsuarioId();

        for (int i = 0; i < detallesEntity.size(); i++) {
            VentaDetalle detalle = detallesEntity.get(i);
            VentaDetalleDTO detDTO = dto.getDetalles().get(i);
            detalle.setVentaId(ventaIdFinal);
            ventaDetalleRepository.save(detalle);

            Set<Long> excluidos = detDTO.getIngredientesExcluidosIds() != null
                    ? new HashSet<>(detDTO.getIngredientesExcluidosIds())
                    : new HashSet<>();
            descontarStockProducto(detDTO.getProductoId(), detDTO.getCantidad(), detDTO.getProductoNombre(),
                    empresaId, usuarioId, ventaIdFinal, excluidos);
        }

        VentaDTO result = toDTOConRelaciones(venta);
        result.setDetalles(ventaDetalleRepository.findByVentaId(ventaIdFinal).stream()
                .map(ventaMapper::toDetalleDTO).toList());

        try {
            notificacionService.notificarNuevaVenta(ventaIdFinal, venta.getTotal(),
                    venta.getMetodoPago() != null ? venta.getMetodoPago().name() : null,
                    venta.getFechaVenta(), result.getNombreUsuario(), result.getNombreCliente(), empresaId);
        } catch (Exception ignored) {
        }

        return result;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','CAJERO')")
    public void anular(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
        verificarPerteneceAEmpresa(venta.getEmpresaId());
        if (!venta.getActivo()) {
            throw new BusinessException("La venta ya está anulada");
        }

        List<VentaDetalle> detalles = ventaDetalleRepository.findByVentaId(id);
        Long empresaId = TenantContext.getEmpresaId();

        for (VentaDetalle detalle : detalles) {
            revertirStockProducto(detalle.getProductoId(), detalle.getCantidad(), detalle.getProductoNombre(),
                    empresaId, venta.getUsuarioId(), id);
        }

        venta.setActivo(false);
        ventaRepository.save(venta);

        try {
            notificacionService.notificarVentaAnulada(id, empresaId);
        } catch (Exception ignored) {
        }
    }

    private BigDecimal obtenerStockProducto(Producto producto) {
        if (Boolean.TRUE.equals(producto.getTieneReceta())) {
            return calcularStockReceta(producto);
        }
        return producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;
    }

    private BigDecimal calcularStockReceta(Producto producto) {
        Optional<Receta> optReceta = recetaRepository.findByProductoIdAndActivoTrue(producto.getId());
        if (optReceta.isEmpty())
            return BigDecimal.ZERO;
        List<RecetaDetalle> detalles = recetaDetalleRepository.findByRecetaId(optReceta.get().getId());
        if (detalles.isEmpty())
            return BigDecimal.ZERO;

        BigDecimal stockMinimo = null;
        for (RecetaDetalle det : detalles) {
            Ingrediente ing = ingredienteRepository.findById(det.getIngredienteId()).orElse(null);
            if (ing == null)
                continue;
            BigDecimal stockIng = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            if (det.getCantidad().compareTo(BigDecimal.ZERO) <= 0)
                continue;
            BigDecimal stockCalculado = stockIng.divide(det.getCantidad(), 2, RoundingMode.FLOOR);
            if (stockMinimo == null || stockCalculado.compareTo(stockMinimo) < 0) {
                stockMinimo = stockCalculado;
            }
        }
        return stockMinimo != null ? stockMinimo : BigDecimal.ZERO;
    }

    private void descontarStockProducto(Long productoId, BigDecimal cantidadVendida, String productoNombre,
            Long empresaId, Long usuarioId, Long ventaId,
            Set<Long> ingredientesExcluidos) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

        if (Boolean.TRUE.equals(producto.getTieneReceta())) {
            Optional<Receta> optReceta = recetaRepository.findByProductoIdAndActivoTrue(productoId);
            if (optReceta.isPresent()) {
                List<RecetaDetalle> recetaDetalles = recetaDetalleRepository.findByRecetaId(optReceta.get().getId());
                for (RecetaDetalle rd : recetaDetalles) {
                    if (ingredientesExcluidos != null && ingredientesExcluidos.contains(rd.getIngredienteId()))
                        continue;
                    Ingrediente ing = ingredienteRepository.findById(rd.getIngredienteId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Ingrediente no encontrado: " + rd.getIngredienteId()));
                    BigDecimal cantidadDescontar = rd.getCantidad().multiply(cantidadVendida);
                    BigDecimal stockAntes = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
                    ing.setStockActual(stockAntes.subtract(cantidadDescontar));
                    if (ing.getStockActual().compareTo(BigDecimal.ZERO) < 0) {
                        ing.setStockActual(BigDecimal.ZERO);
                    }
                    ingredienteRepository.save(ing);
                    registrarMovimientoInventario(empresaId, usuarioId, "INGREDIENTE", ing.getId(),
                            ing.getNombre(), MovimientoTipo.VENTA, ventaId,
                            cantidadDescontar, "-", stockAntes, ing.getStockActual(), null);
                }
            }
        } else {
            BigDecimal stockAntes = producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;
            producto.setStockActual(stockAntes.subtract(cantidadVendida));
            if (producto.getStockActual().compareTo(BigDecimal.ZERO) < 0) {
                producto.setStockActual(BigDecimal.ZERO);
            }
            productoRepository.save(producto);
            registrarMovimientoInventario(empresaId, usuarioId, "PRODUCTO", productoId,
                    productoNombre, MovimientoTipo.VENTA, ventaId,
                    cantidadVendida, "-", stockAntes, producto.getStockActual(), null);
        }
    }

    private void revertirStockProducto(Long productoId, BigDecimal cantidadVendida, String productoNombre,
            Long empresaId, Long usuarioId, Long ventaId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

        if (Boolean.TRUE.equals(producto.getTieneReceta())) {
            Optional<Receta> optReceta = recetaRepository.findByProductoIdAndActivoTrue(productoId);
            if (optReceta.isPresent()) {
                List<RecetaDetalle> recetaDetalles = recetaDetalleRepository.findByRecetaId(optReceta.get().getId());
                for (RecetaDetalle rd : recetaDetalles) {
                    Ingrediente ing = ingredienteRepository.findById(rd.getIngredienteId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Ingrediente no encontrado: " + rd.getIngredienteId()));
                    BigDecimal cantidadReversar = rd.getCantidad().multiply(cantidadVendida);
                    BigDecimal stockAntes = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
                    ing.setStockActual(stockAntes.add(cantidadReversar));
                    ingredienteRepository.save(ing);
                    registrarMovimientoInventario(empresaId, usuarioId, "INGREDIENTE", ing.getId(),
                            ing.getNombre(), MovimientoTipo.VENTA_ANULACION, ventaId,
                            cantidadReversar, "+", stockAntes, ing.getStockActual(),
                            "Anulación de venta #" + ventaId);
                }
            }
        } else {
            BigDecimal stockAntes = producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;
            producto.setStockActual(stockAntes.add(cantidadVendida));
            productoRepository.save(producto);
            registrarMovimientoInventario(empresaId, usuarioId, "PRODUCTO", productoId,
                    productoNombre, MovimientoTipo.VENTA_ANULACION, ventaId,
                    cantidadVendida, "+", stockAntes, producto.getStockActual(),
                    "Anulación de venta #" + ventaId);
        }
    }

    private void registrarMovimientoInventario(Long empresaId, Long usuarioId,
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

    private BigDecimal calcularCambio(MetodoPago metodo, BigDecimal efectivo, BigDecimal transferencia,
            BigDecimal total) {
        if (metodo == MetodoPago.TRANSFERENCIA)
            return BigDecimal.ZERO;
        BigDecimal recibido = efectivo.add(transferencia);
        BigDecimal cambio = recibido.subtract(total);
        return cambio.compareTo(BigDecimal.ZERO) > 0 ? cambio : BigDecimal.ZERO;
    }

    private VentaDTO toDTOConRelaciones(Venta venta) {
        VentaDTO dto = ventaMapper.toDTO(venta);
        dto.setNombreUsuario(usuarioRepository.findById(venta.getUsuarioId())
                .map(u -> u.getNombre() + (u.getApellido() != null ? " " + u.getApellido() : ""))
                .orElse("Usuario #" + venta.getUsuarioId()));
        dto.setNombreCliente(clienteRepository.findById(venta.getClienteId())
                .map(c -> c.getNombres() + " " + c.getApellidos())
                .orElse("Cliente #" + venta.getClienteId()));
        return dto;
    }

    private void verificarPerteneceAEmpresa(Long empresaId) {
        Long current = TenantContext.getEmpresaId();
        if (!empresaId.equals(current)) {
            throw new ResourceNotFoundException("El recurso no pertenece a su empresa");
        }
    }
}
