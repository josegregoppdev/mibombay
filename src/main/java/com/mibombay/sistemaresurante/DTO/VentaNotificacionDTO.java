package com.mibombay.sistemaresurante.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaNotificacionDTO {
    private Long ventaId;
    private BigDecimal total;
    private String metodoPago;
    private LocalDateTime fechaVenta;
    private String nombreUsuario;
    private String nombreCliente;
    private String tipo;
    private DashboardStatsDTO statsActualizadas;
}
