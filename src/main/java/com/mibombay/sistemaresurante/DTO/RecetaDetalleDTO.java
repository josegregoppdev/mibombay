package com.mibombay.sistemaresurante.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaDetalleDTO {

    private Long id;
    private Long recetaId;
    private Long productoId;

    @NotNull(message = "El ingrediente es obligatorio")
    private Long ingredienteId;

    private String nombreIngrediente;
    private String unidad;

    @NotNull(message = "La cantidad es obligatoria")
    private BigDecimal cantidad;

    private BigDecimal costo;
}
