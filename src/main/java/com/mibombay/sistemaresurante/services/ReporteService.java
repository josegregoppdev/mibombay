package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.ConsumoReporteDTO;
import com.mibombay.sistemaresurante.models.Ingrediente;
import com.mibombay.sistemaresurante.models.InventarioFisico;
import com.mibombay.sistemaresurante.models.InventarioFisicoDetalle;
import com.mibombay.sistemaresurante.models.Producto;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.repositories.IngredienteRepository;
import com.mibombay.sistemaresurante.repositories.InventarioFisicoDetalleRepository;
import com.mibombay.sistemaresurante.repositories.InventarioFisicoRepository;
import com.mibombay.sistemaresurante.repositories.MovimientoInventarioRepository;
import com.mibombay.sistemaresurante.repositories.ProductoRepository;
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
public class ReporteService {

    private final IngredienteRepository ingredienteRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final InventarioFisicoDetalleRepository inventarioDetalleRepository;
    private final InventarioFisicoRepository inventarioFisicoRepository;

    public ReporteService(IngredienteRepository ingredienteRepository,
                          ProductoRepository productoRepository,
                          MovimientoInventarioRepository movimientoRepository,
                          InventarioFisicoDetalleRepository inventarioDetalleRepository,
                          InventarioFisicoRepository inventarioFisicoRepository) {
        this.ingredienteRepository = ingredienteRepository;
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.inventarioDetalleRepository = inventarioDetalleRepository;
        this.inventarioFisicoRepository = inventarioFisicoRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ConsumoReporteDTO> generarReporteConsumo(LocalDate desde, LocalDate hasta, Long empresaId) {
        List<ConsumoReporteDTO> reporte = new ArrayList<>();

        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);

        List<Ingrediente> ingredientes = ingredienteRepository
                .findAllByEmpresaIdAndActivoTrueOrderByNombreAsc(empresaId);
        for (Ingrediente ing : ingredientes) {
            reporte.add(calcularItem("INGREDIENTE", ing.getId(), ing.getNombre(),
                    ing.getUnidadMedida() != null ? ing.getUnidadMedida().name() : null,
                    ing.getStockActual(),
                    ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO,
                    desde, hasta, desdeDt, hastaDt, empresaId));
        }

        List<Producto> productos = productoRepository
                .findAllByEmpresaIdAndTieneRecetaFalseAndActivoTrueOrderByNombreAsc(empresaId);
        for (Producto p : productos) {
            reporte.add(calcularItem("PRODUCTO", p.getId(), p.getNombre(),
                    "UNIDAD", p.getStockActual(),
                    p.getPrecioCompra() != null ? p.getPrecioCompra() : BigDecimal.ZERO,
                    desde, hasta, desdeDt, hastaDt, empresaId));
        }

        return reporte;
    }

    private ConsumoReporteDTO calcularItem(String itemTipo, Long itemId, String itemNombre,
                                            String unidadMedida, BigDecimal stockActual,
                                            BigDecimal precioCompra,
                                            LocalDate desde, LocalDate hasta,
                                            LocalDateTime desdeDt, LocalDateTime hastaDt,
                                            Long empresaId) {
        BigDecimal stockHasta = stockActual;
        List<InventarioFisicoDetalle> invHastaList = inventarioDetalleRepository
                .findUltimoConfirmadoByItemAntesDe(itemTipo, itemId, empresaId, hasta);
        if (!invHastaList.isEmpty()) {
            InventarioFisicoDetalle invHasta = invHastaList.get(0);
            BigDecimal stockFisico = invHasta.getStockFisico();
            Long invId = invHasta.getInventarioFisicoId();
            LocalDate fechaInv = inventarioFisicoRepository.findById(invId)
                    .map(InventarioFisico::getFecha).orElse(hasta);
            if (fechaInv.isBefore(hasta)) {
                BigDecimal movsPosterior = movimientoRepository.sumNetoCambioByItemBetween(
                        itemTipo, itemId, fechaInv.plusDays(1).atStartOfDay(), hastaDt);
                stockHasta = stockFisico.add(movsPosterior != null ? movsPosterior : BigDecimal.ZERO);
            } else {
                stockHasta = stockFisico;
            }
        }

        BigDecimal netoCambio = movimientoRepository.sumNetoCambioByItemBetween(itemTipo, itemId, desdeDt, hastaDt);
        BigDecimal stockDesde = stockHasta.subtract(netoCambio != null ? netoCambio : BigDecimal.ZERO);

        BigDecimal compras = movimientoRepository.sumCantidadByItemAndTipoBetween(
                itemTipo, itemId, MovimientoTipo.COMPRA, desdeDt, hastaDt);
        BigDecimal comprasAnulacion = movimientoRepository.sumCantidadByItemAndTipoBetween(
                itemTipo, itemId, MovimientoTipo.COMPRA_ANULACION, desdeDt, hastaDt);
        BigDecimal comprasNetas = (compras != null ? compras : BigDecimal.ZERO)
                .subtract(comprasAnulacion != null ? comprasAnulacion : BigDecimal.ZERO);

        BigDecimal consumo = movimientoRepository.sumCantidadByItemAndTipoBetween(
                itemTipo, itemId, MovimientoTipo.VENTA, desdeDt, hastaDt);

        BigDecimal merma = inventarioDetalleRepository.sumMermaByItemBetween(
                itemTipo, itemId, empresaId, desde, hasta);
        BigDecimal desperdicio = inventarioDetalleRepository.sumDesperdicioByItemBetween(
                itemTipo, itemId, empresaId, desde, hasta);

        BigDecimal costoMerma = merma.multiply(precioCompra);
        BigDecimal costoDesperdicio = desperdicio.multiply(precioCompra);

        BigDecimal diferencia = inventarioDetalleRepository.sumDiferenciaByItemBetween(
                itemTipo, itemId, empresaId, desde, hasta);
        BigDecimal costoDiferencia = diferencia.abs().multiply(precioCompra);

        BigDecimal stockReal = null;
        try {
            List<InventarioFisicoDetalle> ultimoList = inventarioDetalleRepository
                    .findUltimoConfirmadoByItem(itemTipo, itemId, empresaId);
            if (!ultimoList.isEmpty()) {
                stockReal = ultimoList.get(0).getStockFisico();
            }
        } catch (Exception ignored) {}

        return ConsumoReporteDTO.builder()
                .itemTipo(itemTipo)
                .itemId(itemId)
                .itemNombre(itemNombre)
                .unidadMedida(unidadMedida)
                .stockDesde(stockDesde)
                .compras(comprasNetas)
                .consumo(consumo != null ? consumo : BigDecimal.ZERO)
                .merma(merma != null ? merma : BigDecimal.ZERO)
                .desperdicio(desperdicio != null ? desperdicio : BigDecimal.ZERO)
                .costoMerma(costoMerma)
                .costoDesperdicio(costoDesperdicio)
                .diferencia(diferencia != null ? diferencia.negate() : BigDecimal.ZERO)
                .costoDiferencia(costoDiferencia)
                .stockHasta(stockHasta)
                .stockReal(stockReal)
                .build();
    }
}
