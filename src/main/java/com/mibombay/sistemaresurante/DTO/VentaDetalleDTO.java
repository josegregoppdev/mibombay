package com.mibombay.sistemaresurante.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VentaDetalleDTO {

    private Long id;
    private Long ventaId;

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    private String productoNombre;

    @NotNull(message = "La cantidad es obligatoria")
    private BigDecimal cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    private BigDecimal precioUnitario;

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    private String modificaciones;
    private String adicionales;
    private List<Long> ingredientesExcluidosIds;
}
