package com.mibombay.sistemaresurante.services;

import com.mibombay.sistemaresurante.DTO.*;
import com.mibombay.sistemaresurante.models.*;
import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import com.mibombay.sistemaresurante.repositories.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FoodCostService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final InventarioFisicoDetalleRepository inventarioDetalleRepository;
    private final CostoComidaDiariaRepository costoCDiariaRepository;
    private final CostoComidaDiariaItemRepository costoCDiariaItemRepository;

    public FoodCostService(VentaRepository ventaRepository,
                           ProductoRepository productoRepository,
                           IngredienteRepository ingredienteRepository,
                           MovimientoInventarioRepository movimientoRepository,
                           InventarioFisicoDetalleRepository inventarioDetalleRepository,
                           CostoComidaDiariaRepository costoCdiariaRepository,
                           CostoComidaDiariaItemRepository costoCdiariaItemRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.movimientoRepository = movimientoRepository;
        this.inventarioDetalleRepository = inventarioDetalleRepository;
        this.costoCDiariaRepository = costoCdiariaRepository;
        this.costoCDiariaItemRepository = costoCdiariaItemRepository;
    }

    // ============================================================
    // 1. MÉTODOS PÚBLICOS DE CÁLCULO (read-only)
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    public FoodCostDiarioDTO calcularDiario(LocalDate fecha, Long empresaId) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(LocalTime.MAX);

        List<Venta> ventas = ventaRepository
                .findByEmpresaIdAndFechaVentaBetweenAndActivoTrueOrderByFechaVentaDesc(empresaId, desde, hasta);

        BigDecimal ventasTotales = ventas.stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoIngredientesVendidos = calcularCostoVentasFromMovimientos(desde, hasta, empresaId);

        BigDecimal foodCostPct = calcularPorcentaje(costoIngredientesVendidos, ventasTotales);

        List<Ingrediente> ingredientes = ingredienteRepository
                .findAllByEmpresaIdAndActivoTrueAndConsumibleFalseOrderByNombreAsc(empresaId);
        List<Producto> productos = productoRepository
                .findAllByEmpresaIdAndTieneRecetaFalseAndActivoTrueOrderByNombreAsc(empresaId);

        BigDecimal invInicialValor = BigDecimal.ZERO;
        BigDecimal invFinalValor = BigDecimal.ZERO;
        BigDecimal comprasValor = BigDecimal.ZERO;
        BigDecimal mermaValor = BigDecimal.ZERO;
        BigDecimal desperdicioValor = BigDecimal.ZERO;
        BigDecimal diferenciaInexplicadaValor = BigDecimal.ZERO;

        for (Ingrediente ing : ingredientes) {
            BigDecimal precio = ing.getPrecioCompra() != null ? ing.getPrecioCompra() : BigDecimal.ZERO;
            String tipo = "INGREDIENTE";
            Long id = ing.getId();

            BigDecimal netoCambio = netoCambio(tipo, id, desde, hasta);
            BigDecimal stockHasta = ing.getStockActual() != null ? ing.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockDesde = stockHasta.subtract(netoCambio);

            invInicialValor = invInicialValor.add(stockDesde.multiply(precio));
            invFinalValor = invFinalValor.add(stockHasta.multiply(precio));

            BigDecimal comprasNetas = comprasNetasMovimientos(tipo, id, desde, hasta);
            comprasValor = comprasValor.add(comprasNetas.multiply(precio));

            BigDecimal m = inventarioDetalleRepository.sumMermaByItemBetween(tipo, id, empresaId, fecha, fecha);
            BigDecimal d = inventarioDetalleRepository.sumDesperdicioByItemBetween(tipo, id, empresaId, fecha, fecha);
            mermaValor = mermaValor.add(val(m).multiply(precio));
            desperdicioValor = desperdicioValor.add(val(d).multiply(precio));
            diferenciaInexplicadaValor = diferenciaInexplicadaValor.add(
                    val(inventarioDetalleRepository.sumDiferenciaByItemBetween(tipo, id, empresaId, fecha, fecha)).multiply(precio));
        }

        for (Producto p : productos) {
            BigDecimal precio = p.getPrecioCompra() != null ? p.getPrecioCompra() : BigDecimal.ZERO;
            String tipo = "PRODUCTO";
            Long id = p.getId();

            BigDecimal netoCambio = netoCambio(tipo, id, desde, hasta);
            BigDecimal stockHasta = p.getStockActual() != null ? p.getStockActual() : BigDecimal.ZERO;
            BigDecimal stockDesde = stockHasta.subtract(netoCambio);

            invInicialValor = invInicialValor.add(stockDesde.multiply(precio));
            invFinalValor = invFinalValor.add(stockHasta.multiply(precio));

            BigDecimal comprasNetas = comprasNetasMovimientos(tipo, id, desde, hasta);
            comprasValor = comprasValor.add(comprasNetas.multiply(precio));

            BigDecimal m = inventarioDetalleRepository.sumMermaByItemBetween(tipo, id, empresaId, fecha, fecha);
            BigDecimal d = inventarioDetalleRepository.sumDesperdicioByItemBetween(tipo, id, empresaId, fecha, fecha);
            mermaValor = mermaValor.add(val(m).multiply(precio));
            desperdicioValor = desperdicioValor.add(val(d).multiply(precio));
            diferenciaInexplicadaValor = diferenciaInexplicadaValor.add(
                    val(inventarioDetalleRepository.sumDiferenciaByItemBetween(tipo, id, empresaId, fecha, fecha)).multiply(precio));
        }

        BigDecimal consumoIndirectoValor = calcularConsumoIndirectoValor(desde, hasta);
        costoIngredientesVendidos = costoIngredientesVendidos.add(consumoIndirectoValor);
        foodCostPct = calcularPorcentaje(costoIngredientesVendidos, ventasTotales);

        BigDecimal costoAlimentosContable = costoIngredientesVendidos.add(mermaValor).add(desperdicioValor);
        BigDecimal costoRealValor = costoAlimentosContable.add(diferenciaInexplicadaValor);
        BigDecimal foodCostContablePct = calcularPorcentaje(costoAlimentosContable, ventasTotales);
        BigDecimal costoRealPct = calcularPorcentaje(costoRealValor, ventasTotales);
        BigDecimal diferenciaContable = costoRealValor.subtract(costoAlimentosContable);

        BigDecimal mermaPct = calcularPorcentaje(mermaValor, ventasTotales);
        BigDecimal desperdicioPct = calcularPorcentaje(desperdicioValor, ventasTotales);
        BigDecimal diferenciaInvPct = calcularPorcentaje(diferenciaContable, ventasTotales);

        FoodCostResumenDTO resumen = FoodCostResumenDTO.builder()
                .fecha(fecha)
                .ventasTotales(ventasTotales)
                .costoIngredientesVendidos(costoIngredientesVendidos)
                .foodCostPorcentaje(foodCostPct)
                .inventarioInicialValor(invInicialValor)
                .comprasValor(comprasValor)
                .inventarioFinalValor(invFinalValor)
                .costoAlimentosContable(costoAlimentosContable)
                .foodCostContablePorcentaje(foodCostContablePct)
                .mermaValor(mermaValor)
                .desperdicioValor(desperdicioValor)
                .diferenciaContable(diferenciaContable)
                .mermaPorcentaje(mermaPct)
                .desperdicioPorcentaje(desperdicioPct)
                .diferenciaInventarioPorcentaje(diferenciaInvPct)
                .consumoIndirectoValor(consumoIndirectoValor)
                .costoRealValor(costoRealValor)
                .costoRealPorcentaje(costoRealPct)
                .build();

        List<FoodCostItemDTO> itemsDTO = calcularPorItemFromMovimientos(desde, hasta, empresaId);

        return FoodCostDiarioDTO.builder()
                .resumen(resumen)
                .items(itemsDTO)
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<FoodCostItemDTO> calcularPorItem(LocalDate fecha, Long empresaId) {
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.atTime(LocalTime.MAX);
        return calcularPorItemFromMovimientos(desde, hasta, empresaId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<FoodCostItemDTO> calcularPorItemFromMovimientos(LocalDateTime desde, LocalDateTime hasta, Long empresaId) {
        Map<String, FoodCostItemDTO> itemMap = new LinkedHashMap<>();

        List<MovimientoInventario> movimientos = movimientoRepository
                .findByEmpresaIdAndMovimientoTipoInAndFechaMovimientoBetweenOrderByItemTipoItemId(
                        empresaId, List.of(MovimientoTipo.VENTA, MovimientoTipo.VENTA_ANULACION), desde, hasta);

        Map<String, BigDecimal> precioCache = new HashMap<>();

        for (MovimientoInventario mov : movimientos) {
            String itemTipo = mov.getItemTipo();
            Long itemId = mov.getItemId();
            String key = itemTipo + ":" + itemId;
            BigDecimal cantidad = val(mov.getCantidad());

            BigDecimal precioUnitario = precioCache.computeIfAbsent(key, k -> {
                if ("INGREDIENTE".equals(itemTipo)) {
                    return ingredienteRepository.findById(itemId)
                            .map(i -> val(i.getPrecioCompra()))
                            .orElse(BigDecimal.ZERO);
                } else if ("PRODUCTO".equals(itemTipo)) {
                    return productoRepository.findById(itemId)
                            .map(p -> val(p.getPrecioCompra()))
                            .orElse(BigDecimal.ZERO);
                }
                return BigDecimal.ZERO;
            });

            String tipoItem = "INGREDIENTE".equals(itemTipo) ? "INGREDIENTE" : "PRODUCTO";
            String unidadMedida = "INGREDIENTE".equals(itemTipo) ? "" : "UNIDAD";

            String displayTipo = tipoItem;
            String nombreItem = mov.getItemNombre() != null ? mov.getItemNombre() : "";
            String unidad = unidadMedida;

            if (MovimientoTipo.VENTA_ANULACION.equals(mov.getMovimientoTipo())) {
                cantidad = cantidad.negate();
            }

            if (!itemMap.containsKey(key)) {
                if ("INGREDIENTE".equals(itemTipo)) {
                    Ingrediente ing = ingredienteRepository.findById(itemId).orElse(null);
                    if (ing != null) {
                        unidad = ing.getUnidadMedida().name();
                        if (nombreItem.isEmpty()) nombreItem = ing.getNombre();
                    }
                } else {
                    Producto prod = productoRepository.findById(itemId).orElse(null);
                    if (prod != null && nombreItem.isEmpty()) nombreItem = prod.getNombre();
                }

                itemMap.put(key, FoodCostItemDTO.builder()
                        .itemId(itemId)
                        .itemNombre(nombreItem)
                        .itemTipo(displayTipo)
                        .unidadMedida(unidad)
                        .cantidadConsumida(cantidad)
                        .precioCostoUnitario(precioUnitario)
                        .costoGrupo(cantidad.multiply(precioUnitario))
                        .build());
            } else {
                FoodCostItemDTO existing = itemMap.get(key);
                existing.setCantidadConsumida(existing.getCantidadConsumida().add(cantidad));
                existing.setCostoGrupo(existing.getCostoGrupo().add(cantidad.multiply(precioUnitario)));
            }
        }

        // Consumibles: movimientos CONSUMO del día (consumo indirecto)
        List<MovimientoInventario> consumos = movimientoRepository
                .findByMovimientoTipoAndFechaMovimientoBetweenOrderByFechaMovimientoAsc(MovimientoTipo.CONSUMO, desde, hasta);
        for (MovimientoInventario mov : consumos) {
            if (mov.getEmpresaId() != null && !mov.getEmpresaId().equals(empresaId)) continue;
            if (!"INGREDIENTE".equals(mov.getItemTipo())) continue;
            Ingrediente ing = ingredienteRepository.findById(mov.getItemId()).orElse(null);
            if (ing == null) continue;

            String key = "CONSUMIBLE:" + mov.getItemId();
            BigDecimal cantidad = val(mov.getCantidad());
            BigDecimal precioUnitario = val(ing.getPrecioCompra());
            BigDecimal costoGrupo = cantidad.multiply(precioUnitario);

            if (!itemMap.containsKey(key)) {
                itemMap.put(key, FoodCostItemDTO.builder()
                        .itemId(mov.getItemId())
                        .itemNombre(mov.getItemNombre() != null ? mov.getItemNombre() : ing.getNombre())
                        .itemTipo("CONSUMIBLE")
                        .unidadMedida(ing.getUnidadMedida().name())
                        .cantidadConsumida(cantidad)
                        .precioCostoUnitario(precioUnitario)
                        .costoGrupo(costoGrupo)
                        .build());
            } else {
                FoodCostItemDTO existing = itemMap.get(key);
                existing.setCantidadConsumida(existing.getCantidadConsumida().add(cantidad));
                existing.setCostoGrupo(existing.getCostoGrupo().add(costoGrupo));
            }
        }

        BigDecimal ventasTotales = ventaRepository
                .findByEmpresaIdAndFechaVentaBetweenAndActivoTrueOrderByFechaVentaDesc(empresaId, desde, hasta)
                .stream()
                .map(Venta::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FoodCostItemDTO> resultado = new ArrayList<>();
        for (FoodCostItemDTO item : itemMap.values()) {
            BigDecimal pct = calcularPorcentaje(item.getCostoGrupo(), ventasTotales);
            resultado.add(FoodCostItemDTO.builder()
                    .itemId(item.getItemId())
                    .itemNombre(item.getItemNombre())
                    .itemTipo(item.getItemTipo())
                    .unidadMedida(item.getUnidadMedida())
                    .cantidadConsumida(item.getCantidadConsumida())
                    .precioCostoUnitario(item.getPrecioCostoUnitario())
                    .costoGrupo(item.getCostoGrupo())
                    .porcentajeDelCosto(pct)
                    .build());
        }

        resultado.sort((a, b) -> b.getCostoGrupo().compareTo(a.getCostoGrupo()));
        return resultado;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ConsumoResumenDTO calcularResumenConsumo(LocalDate desde, LocalDate hasta, Long empresaId) {
        List<CostoComidaDiaria> registros = obtenerCostoComidaPorRango(empresaId, desde, hasta);

        if (registros.isEmpty()) {
            return ConsumoResumenDTO.builder()
                    .desde(desde)
                    .hasta(hasta)
                    .ventasTotales(BigDecimal.ZERO)
                    .costoIngredientesVendidos(BigDecimal.ZERO)
                    .mermaValor(BigDecimal.ZERO)
                    .desperdicioValor(BigDecimal.ZERO)
                    .diferenciaValor(BigDecimal.ZERO)
                    .foodCostPorcentaje(BigDecimal.ZERO)
                    .mermaPorcentaje(BigDecimal.ZERO)
                    .desperdicioPorcentaje(BigDecimal.ZERO)
                    .diferenciaPorcentaje(BigDecimal.ZERO)
                    .costoRealValor(BigDecimal.ZERO)
                    .costoRealPorcentaje(BigDecimal.ZERO)
                    .costoAlimentosContable(BigDecimal.ZERO)
                    .foodCostContablePorcentaje(BigDecimal.ZERO)
                    .esPromedio(false)
                    .diasConDatos(0)
                    .totalDias(0)
                    .build();
        }

        BigDecimal sumVentas = BigDecimal.ZERO;
        BigDecimal sumCostos = BigDecimal.ZERO;
        BigDecimal sumMerma = BigDecimal.ZERO;
        BigDecimal sumDesperdicio = BigDecimal.ZERO;
        BigDecimal sumDiferencia = BigDecimal.ZERO;
        BigDecimal sumFoodCostPct = BigDecimal.ZERO;
        BigDecimal sumMermaPct = BigDecimal.ZERO;
        BigDecimal sumDespPct = BigDecimal.ZERO;
        BigDecimal sumDifPct = BigDecimal.ZERO;
        BigDecimal sumCostoReal = BigDecimal.ZERO;
        BigDecimal sumCostoContable = BigDecimal.ZERO;
        BigDecimal sumCostoRealPct = BigDecimal.ZERO;
        BigDecimal sumCostoContablePct = BigDecimal.ZERO;

        int n = registros.size();

        for (CostoComidaDiaria r : registros) {
            sumVentas = sumVentas.add(val(r.getVentasTotales()));
            sumCostos = sumCostos.add(val(r.getCostoIngredientesVendidos()));
            sumMerma = sumMerma.add(val(r.getMermaValor()));
            sumDesperdicio = sumDesperdicio.add(val(r.getDesperdicioValor()));
            sumDiferencia = sumDiferencia.add(val(r.getDiferenciaValor()));
            sumFoodCostPct = sumFoodCostPct.add(val(r.getFoodCostPorcentaje()));
            sumMermaPct = sumMermaPct.add(val(r.getMermaPorcentaje()));
            sumDespPct = sumDespPct.add(val(r.getDesperdicioPorcentaje()));
            sumDifPct = sumDifPct.add(val(r.getDiferenciaPorcentaje()));
            sumCostoReal = sumCostoReal.add(val(r.getCostoRealValor()));
            sumCostoContable = sumCostoContable.add(val(r.getCostoAlimentosContable()));
            sumCostoRealPct = sumCostoRealPct.add(val(r.getCostoRealPorcentaje()));
            sumCostoContablePct = sumCostoContablePct.add(val(r.getFoodCostContablePorcentaje()));
        }

        boolean esMultiDia = n > 1;

        BigDecimal foodCostPct;
        BigDecimal mermaPct;
        BigDecimal despPct;
        BigDecimal difPct;
        BigDecimal costoRealPct;
        BigDecimal costoContablePct;

        if (esMultiDia && sumVentas.compareTo(BigDecimal.ZERO) > 0) {
            foodCostPct = calcularPorcentaje(sumCostos, sumVentas);
            mermaPct = calcularPorcentaje(sumMerma, sumVentas);
            despPct = calcularPorcentaje(sumDesperdicio, sumVentas);
            difPct = calcularPorcentaje(sumDiferencia, sumVentas);
            costoRealPct = calcularPorcentaje(sumCostoReal, sumVentas);
            costoContablePct = calcularPorcentaje(sumCostoContable, sumVentas);
        } else if (n == 1) {
            CostoComidaDiaria r = registros.get(0);
            foodCostPct = r.getFoodCostPorcentaje();
            mermaPct = r.getMermaPorcentaje();
            despPct = r.getDesperdicioPorcentaje();
            difPct = r.getDiferenciaPorcentaje();
            costoRealPct = r.getCostoRealPorcentaje();
            costoContablePct = r.getFoodCostContablePorcentaje();
        } else {
            foodCostPct = BigDecimal.ZERO;
            mermaPct = BigDecimal.ZERO;
            despPct = BigDecimal.ZERO;
            difPct = BigDecimal.ZERO;
            costoRealPct = BigDecimal.ZERO;
            costoContablePct = BigDecimal.ZERO;
        }

        return ConsumoResumenDTO.builder()
                .desde(desde)
                .hasta(hasta)
                .ventasTotales(sumVentas)
                .costoIngredientesVendidos(sumCostos)
                .mermaValor(sumMerma)
                .desperdicioValor(sumDesperdicio)
                .diferenciaValor(sumDiferencia)
                .foodCostPorcentaje(foodCostPct)
                .mermaPorcentaje(mermaPct)
                .desperdicioPorcentaje(despPct)
                .diferenciaPorcentaje(difPct)
                .costoRealValor(sumCostoReal)
                .costoRealPorcentaje(costoRealPct)
                .costoAlimentosContable(sumCostoContable)
                .foodCostContablePorcentaje(costoContablePct)
                .esPromedio(esMultiDia)
                .diasConDatos(n)
                .totalDias(n)
                .build();
    }

    // ============================================================
    // 2. MÉTODOS PÚBLICOS DE PERSISTENCIA (write)
    // ============================================================

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CostoComidaDiaria guardarCostoComidaDiaria(LocalDate fecha, Long empresaId) {
        FoodCostDiarioDTO diario = calcularDiario(fecha, empresaId);
        FoodCostResumenDTO r = diario.getResumen();

        CostoComidaDiaria entidad = costoCDiariaRepository
                .findByEmpresaIdAndFechaAndActivoTrue(empresaId, fecha)
                .orElse(CostoComidaDiaria.builder()
                        .empresaId(empresaId)
                        .fecha(fecha)
                        .items(new ArrayList<>())
                        .build());

        entidad.setVentasTotales(val(r.getVentasTotales()));
        entidad.setCostoIngredientesVendidos(val(r.getCostoIngredientesVendidos()));
        entidad.setFoodCostPorcentaje(val(r.getFoodCostPorcentaje()));
        entidad.setInventarioInicialValor(val(r.getInventarioInicialValor()));
        entidad.setComprasValor(val(r.getComprasValor()));
        entidad.setInventarioFinalValor(val(r.getInventarioFinalValor()));
        entidad.setCostoAlimentosContable(val(r.getCostoAlimentosContable()));
        entidad.setFoodCostContablePorcentaje(val(r.getFoodCostContablePorcentaje()));
        entidad.setMermaValor(val(r.getMermaValor()));
        entidad.setMermaPorcentaje(val(r.getMermaPorcentaje()));
        entidad.setDesperdicioValor(val(r.getDesperdicioValor()));
        entidad.setDesperdicioPorcentaje(val(r.getDesperdicioPorcentaje()));
        entidad.setDiferenciaValor(val(r.getDiferenciaContable()));
        entidad.setDiferenciaPorcentaje(val(r.getDiferenciaInventarioPorcentaje()));
        entidad.setConsumoIndirectoValor(val(r.getConsumoIndirectoValor()));
        entidad.setCostoRealValor(val(r.getCostoRealValor()));
        entidad.setCostoRealPorcentaje(val(r.getCostoRealPorcentaje()));

        // Persistir items
        entidad.clearItems();
        for (FoodCostItemDTO itemDTO : diario.getItems()) {
            CostoComidaDiariaItem item = CostoComidaDiariaItem.builder()
                    .itemId(itemDTO.getItemId())
                    .itemNombre(itemDTO.getItemNombre())
                    .itemTipo(itemDTO.getItemTipo())
                    .unidadMedida(itemDTO.getUnidadMedida())
                    .cantidadConsumida(val(itemDTO.getCantidadConsumida()))
                    .precioCostoUnitario(val(itemDTO.getPrecioCostoUnitario()))
                    .costoGrupo(val(itemDTO.getCostoGrupo()))
                    .porcentajeDelCosto(val(itemDTO.getPorcentajeDelCosto()))
                    .build();
            entidad.addItem(item);
        }

        return costoCDiariaRepository.save(entidad);
    }

    // ============================================================
    // 3. MÉTODOS PÚBLICOS DE CONSULTA (read-only)
    // ============================================================

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<CostoComidaDiaria> obtenerCostoComidaDiaria(Long empresaId, LocalDate fecha) {
        return costoCDiariaRepository.findByEmpresaIdAndFechaAndActivoTrue(empresaId, fecha);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<FoodCostItemDTO> obtenerItemsGuardados(Long costoComidaDiariaId) {
        return costoCDiariaItemRepository.findByCostoComidaDiariaIdAndActivoTrue(costoComidaDiariaId)
                .stream()
                .map(item -> FoodCostItemDTO.builder()
                        .itemId(item.getItemId())
                        .itemNombre(item.getItemNombre())
                        .itemTipo(item.getItemTipo())
                        .unidadMedida(item.getUnidadMedida())
                        .cantidadConsumida(val(item.getCantidadConsumida()))
                        .precioCostoUnitario(val(item.getPrecioCostoUnitario()))
                        .costoGrupo(val(item.getCostoGrupo()))
                        .porcentajeDelCosto(val(item.getPorcentajeDelCosto()))
                        .build())
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<CostoComidaDiaria> obtenerCostoComidaPorRango(Long empresaId, LocalDate desde, LocalDate hasta) {
        return costoCDiariaRepository.findByEmpresaIdAndFechaBetweenAndActivoTrueOrderByFechaAsc(empresaId, desde, hasta);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean existeCostoGuardado(Long empresaId, LocalDate fecha) {
        return costoCDiariaRepository.existsByEmpresaIdAndFechaAndActivoTrue(empresaId, fecha);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean existeDiaSiguienteGuardado(Long empresaId, LocalDate fecha) {
        return costoCDiariaRepository.existsByEmpresaIdAndFechaAndActivoTrue(empresaId, fecha.plusDays(1));
    }

    // ============================================================
    // 4. MÉTODOS PRIVADOS DE CÁLCULO (helpers)
    // ============================================================

    private BigDecimal calcularCostoVentasFromMovimientos(LocalDateTime desde, LocalDateTime hasta, Long empresaId) {
        BigDecimal total = BigDecimal.ZERO;

        List<Ingrediente> ingredientes = ingredienteRepository
                .findAllByEmpresaIdAndActivoTrueAndConsumibleFalseOrderByNombreAsc(empresaId);
        for (Ingrediente ing : ingredientes) {
            BigDecimal vendido = movimientoRepository.sumCantidadByItemAndTipoBetween(
                    "INGREDIENTE", ing.getId(), MovimientoTipo.VENTA, desde, hasta);
            BigDecimal anulado = movimientoRepository.sumCantidadByItemAndTipoBetween(
                    "INGREDIENTE", ing.getId(), MovimientoTipo.VENTA_ANULACION, desde, hasta);
            BigDecimal neto = val(vendido).subtract(val(anulado));
            total = total.add(neto.multiply(val(ing.getPrecioCompra())));
        }

        List<Producto> productos = productoRepository
                .findAllByEmpresaIdAndTieneRecetaFalseAndActivoTrueOrderByNombreAsc(empresaId);
        for (Producto p : productos) {
            BigDecimal vendido = movimientoRepository.sumCantidadByItemAndTipoBetween(
                    "PRODUCTO", p.getId(), MovimientoTipo.VENTA, desde, hasta);
            BigDecimal anulado = movimientoRepository.sumCantidadByItemAndTipoBetween(
                    "PRODUCTO", p.getId(), MovimientoTipo.VENTA_ANULACION, desde, hasta);
            BigDecimal neto = val(vendido).subtract(val(anulado));
            total = total.add(neto.multiply(val(p.getPrecioCompra())));
        }

        return total;
    }

    private BigDecimal netoCambio(String itemTipo, Long itemId, LocalDateTime desde, LocalDateTime hasta) {
        BigDecimal neto = movimientoRepository.sumNetoCambioByItemBetween(itemTipo, itemId, desde, hasta);
        return neto != null ? neto : BigDecimal.ZERO;
    }

    private BigDecimal comprasNetasMovimientos(String itemTipo, Long itemId, LocalDateTime desde, LocalDateTime hasta) {
        BigDecimal c = movimientoRepository.sumCantidadByItemAndTipoBetween(itemTipo, itemId, MovimientoTipo.COMPRA, desde, hasta);
        BigDecimal ca = movimientoRepository.sumCantidadByItemAndTipoBetween(itemTipo, itemId, MovimientoTipo.COMPRA_ANULACION, desde, hasta);
        return val(c).subtract(val(ca));
    }

    private BigDecimal calcularConsumoIndirectoValor(LocalDateTime desde, LocalDateTime hasta) {
        List<MovimientoInventario> consumos = movimientoRepository
                .findByMovimientoTipoAndFechaMovimientoBetweenOrderByFechaMovimientoAsc(MovimientoTipo.CONSUMO, desde, hasta);
        BigDecimal total = BigDecimal.ZERO;
        for (MovimientoInventario mov : consumos) {
            if (!"INGREDIENTE".equals(mov.getItemTipo())) continue;
            BigDecimal precio = ingredienteRepository.findById(mov.getItemId())
                    .map(i -> i.getPrecioCompra() != null ? i.getPrecioCompra() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);
            total = total.add(val(mov.getCantidad()).multiply(precio));
        }
        return total;
    }

    // ============================================================
    // 5. MÉTODOS PRIVADOS DE UTILIDAD
    // ============================================================

    private BigDecimal val(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal calcularPorcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (parte == null) return BigDecimal.ZERO;
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }
}
