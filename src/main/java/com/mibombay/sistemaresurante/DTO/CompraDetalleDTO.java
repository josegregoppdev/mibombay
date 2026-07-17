package com.mibombay.sistemaresurante.DTO;

import com.mibombay.sistemaresurante.models.enums.TipoItemCompra;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompraDetalleDTO {

    private Long id;
    private Long compraId;

    @NotNull(message = "El tipo de item es obligatorio")
    private TipoItemCompra itemTipo;

    @NotNull(message = "El item es obligatorio")
    private Long itemId;

    private String itemNombre;

    @NotNull(message = "La cantidad es obligatoria")
    private BigDecimal cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    private BigDecimal precioUnitario;

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
}
