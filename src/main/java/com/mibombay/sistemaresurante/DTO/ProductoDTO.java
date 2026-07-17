package com.mibombay.sistemaresurante.DTO;

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
public class ProductoDTO {

    private Long id;
    private Long empresaId;
    private Long usuarioId;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio de venta es obligatorio")
    private BigDecimal precioVenta;

    private BigDecimal precioCompra;

    @Builder.Default
    private Boolean tieneReceta = false;

    @Builder.Default
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Builder.Default
    private Boolean activo = true;
    private LocalDateTime fechaCreacion;

    private BigDecimal costoReceta;
    private BigDecimal margenGanancia;
}
