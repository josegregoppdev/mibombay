package com.mibombay.sistemaresurante.DTO;

import com.mibombay.sistemaresurante.models.enums.MetodoPago;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VentaSuspendidaDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;
    private String etiqueta;
    private String itemsJson;
    private Long clienteId;
    private String clienteNombre;
    private MetodoPago metodoPago;
    @Builder.Default
    private BigDecimal recibidoEfectivo = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal recibidoTransferencia = BigDecimal.ZERO;
    @Builder.Default
    private Boolean paraLlevar = false;
    @Builder.Default
    private Integer ordenTab = 1;
}
