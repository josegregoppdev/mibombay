package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @AllArgsConstructor @Builder
public class ConsumoResumenDTO {
    private LocalDate desde;
    private LocalDate hasta;

    @Builder.Default
    private BigDecimal ventasTotales = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal costoIngredientesVendidos = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal mermaValor = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal desperdicioValor = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal diferenciaValor = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal foodCostPorcentaje = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal mermaPorcentaje = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal desperdicioPorcentaje = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal diferenciaPorcentaje = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal costoRealValor = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal costoRealPorcentaje = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal costoAlimentosContable = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal foodCostContablePorcentaje = BigDecimal.ZERO;

    private boolean esPromedio;
    private int diasConDatos;
    private int totalDias;
}
