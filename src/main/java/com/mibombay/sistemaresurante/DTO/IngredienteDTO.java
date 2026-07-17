package com.mibombay.sistemaresurante.DTO;

import com.mibombay.sistemaresurante.models.enums.UnidadMedida;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredienteDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "La unidad de medida es obligatoria")
    private UnidadMedida unidadMedida;

    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal precioCompra;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    @Builder.Default
    private Boolean consumible = false;
}
