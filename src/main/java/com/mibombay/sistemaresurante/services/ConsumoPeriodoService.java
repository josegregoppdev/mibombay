package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.ConsumoPeriodoDTO;
import com.mibombay.sistemaresurante.DTO.ConsumoPeriodoFormDTO;
import com.mibombay.sistemaresurante.exceptions.BusinessException;
import com.mibombay.sistemaresurante.exceptions.ResourceNotFoundException;
import com.mibombay.sistemaresurante.models.*;
import com.mibombay.sistemaresurante.models.enums.ConsumoEstado;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.repositories.*;
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
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ConsumoPeriodoService {

    private final ConsumoPeriodoRepository repository;
    private final ConsumoPeriodoDetalleRepository detalleRepository;
    private final IngredienteRepository ingredienteRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public ConsumoPeriodoService(ConsumoPeriodoRepository repository,
                                 ConsumoPeriodoDetalleRepository detalleRepository,
                                 IngredienteRepository ingredienteRepository,
                                 MovimientoInventarioRepository movimientoRepository) {
        this.repository = repository;
        this.detalleRepository = detalleRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<ConsumoPeriodoDTO> listar(Long empresaId) {
        return repository.findByEmpresaIdAndActivoTrueOrderByFechaFinDescIdDesc(empresaId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ConsumoPeriodo obtenerPorId(Long id) {
        Long empresaId = TenantContext.getEmpresaId();
        return repository.findById(id)
                .filter(c -> c.getEmpresaId().equals(empresaId) && Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Período de consumo no encontrado: " + id));
    }

    public List<ConsumoPeriodoDetalle> obtenerDetalles(Long periodoId) {
        return detalleRepository.findByConsumoPeriodoIdAndActivoTrueOrderByItemNombreAsc(periodoId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ConsumoPeriodo crearBorrador(String tipo, Long usuarioId, Long empresaId) {
        if (repository.existsByEmpresaIdAndTipoAndEstadoAndActivoTrue(empresaId, tipo, ConsumoEstado.CONFIRMADO)) {
            throw new BusinessException("Ya existe un período " + tipo + " confirmado.");
        }

        LocalDate hoy = LocalDate.now();
        LocalDate inicio, fin;
        if ("SEMANAL".equals(tipo)) {
            inicio = hoy.minusDays(6);
            fin = hoy;
        } else {
            inicio = hoy.minusDays(29);
            fin = hoy;
        }

        ConsumoPeriodo periodo = ConsumoPeriodo.builder()
                .empresaId(empresaId)
                .usuarioId(usuarioId)
                .tipo(tipo)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .estado(ConsumoEstado.BORRADOR)
                .build();
        periodo = repository.save(periodo);

        List<Ingrediente> consumibles = ingredienteRepository
                .findAllByEmpresaIdAndActivoTrueAndConsumibleTrueOrderByNombreAsc(empresaId);
        List<ConsumoPeriodoDetalle> detalles = new ArrayList<>();
        for (Ingrediente ing : consumibles) {
            BigDecimal stockActual = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            detalles.add(ConsumoPeriodoDetalle.builder()
                    .consumoPeriodoId(periodo.getId())
                    .itemTipo("INGREDIENTE")
                    .itemId(ing.getId())
                    .itemNombre(ing.getNombre())
                    .stockSistema(stockActual)
                    .stockFinal(stockActual)
                    .merma(BigDecimal.ZERO)
                    .desperdicio(BigDecimal.ZERO)
                    .build());
        }
        detalleRepository.saveAll(detalles);
        return periodo;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ConsumoPeriodo actualizarBorrador(Long id, List<ConsumoPeriodoFormDTO.ItemConsumo> items, Long empresaId) {
        ConsumoPeriodo periodo = repository.findById(id)
                .filter(c -> c.getEmpresaId().equals(empresaId) && Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Período de consumo no encontrado: " + id));
        if (periodo.getEstado() != ConsumoEstado.BORRADOR) {
            throw new BusinessException("No se puede modificar un período confirmado");
        }

        Map<String, ConsumoPeriodoFormDTO.ItemConsumo> itemsMap = items.stream()
                .collect(Collectors.toMap(
                        i -> i.getItemTipo() + ":" + i.getItemId(),
                        i -> i
                ));

        List<ConsumoPeriodoDetalle> detalles = detalleRepository
                .findByConsumoPeriodoIdAndActivoTrue(periodo.getId());
        for (ConsumoPeriodoDetalle d : detalles) {
            String key = d.getItemTipo() + ":" + d.getItemId();
            ConsumoPeriodoFormDTO.ItemConsumo item = itemsMap.get(key);
            if (item != null) {
                d.setStockFinal(item.getStockFinal() != null ? item.getStockFinal() : BigDecimal.ZERO);
                d.setMerma(item.getMerma() != null ? item.getMerma() : BigDecimal.ZERO);
                d.setDesperdicio(item.getDesperdicio() != null ? item.getDesperdicio() : BigDecimal.ZERO);
                detalleRepository.save(d);
            }
        }
        return periodo;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ConsumoPeriodo confirmar(Long id, Long usuarioId, Long empresaId) {
        ConsumoPeriodo periodo = repository.findById(id)
                .filter(c -> c.getEmpresaId().equals(empresaId) && Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Período de consumo no encontrado: " + id));
        if (periodo.getEstado() != ConsumoEstado.BORRADOR) {
            throw new BusinessException("El período ya está confirmado");
        }

        List<ConsumoPeriodoDetalle> detalles = detalleRepository
                .findByConsumoPeriodoIdAndActivoTrue(periodo.getId());
        LocalDateTime ahora = LocalDateTime.now();

        for (ConsumoPeriodoDetalle d : detalles) {
            Ingrediente ing = ingredienteRepository.findById(d.getItemId()).orElse(null);
            if (ing == null) continue;

            BigDecimal stockAntes = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            BigDecimal merma = d.getMerma() != null ? d.getMerma() : BigDecimal.ZERO;
            BigDecimal desperdicio = d.getDesperdicio() != null ? d.getDesperdicio() : BigDecimal.ZERO;
            BigDecimal stockFinal = d.getStockFinal() != null ? d.getStockFinal() : BigDecimal.ZERO;

            BigDecimal consumido = stockAntes.subtract(stockFinal).subtract(merma).subtract(desperdicio);
            if (consumido.compareTo(BigDecimal.ZERO) < 0) consumido = BigDecimal.ZERO;

            String obs = periodo.getTipo().toLowerCase() + ": " + periodo.getFechaInicio() + " a " + periodo.getFechaFin();

            if (consumido.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal stockPostConsumo = stockAntes.subtract(consumido);
                if (stockPostConsumo.compareTo(BigDecimal.ZERO) < 0) stockPostConsumo = BigDecimal.ZERO;
                MovimientoInventario mov = MovimientoInventario.builder()
                        .empresaId(empresaId).usuarioId(usuarioId)
                        .itemTipo(d.getItemTipo()).itemId(d.getItemId()).itemNombre(d.getItemNombre())
                        .movimientoTipo(MovimientoTipo.CONSUMO)
                        .referenciaId(periodo.getId())
                        .cantidad(consumido).signo("-")
                        .stockAnterior(stockAntes)
                        .stockPosterior(stockPostConsumo)
                        .fechaMovimiento(ahora).observacion("Consumo " + obs)
                        .build();
                movimientoRepository.save(mov);
                stockAntes = stockPostConsumo;
            }

            if (merma.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal stockPostMerma = stockAntes.subtract(merma);
                if (stockPostMerma.compareTo(BigDecimal.ZERO) < 0) stockPostMerma = BigDecimal.ZERO;
                MovimientoInventario mov = MovimientoInventario.builder()
                        .empresaId(empresaId).usuarioId(usuarioId)
                        .itemTipo(d.getItemTipo()).itemId(d.getItemId()).itemNombre(d.getItemNombre())
                        .movimientoTipo(MovimientoTipo.MERMA)
                        .referenciaId(periodo.getId())
                        .cantidad(merma).signo("-")
                        .stockAnterior(stockAntes)
                        .stockPosterior(stockPostMerma)
                        .fechaMovimiento(ahora).observacion("Merma " + obs)
                        .build();
                movimientoRepository.save(mov);
                stockAntes = stockPostMerma;
            }

            if (desperdicio.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal stockPostDesp = stockAntes.subtract(desperdicio);
                if (stockPostDesp.compareTo(BigDecimal.ZERO) < 0) stockPostDesp = BigDecimal.ZERO;
                MovimientoInventario mov = MovimientoInventario.builder()
                        .empresaId(empresaId).usuarioId(usuarioId)
                        .itemTipo(d.getItemTipo()).itemId(d.getItemId()).itemNombre(d.getItemNombre())
                        .movimientoTipo(MovimientoTipo.DESPERDICIO)
                        .referenciaId(periodo.getId())
                        .cantidad(desperdicio).signo("-")
                        .stockAnterior(stockAntes)
                        .stockPosterior(stockPostDesp)
                        .fechaMovimiento(ahora).observacion("Desperdicio " + obs)
                        .build();
                movimientoRepository.save(mov);
                stockAntes = stockPostDesp;
            }

            ing.setStockActual(stockFinal);
            ingredienteRepository.save(ing);
        }

        periodo.setEstado(ConsumoEstado.CONFIRMADO);
        return repository.save(periodo);
    }

    private BigDecimal val(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private ConsumoPeriodoDTO toDTO(ConsumoPeriodo cp) {
        List<ConsumoPeriodoDetalle> detalles = detalleRepository
                .findByConsumoPeriodoIdAndActivoTrue(cp.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (ConsumoPeriodoDetalle d : detalles) {
            BigDecimal consumido = val(d.getStockSistema()).subtract(val(d.getStockFinal()))
                    .subtract(val(d.getMerma())).subtract(val(d.getDesperdicio()));
            if (consumido.compareTo(BigDecimal.ZERO) > 0) total = total.add(consumido);
        }
        return ConsumoPeriodoDTO.builder()
                .id(cp.getId())
                .tipo(cp.getTipo())
                .fechaInicio(cp.getFechaInicio())
                .fechaFin(cp.getFechaFin())
                .estado(cp.getEstado().name())
                .totalItems(detalles.size())
                .totalConsumido(total)
                .build();
    }
}
