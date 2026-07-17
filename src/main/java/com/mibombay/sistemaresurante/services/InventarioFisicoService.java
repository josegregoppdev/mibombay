package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.*;
import com.mibombay.sistemaresurante.models.enums.InventarioEstado;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.repositories.InventarioFisicoRepository;
import com.mibombay.sistemaresurante.repositories.InventarioFisicoDetalleRepository;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.MovimientoInventarioRepository;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
import com.mibombay.sistemaresurante.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class InventarioFisicoService {

    private final InventarioFisicoRepository inventarioFisicoRepository;
    private final InventarioFisicoDetalleRepository detalleRepository;
    private final IngredienteRepository ingredienteRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public InventarioFisicoService(InventarioFisicoRepository inventarioFisicoRepository,
                                   InventarioFisicoDetalleRepository detalleRepository,
                                   IngredienteRepository ingredienteRepository,
                                   ProductoRepository productoRepository,
                                   MovimientoInventarioRepository movimientoInventarioRepository) {
        this.inventarioFisicoRepository = inventarioFisicoRepository;
        this.detalleRepository = detalleRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.productoRepository = productoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<InventarioFisico> listar(Long empresaId) {
        return inventarioFisicoRepository.findByEmpresaIdAndActivoTrueOrderByFechaDescIdDesc(empresaId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public InventarioFisico obtenerPorId(Long id) {
        Long empresaId = TenantContext.getEmpresaId();
        return inventarioFisicoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario físico no encontrado: " + id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<InventarioFisicoDetalle> obtenerDetalles(Long inventarioId) {
        return detalleRepository.findByInventarioFisicoIdAndActivoTrueOrderByItemTipoAscItemNombreAsc(inventarioId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean existeConfirmadoHoy(Long empresaId) {
        return inventarioFisicoRepository.existsByEmpresaIdAndFechaAndEstadoAndActivoTrue(
                empresaId, LocalDate.now(), InventarioEstado.CONFIRMADO);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public InventarioFisico obtenerBorradorHoy(Long empresaId) {
        return inventarioFisicoRepository
                .findByEmpresaIdAndFechaAndEstadoAndActivoTrue(empresaId, LocalDate.now(), InventarioEstado.BORRADOR)
                .orElse(null);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public InventarioFisico crearBorrador(Long usuarioId, Long empresaId,
                                           Map<String, BigDecimal> stocksFisicos,
                                           Map<String, BigDecimal> mermas,
                                           Map<String, BigDecimal> desperdicios) {
        if (existeConfirmadoHoy(empresaId)) {
            throw new BusinessException("Ya existe un inventario físico confirmado para hoy");
        }
        InventarioFisico existente = obtenerBorradorHoy(empresaId);
        if (existente != null) {
            return actualizarBorrador(existente.getId(), stocksFisicos, mermas, desperdicios, empresaId);
        }

        InventarioFisico inventario = InventarioFisico.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .fecha(LocalDate.now())
                .estado(InventarioEstado.BORRADOR)
                .build();
        inventario = inventarioFisicoRepository.save(inventario);

        List<InventarioFisicoDetalle> detalles = construirDetalles(inventario.getId(), empresaId,
                stocksFisicos, mermas, desperdicios);
        detalleRepository.saveAll(detalles);

        return inventario;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public InventarioFisico actualizarBorrador(Long id, Map<String, BigDecimal> stocksFisicos,
                                                Map<String, BigDecimal> mermas,
                                                Map<String, BigDecimal> desperdicios,
                                                Long empresaId) {
        InventarioFisico inventario = inventarioFisicoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario físico no encontrado: " + id));
        if (inventario.getEstado() != InventarioEstado.BORRADOR) {
            throw new BusinessException("No se puede modificar un inventario confirmado");
        }

        List<InventarioFisicoDetalle> detallesActuales = detalleRepository
                .findByInventarioFisicoIdAndActivoTrue(inventario.getId());

        for (InventarioFisicoDetalle d : detallesActuales) {
            String key = d.getItemTipo() + ":" + d.getItemId();
            BigDecimal fisico = stocksFisicos.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal merma = mermas.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal desperdicio = desperdicios.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal stockEsperado = d.getStockSistema().subtract(merma).subtract(desperdicio);
            d.setStockFisico(fisico);
            d.setMerma(merma);
            d.setDesperdicio(desperdicio);
            d.setDiferencia(stockEsperado.subtract(fisico));
            detalleRepository.save(d);
        }

        return inventario;
    }

    private List<InventarioFisicoDetalle> construirDetalles(Long inventarioId, Long empresaId,
                                                             Map<String, BigDecimal> stocksFisicos,
                                                             Map<String, BigDecimal> mermas,
                                                             Map<String, BigDecimal> desperdicios) {
        List<InventarioFisicoDetalle> detalles = new ArrayList<>();

        List<Ingrediente> ingredientes = ingredienteRepository
                .findAllByEmpresaIdAndActivoTrueAndConsumibleFalseOrderByNombreAsc(empresaId);
        for (Ingrediente ing : ingredientes) {
            String key = "INGREDIENTE:" + ing.getId();
            BigDecimal stockSis = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            BigDecimal fisico = stocksFisicos.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal merma = mermas.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal desperdicio = desperdicios.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal stockEsperado = stockSis.subtract(merma).subtract(desperdicio);
            detalles.add(InventarioFisicoDetalle.builder()
                    .inventarioFisicoId(inventarioId)
                    .itemTipo("INGREDIENTE")
                    .itemId(ing.getId())
                    .itemNombre(ing.getNombre())
                    .unidadMedida(ing.getUnidadMedida() != null ? ing.getUnidadMedida().name() : null)
                    .stockSistema(stockSis)
                    .stockFisico(fisico)
                    .diferencia(stockEsperado.subtract(fisico))
                    .merma(merma)
                    .desperdicio(desperdicio)
                    .build());
        }

        List<Producto> productos = productoRepository
                .findAllByEmpresaIdAndTieneRecetaFalseAndActivoTrueOrderByNombreAsc(empresaId);
        for (Producto p : productos) {
            String key = "PRODUCTO:" + p.getId();
            BigDecimal stockSis = p.getStockActual() != null ? p.getStockActual() : BigDecimal.ZERO;
            BigDecimal fisico = stocksFisicos.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal merma = mermas.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal desperdicio = desperdicios.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal stockEsperado = stockSis.subtract(merma).subtract(desperdicio);
            detalles.add(InventarioFisicoDetalle.builder()
                    .inventarioFisicoId(inventarioId)
                    .itemTipo("PRODUCTO")
                    .itemId(p.getId())
                    .itemNombre(p.getNombre())
                    .unidadMedida("UNIDAD")
                    .stockSistema(stockSis)
                    .stockFisico(fisico)
                    .diferencia(stockEsperado.subtract(fisico))
                    .merma(merma)
                    .desperdicio(desperdicio)
                    .build());
        }

        return detalles;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public InventarioFisico confirmar(Long id, Long usuarioId, Long empresaId) {
        InventarioFisico inventario = inventarioFisicoRepository.findByIdAndEmpresaIdAndActivoTrue(id, empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventario físico no encontrado: " + id));
        if (inventario.getEstado() != InventarioEstado.BORRADOR) {
            throw new BusinessException("El inventario ya está confirmado");
        }

        List<InventarioFisicoDetalle> detalles = detalleRepository
                .findByInventarioFisicoIdAndActivoTrue(inventario.getId());

        int ajustesAplicados = 0;
        for (InventarioFisicoDetalle d : detalles) {
            BigDecimal stockActual = d.getStockSistema();
            BigDecimal merma = d.getMerma() != null ? d.getMerma() : BigDecimal.ZERO;
            BigDecimal desperdicio = d.getDesperdicio() != null ? d.getDesperdicio() : BigDecimal.ZERO;

            // 1. Procesar merma: reducir stock
            if (merma.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal postMerma = stockActual.subtract(merma);
                MovimientoInventario movMerma = MovimientoInventario.builder()
                        .empresaId(empresaId)
                        .usuarioId(usuarioId)
                        .itemTipo(d.getItemTipo())
                        .itemId(d.getItemId())
                        .itemNombre(d.getItemNombre())
                        .movimientoTipo(MovimientoTipo.MERMA)
                        .referenciaId(inventario.getId())
                        .cantidad(merma)
                        .signo("-")
                        .stockAnterior(stockActual)
                        .stockPosterior(postMerma)
                        .fechaMovimiento(LocalDateTime.now())
                        .observacion("Merma inventario físico #" + inventario.getId())
                        .build();
                movimientoInventarioRepository.save(movMerma);
                stockActual = postMerma;
            }

            // 2. Procesar desperdicio: reducir stock
            if (desperdicio.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal postDesp = stockActual.subtract(desperdicio);
                MovimientoInventario movDesp = MovimientoInventario.builder()
                        .empresaId(empresaId)
                        .usuarioId(usuarioId)
                        .itemTipo(d.getItemTipo())
                        .itemId(d.getItemId())
                        .itemNombre(d.getItemNombre())
                        .movimientoTipo(MovimientoTipo.DESPERDICIO)
                        .referenciaId(inventario.getId())
                        .cantidad(desperdicio)
                        .signo("-")
                        .stockAnterior(stockActual)
                        .stockPosterior(postDesp)
                        .fechaMovimiento(LocalDateTime.now())
                        .observacion("Desperdicio inventario físico #" + inventario.getId())
                        .build();
                movimientoInventarioRepository.save(movDesp);
                stockActual = postDesp;
            }

            // 3. Calcular diferencia inexplicada
            BigDecimal inexplicada = stockActual.subtract(d.getStockFisico());
            d.setDiferencia(inexplicada);
            detalleRepository.save(d);

            if (inexplicada.compareTo(BigDecimal.ZERO) != 0) {
                String signo = inexplicada.compareTo(BigDecimal.ZERO) > 0 ? "-" : "+";
                BigDecimal cantidadAbs = inexplicada.abs();
                MovimientoInventario mov = MovimientoInventario.builder()
                        .empresaId(empresaId)
                        .usuarioId(usuarioId)
                        .itemTipo(d.getItemTipo())
                        .itemId(d.getItemId())
                        .itemNombre(d.getItemNombre())
                        .movimientoTipo(MovimientoTipo.AJUSTE_INVENTARIO)
                        .referenciaId(inventario.getId())
                        .cantidad(cantidadAbs)
                        .signo(signo)
                        .stockAnterior(stockActual)
                        .stockPosterior(d.getStockFisico())
                        .fechaMovimiento(LocalDateTime.now())
                        .observacion("Ajuste inexplicado inventario físico #" + inventario.getId())
                        .build();
                movimientoInventarioRepository.save(mov);
                ajustesAplicados++;
            }

            // 4. Actualizar stock final al físico contado
            actualizarStockItem(d.getItemTipo(), d.getItemId(), d.getStockFisico());
        }

        inventario.setEstado(InventarioEstado.CONFIRMADO);
        inventario.setFechaConfirmacion(LocalDateTime.now());
        inventario.setUsuarioConfirmacionId(usuarioId);
        return inventarioFisicoRepository.save(inventario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public int contarAjustes(Long inventarioId) {
        return (int) detalleRepository.findByInventarioFisicoIdAndActivoTrue(inventarioId).stream()
                .filter(d -> d.getDiferencia() != null && d.getDiferencia().compareTo(BigDecimal.ZERO) != 0)
                .count();
    }

    private void actualizarStockItem(String itemTipo, Long itemId, BigDecimal nuevoStock) {
        if ("INGREDIENTE".equals(itemTipo)) {
            Ingrediente ing = ingredienteRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + itemId));
            ing.setStockActual(nuevoStock);
            ingredienteRepository.save(ing);
        } else if ("PRODUCTO".equals(itemTipo)) {
            Producto p = productoRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + itemId));
            p.setStockActual(nuevoStock);
            productoRepository.save(p);
        }
    }
}
