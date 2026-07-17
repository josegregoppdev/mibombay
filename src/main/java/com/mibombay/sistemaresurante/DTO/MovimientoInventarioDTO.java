package com.mibombay.sistemaresurante.DTO;

import com.mibombay.sistemaresurante.models.enums.MovimientoTipo;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoInventarioDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;
    private String nombreUsuario;
    private String itemTipo;
    private Long itemId;
    private String itemNombre;
    private MovimientoTipo movimientoTipo;
    private Long referenciaId;
    private BigDecimal cantidad;
    private String signo;
    private BigDecimal stockAnterior;
    private BigDecimal stockPosterior;
    private LocalDateTime fechaMovimiento;
    private String observacion;
}
