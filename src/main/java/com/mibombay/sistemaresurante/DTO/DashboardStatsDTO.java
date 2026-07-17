package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private BigDecimal totalVentasDia;
    private Integer numeroVentasDia;
    private Integer productosStockBajo;
    private Long empresaId;
}
