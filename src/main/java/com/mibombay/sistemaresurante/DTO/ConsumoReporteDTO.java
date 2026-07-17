package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @AllArgsConstructor @Builder
public class ConsumoReporteDTO {
    private String itemTipo;
    private Long itemId;
    private String itemNombre;
    private String unidadMedida;
    private BigDecimal stockDesde;
    private BigDecimal compras;
    private BigDecimal consumo;
    private BigDecimal merma;
    private BigDecimal desperdicio;
    private BigDecimal costoMerma;
    private BigDecimal costoDesperdicio;
    private BigDecimal diferencia;
    private BigDecimal costoDiferencia;
    private BigDecimal stockHasta;
    private BigDecimal stockReal;
}
