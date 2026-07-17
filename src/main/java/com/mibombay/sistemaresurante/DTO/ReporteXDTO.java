package com.mibombay.sistemaresurante.DTO;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReporteXDTO {
    private LocalDate fecha;
    private int cantidadVentas;
    private BigDecimal totalVentas;
    private BigDecimal totalEfectivo;
    private BigDecimal totalTransferencia;
    private boolean cierreRealizado;
    private String nombreUsuarioCierre;
    private LocalDateTime fechaCierre;
}
