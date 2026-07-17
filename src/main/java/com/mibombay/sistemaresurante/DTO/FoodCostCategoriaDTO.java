package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor @Builder
public class FoodCostCategoriaDTO {
    private String categoria;
    private BigDecimal ventas;
    private BigDecimal costos;
    private BigDecimal foodCostPorcentaje;
}
