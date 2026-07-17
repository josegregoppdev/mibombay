package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @AllArgsConstructor @Builder
public class FoodCostResumenDTO {
    private LocalDate fecha;
    private BigDecimal ventasTotales;
    private BigDecimal costoIngredientesVendidos;
    private BigDecimal foodCostPorcentaje;
    private BigDecimal inventarioInicialValor;
    private BigDecimal comprasValor;
    private BigDecimal inventarioFinalValor;
    private BigDecimal costoAlimentosContable;
    private BigDecimal foodCostContablePorcentaje;
    private BigDecimal mermaValor;
    private BigDecimal desperdicioValor;
    private BigDecimal diferenciaContable;
    @Builder.Default
    private BigDecimal mermaPorcentaje = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal desperdicioPorcentaje = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal diferenciaInventarioPorcentaje = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal consumoIndirectoValor = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal costoRealValor = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal costoRealPorcentaje = BigDecimal.ZERO;
}
